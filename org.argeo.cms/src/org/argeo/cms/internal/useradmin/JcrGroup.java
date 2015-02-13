package org.argeo.cms.internal.useradmin;

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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addRequiredMember(Role role) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeMember(Role role) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Role[] getMembers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Role[] getRequiredMembers() {
		// TODO Auto-generated method stub
		return null;
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
