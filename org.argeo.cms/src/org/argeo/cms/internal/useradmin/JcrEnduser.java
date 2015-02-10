package org.argeo.cms.internal.useradmin;

import org.springframework.security.core.userdetails.UserDetails;

class JcrEnduser extends AbstractJcrUser {
	private final UserDetails userDetails;

	public JcrEnduser(UserDetails userDetails) {
		this.userDetails = userDetails;
	}

	UserDetails getUserDetails() {
		return userDetails;
	}

}
