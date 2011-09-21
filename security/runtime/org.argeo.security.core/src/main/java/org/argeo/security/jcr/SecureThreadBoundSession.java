package org.argeo.security.jcr;

import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.jcr.spring.ThreadBoundSession;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;

/**
 * Thread bounded JCR session factory which checks authentication and is
 * autoconfigured in Spring.
 */
public class SecureThreadBoundSession extends ThreadBoundSession {
	private final static Log log = LogFactory
			.getLog(SecureThreadBoundSession.class);

	@Override
	protected Session preCall(Session session) {
		Authentication authentication = SecurityContextHolder.getContext()
				.getAuthentication();
		if (authentication != null) {
			String userID = session.getUserID();
			String currentUserName = authentication.getName();
			if (currentUserName != null) {
				if (!userID.equals(currentUserName)) {
					log.warn("Current session has user ID " + userID
							+ " while logged is user is " + currentUserName
							+ "(authentication=" + authentication + ")"
							+ ". Re-login.");
					// TODO throw an exception
					return login();
				}
			}
			// UserDetails userDetails = (UserDetails)
			// authentication.getDetails();
			// if (userDetails != null) {
			// String currentUserName = userDetails.getUsername();
			// if (!userID.equals(currentUserName)) {
			// log.warn("Current session has user ID " + userID
			// + " while logged is user is " + currentUserName
			// + "(authentication=" + authentication + ")"
			// + ". Re-login.");
			// return login();
			// }
			// }
		}
		return super.preCall(session);
	}

}
