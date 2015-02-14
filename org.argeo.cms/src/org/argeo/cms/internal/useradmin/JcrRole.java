package org.argeo.cms.internal.useradmin;

import java.util.Dictionary;

import org.argeo.cms.CmsException;
import org.osgi.service.useradmin.Role;

abstract class JcrRole implements Role {
	private String name;

	public JcrRole(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getType() {
		return Role.ROLE;
	}

	@Override
	public Dictionary<String, Object> getProperties() {
		throw new CmsException("Not implemented yet");
	}

}
