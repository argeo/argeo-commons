package org.argeo.osgi.useradmin;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.osgi.service.useradmin.Role;

class LdifGroup extends LdifUser implements DirectoryGroup {
	private final String memberAttributeId;

	public LdifGroup(AbstractUserDirectory userAdmin, LdapName dn,
			Attributes attributes) {
		super(userAdmin, dn, attributes);
		memberAttributeId = userAdmin.getMemberAttributeId();
	}

	@Override
	public boolean addMember(Role role) {
		getUserAdmin().checkEdit();
		if (!isEditing())
			startEditing();

		Attribute member = getAttributes().get(memberAttributeId);
		if (member != null) {
			if (member.contains(role.getName()))
				return false;
			else
				member.add(role.getName());
		} else
			getAttributes().put(memberAttributeId, role.getName());
		return true;
	}

	@Override
	public boolean addRequiredMember(Role role) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeMember(Role role) {
		getUserAdmin().checkEdit();
		if (!isEditing())
			startEditing();

		Attribute member = getAttributes().get(memberAttributeId);
		if (member != null) {
			if (!member.contains(role.getName()))
				return false;
			member.remove(role.getName());
			return true;
		} else
			return false;
	}

	@Override
	public Role[] getMembers() {
		List<Role> directMembers = new ArrayList<Role>();
		for (LdapName ldapName : getMemberNames()) {
			Role role = getUserAdmin().getRole(ldapName.toString());
			if (role == null) {
				if (getUserAdmin().getExternalRoles() != null)
					role = getUserAdmin().getExternalRoles().getRole(
							ldapName.toString());
			}
			if (role == null)
				throw new UserDirectoryException("No role found for "
						+ ldapName);
			directMembers.add(role);
		}
		return directMembers.toArray(new Role[directMembers.size()]);
	}

	@Override
	public List<LdapName> getMemberNames() {
		Attribute memberAttribute = getAttributes().get(memberAttributeId);
		if (memberAttribute == null)
			return new ArrayList<LdapName>();
		try {
			List<LdapName> roles = new ArrayList<LdapName>();
			NamingEnumeration<?> values = memberAttribute.getAll();
			while (values.hasMore()) {
				LdapName dn = new LdapName(values.next().toString());
				roles.add(dn);
			}
			return roles;
		} catch (Exception e) {
			throw new UserDirectoryException("Cannot get members", e);
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
