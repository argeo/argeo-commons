package org.argeo.cms.internal.useradmin;

import java.util.Dictionary;

import org.osgi.service.useradmin.User;

abstract class AbstractJcrUser extends JcrRole implements User {

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
