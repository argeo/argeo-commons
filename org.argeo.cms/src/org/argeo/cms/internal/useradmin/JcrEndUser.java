package org.argeo.cms.internal.useradmin;

import org.argeo.security.jcr.JcrUserDetails;
import org.springframework.security.core.userdetails.UserDetails;

class JcrEndUser extends AbstractJcrUser {
	private final JcrUserDetails userDetails;

	public JcrEndUser(JcrUserDetails userDetails) {
		super(userDetails.getUsername());
		this.userDetails = userDetails;
	}

	public UserDetails getUserDetails() {
		return userDetails;
	}

	public String toString() {
		return "ArgeoUser: " + getName();
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof JcrEndUser))
			return false;
		else
			return ((JcrEndUser) obj).getName().equals(getName());
	}

	public int hashCode() {
		return getName().hashCode();
	}
}
