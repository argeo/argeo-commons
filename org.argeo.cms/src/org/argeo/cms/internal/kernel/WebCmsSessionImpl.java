package org.argeo.cms.internal.kernel;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.auth.WebCmsSession;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;
import org.osgi.service.useradmin.Authorization;

public class WebCmsSessionImpl implements WebCmsSession {
	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();
	private final static Log log = LogFactory.getLog(WebCmsSessionImpl.class);

	private final String id;
	private final Authorization authorization;

	private List<SubHttpSession> subHttpSessions = new ArrayList<>();

	private ServiceRegistration<WebCmsSession> serviceRegistration;

	public WebCmsSessionImpl(String sessionId, Authorization authorization) {
		this.id = sessionId;
		this.authorization = authorization;
		// register as service
		Hashtable<String, String> props = new Hashtable<>();
		props.put(WebCmsSession.CMS_DN, authorization.getName());
		props.put(WebCmsSession.CMS_SESSION_ID, sessionId);
		serviceRegistration = bc.registerService(WebCmsSession.class, this, props);
	}

	public void cleanUp() {
		for (SubHttpSession subSession : subHttpSessions)
			subSession.cleanUp();
		serviceRegistration.unregister();
	}

	@Override
	public Authorization getAuthorization() {
		return authorization;
	}

	public ServiceRegistration<WebCmsSession> getServiceRegistration() {
		return serviceRegistration;
	}

	public void addHttpSession(HttpServletRequest request) {
		subHttpSessions.add(new SubHttpSession(request));
	}

	public String getId() {
		return id;
	}

	public String toString() {
		return "CMS Session #" + id;
	}

	static class SubHttpSession {
		private final HttpSession httpSession;
		private final String sessionId;
		// private final String originalURI;
		// private final String servletPath;

		private final Date start = new Date();

		public SubHttpSession(HttpServletRequest request) {
			this.httpSession = request.getSession();
			this.sessionId = httpSession.getId();
			// this.originalURI = request.getRequestURI();
			// this.servletPath = request.getServletPath();
		}

		public Date getStart() {
			return start;
		}

		public void cleanUp() {
			try {
				httpSession.setAttribute(HttpContext.REMOTE_USER, null);
				httpSession.setAttribute(HttpContext.AUTHORIZATION, null);
				// httpSession.setMaxInactiveInterval(1);
			} catch (Exception e) {
				log.warn("Could not clean up " + sessionId, e);
			}
		}

	}
}
