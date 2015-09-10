package org.argeo.osgi.useradmin;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;

public class LdifGroup extends LdifUser implements Group {
	// optimisation
	// List<Role> directMembers = null;

	private final UserAdmin userAdmin;
	private String memberAttrName = "member";

	public LdifGroup(UserAdmin userAdmin, LdapName dn, Attributes attributes) {
		super(dn, attributes);
		this.userAdmin = userAdmin;
	}

	@Override
	public boolean addMember(Role role) {
		Attribute member = getAttributes().get(memberAttrName);
		if (member != null) {
			if (member.contains(role.getName()))
				return false;
		} else
			getAttributes().put(memberAttrName, role.getName());
		// directMembers.add(role);
		// if (role instanceof LdifUser)
		// ((LdifUser) role).directMemberOf.add(this);
		return true;
	}

	@Override
	public boolean addRequiredMember(Role role) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeMember(Role role) {
		Attribute member = getAttributes().get(memberAttrName);
		if (member != null) {
			if (!member.contains(role.getName()))
				return false;
			member.remove(role.getName());
			// directMembers.remove(role);
			// if (role instanceof LdifUser)
			// ((LdifUser) role).directMemberOf.remove(this);
			return true;
		} else
			return false;
	}

	@Override
	public Role[] getMembers() {
		List<Role> directMembers = new ArrayList<Role>();
		for (LdapName ldapName : getMemberNames()) {
			Role role = userAdmin.getRole(ldapName.toString());
			if (role == null && userAdmin instanceof AbstractLdapUserAdmin) {
				AbstractLdapUserAdmin ua = (AbstractLdapUserAdmin) userAdmin;
				if (ua.getExternalRoles() != null)
					role = ua.getExternalRoles().getRole(ldapName.toString());
			}
			if (role == null)
				throw new ArgeoUserAdminException("No role found for "
						+ ldapName);

			// role.directMemberOf.add(group);
			// if (!directMemberOf.containsKey(role.getDn()))
			// directMemberOf.put(role.getDn(), new ArrayList<LdifGroup>());
			// directMemberOf.get(role.getDn()).add(group);
			directMembers.add(role);
		}
		return directMembers.toArray(new Role[directMembers.size()]);
		// if (directMembers != null)
		// return directMembers.toArray(new Role[directMembers.size()]);
		// else
		// throw new ArgeoUserAdminException("Members have not been loaded.");

		// Attribute memberAttribute = getAttributes().get(memberAttrName);
		// if (memberAttribute == null)
		// return new Role[0];
		// try {
		// List<Role> roles = new ArrayList<Role>();
		// NamingEnumeration values = memberAttribute.getAll();
		// while (values.hasMore()) {
		// LdapName dn = new LdapName(values.next().toString());
		// roles.add(new LdifUser(dn, null));
		// }
		// return roles.toArray(new Role[roles.size()]);
		// } catch (Exception e) {
		// throw new ArgeoUserAdminException("Cannot get members", e);
		// }
	}

	// void loadMembers(LdifUserAdmin userAdmin) {
	// directMembers = new ArrayList<Role>();
	// for (LdapName ldapName : getMemberNames()) {
	// LdifUser role;
	// if (userAdmin.groups.containsKey(ldapName))
	// role = userAdmin.groups.get(ldapName);
	// else if (userAdmin.users.containsKey(ldapName))
	// role = userAdmin.users.get(ldapName);
	// else
	// throw new ArgeoUserAdminException("No role found for "
	// + ldapName);
	// role.directMemberOf.add(this);
	// directMembers.add(role);
	// }
	// }

	List<LdapName> getMemberNames() {
		Attribute memberAttribute = getAttributes().get(memberAttrName);
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

	public String getMemberAttrName() {
		return memberAttrName;
	}

}
