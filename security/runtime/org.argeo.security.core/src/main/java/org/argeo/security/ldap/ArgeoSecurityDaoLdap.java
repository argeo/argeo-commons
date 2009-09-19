package org.argeo.security.ldap;

import static org.argeo.security.core.ArgeoUserDetails.createBasicArgeoUser;

import java.util.ArrayList;
import java.util.List;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.argeo.security.ArgeoSecurityDao;
import org.argeo.security.ArgeoUser;
import org.argeo.security.core.ArgeoUserDetails;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.ldap.core.ContextExecutor;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.ldap.DefaultLdapUsernameToDnMapper;
import org.springframework.security.ldap.LdapUsernameToDnMapper;
import org.springframework.security.ldap.LdapUtils;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UserDetailsManager;

public class ArgeoSecurityDaoLdap implements ArgeoSecurityDao, InitializingBean {
	// private final static Log log = LogFactory.getLog(UserDaoLdap.class);

	private UserDetailsManager userDetailsManager;
	private ArgeoLdapAuthoritiesPopulator authoritiesPopulator;
	private String userBase = "ou=users";
	private String usernameAttribute = "uid";

	private final LdapTemplate ldapTemplate;

	/* TODO: factorize with user details manager */
	private LdapUsernameToDnMapper usernameMapper = null;

	public void afterPropertiesSet() throws Exception {
		if (usernameMapper == null)
			usernameMapper = new DefaultLdapUsernameToDnMapper(userBase,
					usernameAttribute);
	}

	public ArgeoSecurityDaoLdap(ContextSource contextSource) {
		ldapTemplate = new LdapTemplate(contextSource);
	}

	public void create(ArgeoUser user) {
		userDetailsManager.createUser(new ArgeoUserDetails(user));
	}

	public ArgeoUser getUser(String uname) {
		return createBasicArgeoUser(getDetails(uname));
	}

	@SuppressWarnings("unchecked")
	public List<ArgeoUser> listUsers() {
		List<String> usernames = (List<String>) ldapTemplate.listBindings(
				new DistinguishedName(userBase), new ContextMapper() {
					public Object mapFromContext(Object ctxArg) {
						DirContextAdapter ctx = (DirContextAdapter) ctxArg;
						return ctx.getStringAttribute(usernameAttribute);
					}
				});

		List<ArgeoUser> lst = new ArrayList<ArgeoUser>();
		for (String username : usernames) {
			lst.add(createBasicArgeoUser(getDetails(username)));
		}
		return lst;
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

	public void update(ArgeoUser user) {
		userDetailsManager.updateUser(new ArgeoUserDetails(user));
	}

	public void delete(String username) {
		userDetailsManager.deleteUser(username);
	}

	public void updatePassword(String oldPassword, String newPassword) {
		userDetailsManager.changePassword(oldPassword, newPassword);
	}

	public Boolean userExists(String username) {
		return userDetailsManager.userExists(username);
	}

	public void createRole(String role, final String superuserName) {
		String group = convertRoleToGroup(role);
		DistinguishedName superuserDn = (DistinguishedName) ldapTemplate
				.executeReadWrite(new ContextExecutor() {
					public Object executeWithContext(DirContext ctx)
							throws NamingException {
						return LdapUtils.getFullDn(usernameMapper
								.buildDn(superuserName), ctx);
					}
				});

		Name groupDn = buildGroupDn(group);
		DirContextAdapter context = new DirContextAdapter();
		context.setAttributeValues("objectClass", new String[] { "top",
				"groupOfUniqueNames" });
		context.setAttributeValue("cn", group);

		// Add superuser because cannot create empty group
		context.setAttributeValue("uniqueMember", superuserDn.toString());

		ldapTemplate.bind(groupDn, context, null);
	}

	public void deleteRole(String role) {
		String group = convertRoleToGroup(role);
		Name dn = buildGroupDn(group);
		ldapTemplate.unbind(dn);
	}

	protected String convertRoleToGroup(String role) {
		// FIXME: factorize with spring security
		String group = role;
		if (group.startsWith("ROLE_")) {
			group = group.substring("ROLE_".length());
			group = group.toLowerCase();
		}
		return group;
	}

	protected Name buildGroupDn(String name) {
		return new DistinguishedName("cn=" + name + ","
				+ authoritiesPopulator.getGroupSearchBase());
	}

	public void setUserDetailsManager(UserDetailsManager userDetailsManager) {
		this.userDetailsManager = userDetailsManager;
	}

	public void setUserBase(String userBase) {
		this.userBase = userBase;
	}

	public void setUsernameAttribute(String usernameAttribute) {
		this.usernameAttribute = usernameAttribute;
	}

	public void setAuthoritiesPopulator(
			ArgeoLdapAuthoritiesPopulator authoritiesPopulator) {
		this.authoritiesPopulator = authoritiesPopulator;
	}

	protected UserDetails getDetails(String username) {
		return userDetailsManager.loadUserByUsername(username);
	}
}
