package org.argeo.cms.auth;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.cms.internal.kernel.WebCmsSessionImpl;
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
			Collection<ServiceReference<WebCmsSession>> sr;
			try {
				sr = bc.getServiceReferences(WebCmsSession.class,
						"(" + WebCmsSession.CMS_SESSION_ID + "=" + httpSessionId + ")");
			} catch (InvalidSyntaxException e) {
				throw new CmsException("Cannot get CMS session for id " + httpSessionId, e);
			}
			if (sr.size() == 1) {
				WebCmsSession cmsSession = bc.getService(sr.iterator().next());
				authorization = cmsSession.getAuthorization();
				if (log.isTraceEnabled())
					log.trace("Retrieved authorization from " + cmsSession);
			} else if (sr.size() == 0)
				authorization = null;
			else
				throw new CmsException(sr.size() + ">1 web sessions detected for http session " + httpSessionId);

		}
		if (authorization == null)
			return false;
		sharedState.put(CmsAuthUtils.SHARED_STATE_AUTHORIZATION, authorization);
		return true;
	}

	// private Authorization checkHttp() {
	// Authorization authorization = null;
	// if (request != null) {
	// authorization = (Authorization)
	// request.getAttribute(HttpContext.AUTHORIZATION);
	// if (authorization == null) {
	// String httpSessionId = request.getSession().getId();
	// authorization = (Authorization)
	// request.getSession().getAttribute(HttpContext.AUTHORIZATION);
	// if (authorization == null) {
	// Collection<ServiceReference<WebCmsSession>> sr;
	// try {
	// sr = bc.getServiceReferences(WebCmsSession.class,
	// "(" + WebCmsSession.CMS_SESSION_ID + "=" + httpSessionId + ")");
	// } catch (InvalidSyntaxException e) {
	// throw new CmsException("Cannot get CMS session for id " + httpSessionId,
	// e);
	// }
	// if (sr.size() == 1) {
	// WebCmsSession cmsSession = bc.getService(sr.iterator().next());
	// authorization = cmsSession.getAuthorization();
	// if (log.isTraceEnabled())
	// log.trace("Retrieved authorization from " + cmsSession);
	// } else if (sr.size() == 0)
	// return null;
	// else
	// throw new CmsException(
	// sr.size() + ">1 web sessions detected for http session " +
	// httpSessionId);
	// }
	// }
	// }
	// return authorization;
	// }

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
		HttpSessionId httpSessionId = registerAuthorization(request, authorizationToRegister);
		if (subject.getPrivateCredentials(HttpSessionId.class).size() == 0)
			subject.getPrivateCredentials().add(httpSessionId);
		else {
			String storedSessionId = subject.getPrivateCredentials(HttpSessionId.class).iterator().next().getValue();
			// if (storedSessionId.equals(httpSessionId.getValue()))
			throw new LoginException(
					"Subject already logged with session " + storedSessionId + " (not " + httpSessionId + ")");
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

	private HttpSessionId registerAuthorization(HttpServletRequest request, Authorization authorization) {
		String httpSessionId = request.getSession().getId();
		if (authorization.getName() != null) {
			request.setAttribute(HttpContext.REMOTE_USER, authorization.getName());
			request.setAttribute(HttpContext.AUTHORIZATION, authorization);

			HttpSession httpSession = request.getSession();
			if (httpSession.getAttribute(HttpContext.AUTHORIZATION) == null) {

				Collection<ServiceReference<WebCmsSession>> sr;
				try {
					sr = bc.getServiceReferences(WebCmsSession.class,
							"(" + WebCmsSession.CMS_SESSION_ID + "=" + httpSessionId + ")");
				} catch (InvalidSyntaxException e) {
					throw new CmsException("Cannot get CMS session for id " + httpSessionId, e);
				}
				ServiceReference<WebCmsSession> cmsSessionRef;
				if (sr.size() == 1) {
					cmsSessionRef = sr.iterator().next();
				} else if (sr.size() == 0) {
					WebCmsSessionImpl cmsSessionImpl = new WebCmsSessionImpl(httpSessionId, authorization);
					cmsSessionRef = cmsSessionImpl.getServiceRegistration().getReference();
					if (log.isDebugEnabled())
						log.debug("Initialized " + cmsSessionImpl + " for " + authorization.getName());
				} else
					throw new CmsException(sr.size() + " CMS sessions registered for " + httpSessionId);

				WebCmsSessionImpl cmsSession = (WebCmsSessionImpl) bc.getService(cmsSessionRef);
				cmsSession.addHttpSession(request);
				if (log.isTraceEnabled())
					log.trace("Added " + request.getServletPath() + " to " + cmsSession + " (" + request.getRequestURI()
							+ ")");
				// httpSession.setAttribute(HttpContext.REMOTE_USER,
				// authorization.getName());
				// httpSession.setAttribute(HttpContext.AUTHORIZATION,
				// authorization);
			}
		}
		return new HttpSessionId(httpSessionId);
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
		String httpSessionId;
		if (subject.getPrivateCredentials(HttpSessionId.class).size() == 1)
			httpSessionId = subject.getPrivateCredentials(HttpSessionId.class).iterator().next().getValue();
		else
			return false;
		Collection<ServiceReference<WebCmsSession>> srs;
		try {
			srs = bc.getServiceReferences(WebCmsSession.class,
					"(" + WebCmsSession.CMS_SESSION_ID + "=" + httpSessionId + ")");
		} catch (InvalidSyntaxException e) {
			throw new CmsException("Cannot retrieve CMS session #" + httpSessionId, e);
		}

		if (srs.size() == 0)
			throw new CmsException("No CMS web session found for http session " + httpSessionId);
		else if (srs.size() > 1)
			throw new CmsException(srs.size() + " CMS web sessions found for http session " + httpSessionId);

		WebCmsSessionImpl cmsSession = (WebCmsSessionImpl) bc.getService(srs.iterator().next());
		cmsSession.cleanUp();
		subject.getPrivateCredentials().removeAll(subject.getPrivateCredentials(HttpSessionId.class));
		if (log.isDebugEnabled())
			log.debug("Cleaned up " + cmsSession);
		return true;
	}

}
