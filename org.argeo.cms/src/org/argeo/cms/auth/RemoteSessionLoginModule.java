package org.argeo.cms.auth;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.argeo.api.cms.CmsLog;
import org.argeo.cms.CmsDeployProperty;
import org.argeo.cms.http.HttpHeader;
import org.argeo.cms.internal.auth.CmsSessionImpl;
import org.argeo.cms.internal.runtime.CmsContextImpl;
import org.argeo.cms.internal.runtime.CmsStateImpl;
import org.osgi.service.useradmin.Authorization;

/** Use a remote session as the basis for authentication. */
public class RemoteSessionLoginModule implements LoginModule {
	private final static CmsLog log = CmsLog.getLog(RemoteSessionLoginModule.class);

	private Subject subject = null;
	private CallbackHandler callbackHandler = null;
	private Map<String, Object> sharedState = null;

	private RemoteAuthRequest request = null;
	private RemoteAuthResponse response = null;

	private Authorization authorization;
	private Locale locale;

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		this.subject = subject;
		this.callbackHandler = callbackHandler;
		this.sharedState = (Map<String, Object>) sharedState;
	}

	@Override
	public boolean login() throws LoginException {
		if (callbackHandler == null)
			return false;
		RemoteAuthCallback remoteAuthCallback = new RemoteAuthCallback();
		try {
			callbackHandler.handle(new Callback[] { remoteAuthCallback });
		} catch (IOException e) {
			throw new LoginException("Cannot handle http callback: " + e.getMessage());
		} catch (UnsupportedCallbackException e) {
			return false;
		}
		request = remoteAuthCallback.getRequest();
		if (request == null) {
			RemoteAuthSession httpSession = remoteAuthCallback.getHttpSession();
			if (httpSession == null)
				return false;
			// TODO factorize with below
			String httpSessionId = httpSession.getId();
			CmsSessionImpl cmsSession = CmsContextImpl.getCmsContext().getCmsSessionByLocalId(httpSessionId);
			if (cmsSession != null && !cmsSession.isAnonymous()) {
				authorization = cmsSession.getAuthorization();
				locale = cmsSession.getLocale();
				if (log.isTraceEnabled())
					log.trace("Retrieved authorization from " + cmsSession);
			}
		} else {
			authorization = (Authorization) request.getAttribute(RemoteAuthRequest.AUTHORIZATION);
			if (authorization == null) {// search by session ID
				RemoteAuthSession httpSession = request.getSession();
				if (httpSession != null) {
					String httpSessionId = httpSession.getId();
					CmsSessionImpl cmsSession = CmsContextImpl.getCmsContext().getCmsSessionByLocalId(httpSessionId);
					if (cmsSession != null && !cmsSession.isAnonymous()) {
						authorization = cmsSession.getAuthorization();
						locale = cmsSession.getLocale();
						if (log.isTraceEnabled())
							log.trace("Retrieved authorization from " + cmsSession);
					}
				}else {
					request.createSession();
				}
			}
			sharedState.put(CmsAuthUtils.SHARED_STATE_HTTP_REQUEST, request);
			extractHttpAuth(request);
			extractClientCertificate(request);
		}
		if (authorization == null) {
			if (log.isTraceEnabled())
				log.trace("HTTP login: " + false);
			return false;
		} else {
			if (log.isTraceEnabled())
				log.trace("HTTP login: " + true);
			request.setAttribute(RemoteAuthRequest.AUTHORIZATION, authorization);
			return true;
		}
	}

	@Override
	public boolean commit() throws LoginException {
		byte[] outToken = (byte[]) sharedState.get(CmsAuthUtils.SHARED_STATE_SPNEGO_OUT_TOKEN);
		if (outToken != null) {
			response.setHeader(HttpHeader.WWW_AUTHENTICATE.getHeaderName(),
					"Negotiate " + java.util.Base64.getEncoder().encodeToString(outToken));
		}

		if (authorization != null) {
			// Locale locale = request.getLocale();
			if (locale == null && request != null)
				locale = request.getLocale();
			if (locale != null)
				subject.getPublicCredentials().add(locale);
			CmsAuthUtils.addAuthorization(subject, authorization);
			CmsAuthUtils.registerSessionAuthorization(request, subject, authorization, locale);
			cleanUp();
			return true;
		} else {
			cleanUp();
			return false;
		}
	}

	@Override
	public boolean abort() throws LoginException {
		cleanUp();
		return false;
	}

	private void cleanUp() {
		authorization = null;
		request = null;
	}

	@Override
	public boolean logout() throws LoginException {
		cleanUp();
		return true;
	}

	private void extractHttpAuth(final RemoteAuthRequest httpRequest) {
		String authHeader = httpRequest.getHeader(HttpHeader.AUTHORIZATION.getHeaderName());
		extractHttpAuth(authHeader);
	}

	private void extractHttpAuth(String authHeader) {
		if (authHeader != null) {
			StringTokenizer st = new StringTokenizer(authHeader);
			if (st.hasMoreTokens()) {
				String basic = st.nextToken();
				if (basic.equalsIgnoreCase(HttpHeader.BASIC)) {
					try {
						// TODO manipulate char[]
						Base64.Decoder decoder = Base64.getDecoder();
						String credentials = new String(decoder.decode(st.nextToken()), "UTF-8");
						// log.debug("Credentials: " + credentials);
						int p = credentials.indexOf(":");
						if (p != -1) {
							final String login = credentials.substring(0, p).trim();
							final char[] password = credentials.substring(p + 1).trim().toCharArray();
							sharedState.put(CmsAuthUtils.SHARED_STATE_NAME, login);
							sharedState.put(CmsAuthUtils.SHARED_STATE_PWD, password);
						} else {
							throw new IllegalStateException("Invalid authentication token");
						}
					} catch (Exception e) {
						throw new IllegalStateException("Couldn't retrieve authentication", e);
					}
				} else if (basic.equalsIgnoreCase(HttpHeader.NEGOTIATE)) {
					String spnegoToken = st.nextToken();
					Base64.Decoder decoder = Base64.getDecoder();
					byte[] authToken = decoder.decode(spnegoToken);
					sharedState.put(CmsAuthUtils.SHARED_STATE_SPNEGO_TOKEN, authToken);
				}
			}
		}
	}

	private void extractClientCertificate(RemoteAuthRequest req) {
		X509Certificate[] certs = (X509Certificate[]) req.getAttribute("javax.servlet.request.X509Certificate");
		if (null != certs && certs.length > 0) {// Servlet container verified the client certificate
			String certDn = certs[0].getSubjectX500Principal().getName();
			sharedState.put(CmsAuthUtils.SHARED_STATE_NAME, certDn);
			sharedState.put(CmsAuthUtils.SHARED_STATE_CERTIFICATE_CHAIN, certs);
			if (log.isDebugEnabled())
				log.debug("Client certificate " + certDn + " verified by servlet container");
		} // Reverse proxy verified the client certificate
		String clientDnHttpHeader = CmsStateImpl.getDeployProperty(CmsContextImpl.getCmsContext().getCmsState(),
				CmsDeployProperty.HTTP_PROXY_SSL_HEADER_DN);
		if (clientDnHttpHeader != null) {
			String certDn = req.getHeader(clientDnHttpHeader);
			// TODO retrieve more cf. https://httpd.apache.org/docs/current/mod/mod_ssl.html
			// String issuerDn = req.getHeader("SSL_CLIENT_I_DN");
			if (certDn != null && !certDn.trim().equals("(null)")) {
				sharedState.put(CmsAuthUtils.SHARED_STATE_NAME, certDn);
				sharedState.put(CmsAuthUtils.SHARED_STATE_CERTIFICATE_CHAIN, "");
				if (log.isDebugEnabled())
					log.debug("Client certificate " + certDn + " verified by reverse proxy");
			}
		}
	}

}
