package org.argeo.cms.osgi.useradmin;

import java.util.HashSet;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.directory.Attribute;
import javax.naming.ldap.LdapName;

import org.argeo.api.cms.directory.CmsRole;
import org.argeo.cms.directory.ldap.AbstractLdapDirectory;
import org.osgi.service.useradmin.Role;

/** Directory group implementation */
class LdifGroup extends LdifUser implements CmsOsgiGroup {
	private final String memberAttributeId;

	LdifGroup(AbstractLdapDirectory userAdmin, LdapName dn) {
		super(userAdmin, dn);
		memberAttributeId = userAdmin.getMemberAttributeId();
	}

	@Override
	public boolean addMember(Role role) {
		try {
			Role foundRole = findRole(new LdapName(role.getName()));
			if (foundRole == null)
				throw new UnsupportedOperationException(
						"Adding role " + role.getName() + " is unsupported within this context.");
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Role name" + role.getName() + " is badly formatted");
		}

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
		Set<CmsOsgiRole> directMembers = new HashSet<>();
		for (LdapName ldapName : getReferences(memberAttributeId)) {
			CmsOsgiRole role = findRole(ldapName);
			if (role == null) {
				throw new IllegalStateException("Role " + ldapName + " not found.");
			}
			directMembers.add(role);
		}
		return directMembers.toArray(new Role[directMembers.size()]);
	}

	@Override
	public Set<? extends CmsRole> getDirectMembers() {
		return doGetDirectMembers();
	}

	protected Set<CmsOsgiRole> doGetDirectMembers() {
		Set<CmsOsgiRole> directMembers = new HashSet<>();
		for (LdapName ldapName : getReferences(memberAttributeId)) {
			CmsOsgiRole role = findRole(ldapName);
			if (role == null) {
				throw new IllegalStateException("Role " + ldapName + " not found.");
			}
			directMembers.add(role);
		}
		return directMembers;
	}

	/**
	 * Whether a role with this name can be found from this context.
	 * 
	 * @return The related {@link Role} or <code>null</code>.
	 */
	protected CmsOsgiRole findRole(LdapName ldapName) {
		Role role = getUserAdmin().getRole(ldapName.toString());
		if (role == null) {
			if (getUserAdmin().getExternalRoles() != null)
				role = getUserAdmin().getExternalRoles().getRole(ldapName.toString());
		}
		return (CmsOsgiRole) role;
	}

//	@Override
//	public List<LdapName> getMemberNames() {
//		Attribute memberAttribute = getAttributes().get(memberAttributeId);
//		if (memberAttribute == null)
//			return new ArrayList<LdapName>();
//		try {
//			List<LdapName> roles = new ArrayList<LdapName>();
//			NamingEnumeration<?> values = memberAttribute.getAll();
//			while (values.hasMore()) {
//				LdapName dn = new LdapName(values.next().toString());
//				roles.add(dn);
//			}
//			return roles;
//		} catch (NamingException e) {
//			throw new IllegalStateException("Cannot get members", e);
//		}
//	}

	@Override
	public Role[] getRequiredMembers() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getType() {
		return GROUP;
	}

	protected DirectoryUserAdmin getUserAdmin() {
		return (DirectoryUserAdmin) getDirectory();
	}
}
