package org.argeo.security.ldap;

import java.util.List;

import javax.naming.Name;

import org.argeo.security.dao.RoleDao;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;

public class RoleDaoLdap implements RoleDao {

	private ArgeoLdapAuthoritiesPopulator authoritiesPopulator;
	private final LdapTemplate ldapTemplate;

	public RoleDaoLdap(ContextSource contextSource) {
		ldapTemplate = new LdapTemplate(contextSource);
	}

	public void create(String role) {
		Name dn = buildDn(role);
		DirContextAdapter context = new DirContextAdapter();
		context.setAttributeValues("objectClass", new String[] { "top",
				"groupOfUniqueNames" });
		context.setAttributeValue("cn", role);
		ldapTemplate.bind(dn, context, null);
	}

	@SuppressWarnings("unchecked")
	public List<String> listEditableRoles() {
		return (List<String>) ldapTemplate.listBindings(authoritiesPopulator
				.getGroupSearchBase(), new ContextMapper() {
			public Object mapFromContext(Object ctxArg) {
				String groupName = ((DirContextAdapter) ctxArg)
						.getStringAttribute(authoritiesPopulator
								.getGroupRoleAttribute());
				String roleName = authoritiesPopulator
						.convertGroupToRole(groupName);
				return roleName;
			}
		});
	}

	public void delete(String role) {
		// TODO Auto-generated method stub

	}

	public void setAuthoritiesPopulator(
			ArgeoLdapAuthoritiesPopulator ldapAuthoritiesPopulator) {
		this.authoritiesPopulator = ldapAuthoritiesPopulator;
	}

	protected Name buildDn(String name) {
		return new DistinguishedName("cn=" + name + ","
				+ authoritiesPopulator.getGroupSearchBase());
	}

}
