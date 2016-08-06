package org.argeo.cms.auth;

import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
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
import org.osgi.framework.ServiceRegistration;
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
				String sessionId = request.getSession().getId();
				authorization = (Authorization) request.getSession().getAttribute(HttpContext.AUTHORIZATION);
				if (authorization == null) {
					Collection<ServiceReference<WebCmsSession>> sr;
					try {
						sr = bc.getServiceReferences(WebCmsSession.class,
								"(" + WebCmsSession.CMS_SESSION_ID + "=" + sessionId + ")");
					} catch (InvalidSyntaxException e) {
						throw new CmsException("Cannot get CMS session for id " + sessionId, e);
					}
					if (sr.size() == 1) {
						WebCmsSession cmsSession = bc.getService(sr.iterator().next());
						authorization = cmsSession.getAuthorization();
						if (log.isTraceEnabled())
							log.trace("Retrieved authorization from " + cmsSession);
					}
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
		String sessionId = request.getSession().getId();
		if (authorization.getName() != null) {
			request.setAttribute(HttpContext.REMOTE_USER, authorization.getName());
			request.setAttribute(HttpContext.AUTHORIZATION, authorization);

			HttpSession httpSession = request.getSession();
			if (httpSession.getAttribute(HttpContext.AUTHORIZATION) == null) {

				Collection<ServiceReference<WebCmsSession>> sr;
				try {
					sr = bc.getServiceReferences(WebCmsSession.class,
							"(" + WebCmsSession.CMS_SESSION_ID + "=" + sessionId + ")");
				} catch (InvalidSyntaxException e) {
					throw new CmsException("Cannot get CMS session for id " + sessionId, e);
				}
				ServiceReference<WebCmsSession> cmsSessionRef;
				if (sr.size() == 1) {
					cmsSessionRef = sr.iterator().next();
				} else if (sr.size() == 0) {
					Hashtable<String, String> props = new Hashtable<>();
					props.put(WebCmsSession.CMS_DN, authorization.getName());
					props.put(WebCmsSession.CMS_SESSION_ID, sessionId);
					WebCmsSessionImpl cmsSessionImpl = new WebCmsSessionImpl(sessionId, authorization);
					ServiceRegistration<WebCmsSession> cmSessionReg = bc.registerService(WebCmsSession.class,
							cmsSessionImpl, props);
					cmsSessionImpl.setServiceRegistration(cmSessionReg);
					cmsSessionRef = cmSessionReg.getReference();
					if (log.isDebugEnabled())
						log.debug("Initialized " + cmsSessionImpl + " for " + authorization.getName());
				} else
					throw new CmsException(sr.size() + " CMS sessions registered for " + sessionId);

				WebCmsSession cmsSession = bc.getService(cmsSessionRef);
				cmsSession.addHttpSession(request);
				if (log.isTraceEnabled())
					log.trace("Added " + request.getServletPath() + " to " + cmsSession + " (" + request.getRequestURI()
							+ ")");
				httpSession.setAttribute(HttpContext.AUTHORIZATION, authorization);
			}
		}
		if (subject.getPrivateCredentials(HttpSessionId.class).size() == 0)
			subject.getPrivateCredentials().add(new HttpSessionId(sessionId));
		else {
			String storedSessionId = subject.getPrivateCredentials(HttpSessionId.class).iterator().next().getValue();
			if (storedSessionId.equals(sessionId))
				throw new LoginException(
						"Subject already logged with session " + storedSessionId + " (not " + sessionId + ")");
		}
		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		return false;
	}

	@Override
	public boolean logout() throws LoginException {
		String sessionId;
		if (subject.getPrivateCredentials(HttpSessionId.class).size() == 1)
			sessionId = subject.getPrivateCredentials(HttpSessionId.class).iterator().next().getValue();
		else
			return false;
		Collection<ServiceReference<WebCmsSession>> srs;
		try {
			srs = bc.getServiceReferences(WebCmsSession.class,
					"(" + WebCmsSession.CMS_SESSION_ID + "=" + sessionId + ")");
		} catch (InvalidSyntaxException e) {
			throw new CmsException("Cannot retrieve CMS session #" + sessionId, e);
		}

		for (Iterator<ServiceReference<WebCmsSession>> it = srs.iterator(); it.hasNext();) {
			ServiceReference<WebCmsSession> sr = it.next();
			WebCmsSession cmsSession = bc.getService(sr);
			cmsSession.cleanUp();
			if (log.isDebugEnabled())
				log.debug("Cleaned up " + cmsSession);
		}
		return true;
	}

}
