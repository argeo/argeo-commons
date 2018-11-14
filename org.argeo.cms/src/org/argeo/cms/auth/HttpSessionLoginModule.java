package org.argeo.cms.auth;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.cms.internal.kernel.Activator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.useradmin.Authorization;

public class HttpSessionLoginModule implements LoginModule {
	private final static Log log = LogFactory.getLog(HttpSessionLoginModule.class);

	private Subject subject = null;
	private CallbackHandler callbackHandler = null;
	private Map<String, Object> sharedState = null;

	private HttpServletRequest request = null;
	private HttpServletResponse response = null;

	private BundleContext bc;

	private Authorization authorization;
	private Locale locale;

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		bc = FrameworkUtil.getBundle(HttpSessionLoginModule.class).getBundleContext();
		assert bc != null;
		this.subject = subject;
		this.callbackHandler = callbackHandler;
		this.sharedState = (Map<String, Object>) sharedState;
	}

	@Override
	public boolean login() throws LoginException {
		if (callbackHandler == null)
			return false;
		HttpRequestCallback httpCallback = new HttpRequestCallback();
		try {
			callbackHandler.handle(new Callback[] { httpCallback });
		} catch (IOException e) {
			throw new LoginException("Cannot handle http callback: " + e.getMessage());
		} catch (UnsupportedCallbackException e) {
			return false;
		}
		request = httpCallback.getRequest();
		if (request == null)
			return false;
		authorization = (Authorization) request.getAttribute(HttpContext.AUTHORIZATION);
		if (authorization == null) {// search by session ID
			HttpSession httpSession = request.getSession(false);
			if (httpSession == null) {
				// TODO make sure this is always safe
				if (log.isTraceEnabled())
					log.trace("Create http session");
				httpSession = request.getSession(true);
			}
			String httpSessionId = httpSession.getId();
			// authorization = (Authorization)
			// request.getSession().getAttribute(HttpContext.AUTHORIZATION);
			// if (authorization == null) {
			Collection<ServiceReference<CmsSession>> sr;
			try {
				sr = bc.getServiceReferences(CmsSession.class,
						"(" + CmsSession.SESSION_LOCAL_ID + "=" + httpSessionId + ")");
			} catch (InvalidSyntaxException e) {
				throw new CmsException("Cannot get CMS session for id " + httpSessionId, e);
			}
			if (sr.size() == 1) {
				CmsSession cmsSession = bc.getService(sr.iterator().next());
				locale = cmsSession.getLocale();
				authorization = cmsSession.getAuthorization();
				if (authorization.getName() == null)
					authorization = null;// anonymous is not sufficient
				if (log.isTraceEnabled())
					log.trace("Retrieved authorization from " + cmsSession);
			} else if (sr.size() == 0)
				authorization = null;
			else
				throw new CmsException(sr.size() + ">1 web sessions detected for http session " + httpSessionId);

		}
		sharedState.put(CmsAuthUtils.SHARED_STATE_HTTP_REQUEST, request);
		extractHttpAuth(request);
		extractClientCertificate(request);
		if (authorization == null) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean commit() throws LoginException {
		byte[] outToken = (byte[]) sharedState.get(CmsAuthUtils.SHARED_STATE_SPNEGO_OUT_TOKEN);
		if (outToken != null) {
			response.setHeader(CmsAuthUtils.HEADER_WWW_AUTHENTICATE,
					"Negotiate " + java.util.Base64.getEncoder().encodeToString(outToken));
		}

		if (authorization != null) {
			// Locale locale = request.getLocale();
			if (locale == null)
				locale = request.getLocale();
			subject.getPublicCredentials().add(locale);
			CmsAuthUtils.addAuthorization(subject, authorization, locale, request);
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

	private void extractHttpAuth(final HttpServletRequest httpRequest) {
		String authHeader = httpRequest.getHeader(CmsAuthUtils.HEADER_AUTHORIZATION);
		if (authHeader != null) {
			StringTokenizer st = new StringTokenizer(authHeader);
			if (st.hasMoreTokens()) {
				String basic = st.nextToken();
				if (basic.equalsIgnoreCase("Basic")) {
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
							throw new CmsException("Invalid authentication token");
						}
					} catch (Exception e) {
						throw new CmsException("Couldn't retrieve authentication", e);
					}
				} else if (basic.equalsIgnoreCase("Negotiate")) {
					String spnegoToken = st.nextToken();
					Base64.Decoder decoder = Base64.getDecoder();
					byte[] authToken = decoder.decode(spnegoToken);
					sharedState.put(CmsAuthUtils.SHARED_STATE_SPNEGO_TOKEN, authToken);
				}
			}
		}

		// auth token
		// String mail = request.getParameter(LdapAttrs.mail.name());
		// String authPassword = request.getParameter(LdapAttrs.authPassword.name());
		// if (authPassword != null) {
		// sharedState.put(CmsAuthUtils.SHARED_STATE_PWD, authPassword);
		// if (mail != null)
		// sharedState.put(CmsAuthUtils.SHARED_STATE_NAME, mail);
		// }
	}

	private void extractClientCertificate(HttpServletRequest req) {
		X509Certificate[] certs = (X509Certificate[]) req.getAttribute("javax.servlet.request.X509Certificate");
		if (null != certs && certs.length > 0) {// Servlet container verified the client certificate
			String certDn = certs[0].getSubjectX500Principal().getName();
			sharedState.put(CmsAuthUtils.SHARED_STATE_NAME, certDn);
			sharedState.put(CmsAuthUtils.SHARED_STATE_CERTIFICATE_CHAIN, certs);
			if (log.isDebugEnabled())
				log.debug("Client certificate " + certDn + " verified by servlet container");
		} // Reverse proxy verified the client certificate
		String clientDnHttpHeader = Activator.getHttpProxySslHeader();
		if (clientDnHttpHeader != null) {
			String certDn = req.getHeader(clientDnHttpHeader);
			// TODO retrieve more cf. https://httpd.apache.org/docs/current/mod/mod_ssl.html
			// String issuerDn = req.getHeader("SSL_CLIENT_I_DN");
			if (certDn != null && !certDn.trim().equals("")) {
				sharedState.put(CmsAuthUtils.SHARED_STATE_NAME, certDn);
				sharedState.put(CmsAuthUtils.SHARED_STATE_CERTIFICATE_CHAIN, "");
				if (log.isDebugEnabled())
					log.debug("Client certificate " + certDn + " verified by reverse proxy");
			}
		}
	}

}
