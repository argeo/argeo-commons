package org.argeo.cms.internal.useradmin;

import org.springframework.security.core.userdetails.UserDetails;

class JcrEndUser extends AbstractJcrUser  {
	private final UserDetails userDetails;

	public JcrEndUser(UserDetails userDetails) {
		this.userDetails = userDetails;
	}

	UserDetails getUserDetails() {
		return userDetails;
	}

}
