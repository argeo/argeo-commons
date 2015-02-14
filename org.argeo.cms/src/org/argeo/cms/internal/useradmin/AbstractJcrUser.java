package org.argeo.cms.internal.useradmin;

import java.util.Dictionary;

import org.argeo.cms.CmsException;
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
		throw new CmsException("Not implemented yet");
	}

	@Override
	public boolean hasCredential(String key, Object value) {
		throw new CmsException("Not implemented yet");
	}

}
