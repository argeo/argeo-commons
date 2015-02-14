package org.argeo.cms.internal.useradmin;

import org.argeo.cms.CmsException;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;

class JcrGroup extends AbstractJcrUser implements Group {
	public JcrGroup(String name) {
		super(name);
	}

	//
	// OSGi MODEL
	//
	@Override
	public int getType() {
		return Role.GROUP;
	}

	@Override
	public boolean addMember(Role role) {
		throw new CmsException("Not implemented yet");
	}

	@Override
	public boolean addRequiredMember(Role role) {
		throw new CmsException("Not implemented yet");
	}

	@Override
	public boolean removeMember(Role role) {
		throw new CmsException("Not implemented yet");
	}

	@Override
	public Role[] getMembers() {
		throw new CmsException("Not implemented yet");
	}

	@Override
	public Role[] getRequiredMembers() {
		throw new CmsException("Not implemented yet");
	}

	public String toString() {
		return "ArgeoGroup: " + getName();
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof JcrGroup))
			return false;
		else
			return ((JcrGroup) obj).getName().equals(getName());
	}

	public int hashCode() {
		return getName().hashCode();
	}

}
