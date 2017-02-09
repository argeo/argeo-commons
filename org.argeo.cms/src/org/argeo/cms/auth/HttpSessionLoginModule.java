package org.argeo.cms.auth;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Collection;
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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
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
			String httpSessionId = request.getSession().getId();
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
				authorization = cmsSession.getAuthorization();
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
		if (authorization == null)
			return false;
		sharedState.put(CmsAuthUtils.SHARED_STATE_AUTHORIZATION, authorization);
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		// TODO create CmsSession in another module
		Authorization authorizationToRegister;
		if (authorization == null) {
			authorizationToRegister = (Authorization) sharedState.get(CmsAuthUtils.SHARED_STATE_AUTHORIZATION);
		} else { // this login module did the authorization
			CmsAuthUtils.addAuthentication(subject, authorization);
			authorizationToRegister = authorization;
		}
		if (authorizationToRegister == null) {
			return false;
		}
		if (request == null)
			return false;
		CmsAuthUtils.registerSessionAuthorization(bc, request, subject, authorizationToRegister);

		byte[] outToken = (byte[]) sharedState.get(CmsAuthUtils.SHARED_STATE_SPNEGO_OUT_TOKEN);
		if (outToken != null) {
			response.setHeader(CmsAuthUtils.HEADER_WWW_AUTHENTICATE,
					"Negotiate " + java.util.Base64.getEncoder().encodeToString(outToken));
		}

		if (authorization != null) {
			// CmsAuthUtils.addAuthentication(subject, authorization);
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
		return CmsAuthUtils.logoutSession(bc, subject);
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
						String credentials = new String(Base64.decodeBase64(st.nextToken()), "UTF-8");
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
					byte[] authToken = Base64.decodeBase64(spnegoToken);
					sharedState.put(CmsAuthUtils.SHARED_STATE_SPNEGO_TOKEN, authToken);
				}
			}
		}
	}

	private X509Certificate[] extractClientCertificate(HttpServletRequest req) {
		X509Certificate[] certs = (X509Certificate[]) req.getAttribute("javax.servlet.request.X509Certificate");
		if (null != certs && certs.length > 0) {
			return certs;
		}
		return null;
	}

}
