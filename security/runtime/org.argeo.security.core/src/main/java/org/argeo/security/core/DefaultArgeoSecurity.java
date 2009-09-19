package org.argeo.security.core;

import org.argeo.security.ArgeoUser;
import org.argeo.security.ArgeoSecurity;
import org.argeo.security.nature.SimpleUserNature;

public class DefaultArgeoSecurity implements ArgeoSecurity {
	private String superUsername = "root";

	public void beforeCreate(ArgeoUser user) {
		SimpleUserNature simpleUserNature = new SimpleUserNature();
		simpleUserNature.setLastName("");// to prevent issue with sn in LDAP
		user.getUserNatures().add(simpleUserNature);
	}

	public String getSuperUsername() {
		return superUsername;
	}

	public void setSuperUsername(String superUsername) {
		this.superUsername = superUsername;
	}

}
