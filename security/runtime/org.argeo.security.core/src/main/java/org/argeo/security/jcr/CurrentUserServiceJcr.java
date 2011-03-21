package org.argeo.security.jcr;

import java.util.Map;

import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.security.ArgeoUser;
import org.argeo.security.CurrentUserDao;
import org.argeo.security.CurrentUserService;
import org.argeo.security.UserNature;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;

public class CurrentUserServiceJcr implements CurrentUserService {
	private Session session;
	private CurrentUserDao currentUserDao;

	public ArgeoUser getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext()
				.getAuthentication();

		Session userSession;
		if (authentication instanceof JcrAuthenticationToken) {
			userSession = ((JcrAuthenticationToken) authentication)
					.getSession();
		} else {
			if (session == null)
				throw new ArgeoException("No user JCR session available");
			userSession = session;
		}

		JcrUserDetails jcrUserDetails = (JcrUserDetails) authentication
				.getDetails();
		return JcrUserDetails.jcrUserDetailsToArgeoUser(userSession,
				jcrUserDetails);
	}

	public void updateCurrentUserPassword(String oldPassword, String newPassword) {
		currentUserDao.updateCurrentUserPassword(oldPassword, newPassword);

	}

	public void updateCurrentUserNatures(Map<String, UserNature> userNatures) {
		// TODO Auto-generated method stub

	}

	public void setSession(Session session) {
		this.session = session;
	}

	public void setCurrentUserDao(CurrentUserDao currentUserDao) {
		this.currentUserDao = currentUserDao;
	}

}
