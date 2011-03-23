package org.argeo.security.jcr;

import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.jcr.ThreadBoundJcrSessionFactory;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.userdetails.UserDetails;

public class SecureThreadBoundSession extends ThreadBoundJcrSessionFactory {
	private final static Log log = LogFactory
			.getLog(SecureThreadBoundSession.class);

	@Override
	protected Session preCall(Session session) {
		Authentication authentication = SecurityContextHolder.getContext()
				.getAuthentication();
		if (authentication != null) {
			String userID = session.getUserID();
			UserDetails userDetails = (UserDetails) authentication.getDetails();
			if (userDetails != null) {
				String currentUserName = userDetails.getUsername();
				if (!userID.equals(currentUserName)) {
					log.warn("Current session has user ID " + userID
							+ " while logged is user is " + currentUserName
							+ "(authentication=" + authentication + ")"
							+ ". Re-login.");
					return login();
				}
			}
		}
		return super.preCall(session);
	}

}
