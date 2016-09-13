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

public class HttpLoginModule implements LoginModule, AuthConstants {
	private final static Log log = LogFactory.getLog(HttpLoginModule.class);

	private Subject subject = null;
	private CallbackHandler callbackHandler = null;
	private Map<String, Object> sharedState = null;

	private HttpServletRequest request = null;

	private BundleContext bc;

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		bc = FrameworkUtil.getBundle(HttpLoginModule.class).getBundleContext();
		assert bc != null;
		this.subject = subject;
		this.callbackHandler = callbackHandler;
		this.sharedState = (Map<String, Object>) sharedState;
	}

	@Override
	public boolean login() throws LoginException {
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
		Authorization authorization = checkHttp();
		if (authorization == null)
			return false;
		sharedState.put(SHARED_STATE_AUTHORIZATION, authorization);
		return true;
	}

	private Authorization checkHttp() {
		Authorization authorization = null;
		if (request != null) {
			authorization = (Authorization) request.getAttribute(HttpContext.AUTHORIZATION);
			if (authorization == null) {
				String httpSessionId = request.getSession().getId();
				authorization = (Authorization) request.getSession().getAttribute(HttpContext.AUTHORIZATION);
				if (authorization == null) {
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
						return null;
					else
						throw new CmsException(
								sr.size() + ">1 web sessions detected for http session " + httpSessionId);
				}
			}
		}
		return authorization;
	}

	@Override
	public boolean commit() throws LoginException {
		Authorization authorization = (Authorization) sharedState.get(SHARED_STATE_AUTHORIZATION);
		if (authorization == null)
			return false;
		if (request == null)
			return false;
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
				httpSession.setAttribute(HttpContext.AUTHORIZATION, authorization);
			}
		}
		if (subject.getPrivateCredentials(HttpSessionId.class).size() == 0)
			subject.getPrivateCredentials().add(new HttpSessionId(httpSessionId));
		else {
			String storedSessionId = subject.getPrivateCredentials(HttpSessionId.class).iterator().next().getValue();
			if (storedSessionId.equals(httpSessionId))
				throw new LoginException(
						"Subject already logged with session " + storedSessionId + " (not " + httpSessionId + ")");
		}
		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		return false;
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
			throw new CmsException("No CMS web sesison found for http session " + httpSessionId);
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
