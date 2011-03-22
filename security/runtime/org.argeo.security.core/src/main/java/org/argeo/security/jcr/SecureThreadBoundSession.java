package org.argeo.security.jcr;

import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.jcr.ThreadBoundJcrSessionFactory;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;

public class SecureThreadBoundSession extends ThreadBoundJcrSessionFactory {
	private final static Log log = LogFactory
			.getLog(SecureThreadBoundSession.class);

	@Override
	protected Session preCall(Session session) {
		Authentication authentication = SecurityContextHolder.getContext()
				.getAuthentication();
		if (authentication != null) {
			if (!session.getUserID().equals(
					authentication.getPrincipal().toString())) {
				log.warn("Current session has user ID " + session.getUserID()
						+ " while authentication is " + authentication
						+ ". Re-login.");
				return login();
			}
		}
		return super.preCall(session);
	}

}
