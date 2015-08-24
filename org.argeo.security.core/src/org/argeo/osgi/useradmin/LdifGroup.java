package org.argeo.osgi.useradmin;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;

public class LdifGroup extends LdifUser implements Group {

	public LdifGroup(LdapName dn, Attributes attributes) {
		super(dn, attributes);
	}

	@Override
	public boolean addMember(Role role) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addRequiredMember(Role role) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeMember(Role role) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Role[] getMembers() {
		Attribute memberAttribute = getAttributes().get("member");
		if (memberAttribute == null)
			return new Role[0];
		try {
			List<Role> roles = new ArrayList<Role>();
			NamingEnumeration values = memberAttribute.getAll();
			while (values.hasMore()) {
				LdapName dn = new LdapName(values.next().toString());
				roles.add(new LdifUser(dn, null));
			}
			return roles.toArray(new Role[roles.size()]);
		} catch (Exception e) {
			throw new ArgeoUserAdminException("Cannot get members", e);
		}
	}

	@Override
	public Role[] getRequiredMembers() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getType() {
		return GROUP;
	}

}
