package org.argeo.server.jcr.mvc;

import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

public class OpenSessionInViewJcrInterceptor implements WebRequestInterceptor {
	private final static Log log = LogFactory
			.getLog(OpenSessionInViewJcrInterceptor.class);

	private Session session;

	public void preHandle(WebRequest request) throws Exception {
		if (log.isTraceEnabled())
			log.trace("preHandle: " + request);
		// Authentication auth = SecurityContextHolder.getContext()
		// .getAuthentication();
		// if (auth != null)
		// log.debug("auth=" + auth + ", authenticated="
		// + auth.isAuthenticated() + ", name=" + auth.getName());
		// else
		// log.debug("No auth");

		// FIXME: find a safer way to initialize
		// FIXME: not really needed to initialize here
		//session.getRepository();
	}

	public void postHandle(WebRequest request, ModelMap model) throws Exception {
		// if (log.isDebugEnabled())
		// log.debug("postHandle: " + request);
	}

	public void afterCompletion(WebRequest request, Exception ex)
			throws Exception {
		if (log.isTraceEnabled())
			log.trace("afterCompletion: " + request);
		// FIXME: only close session that were open
		session.logout();
	}

	public void setSession(Session session) {
		this.session = session;
	}

}
