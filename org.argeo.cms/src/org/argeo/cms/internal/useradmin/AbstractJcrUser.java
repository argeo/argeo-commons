package org.argeo.cms.internal.useradmin;

import java.util.Dictionary;

import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

abstract class AbstractJcrUser extends JcrRole implements User {
 	public AbstractJcrUser(String name) {
		super(name);
	}

	@Override
	public int getType() {
		return Role.USER;
	}

	@Override
	public Dictionary<String, Object> getCredentials() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasCredential(String key, Object value) {
		// TODO Auto-generated method stub
		return false;
	}

}
