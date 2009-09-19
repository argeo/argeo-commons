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
import org.springframework.security.ldap.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.LdapUsernameToDnMapper;
import org.springframework.security.ldap.LdapUtils;
import org.springframework.security.ldap.populator.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UserDetailsManager;
import org.springframework.security.userdetails.ldap.LdapUserDetailsManager;
import org.springframework.security.userdetails.ldap.UserDetailsContextMapper;

public class ArgeoSecurityDaoLdap implements ArgeoSecurityDao, InitializingBean {
	// private final static Log log = LogFactory.getLog(UserDaoLdap.class);

	private UserDetailsManager userDetailsManager;
	private LdapAuthoritiesPopulator authoritiesPopulator;
	private String userBase = "ou=users";
	private String usernameAttributeName = "uid";
	private String groupBase = "ou=groups";
	private String groupRoleAttributeName = "cn";
	private String groupMemberAttributeName = "uniquemember";
	private String defaultRole = "ROLE_USER";
	private String rolePrefix = "ROLE_";

	private final LdapTemplate ldapTemplate;

	private LdapUsernameToDnMapper usernameMapper = null;

	private UserDetailsContextMapper userDetailsMapper;
	private List<UserNatureMapper> userNatureMappers;

	public void afterPropertiesSet() throws Exception {
		if (usernameMapper == null)
			usernameMapper = new DefaultLdapUsernameToDnMapper(userBase,
					usernameAttributeName);

		if (authoritiesPopulator == null) {
			DefaultLdapAuthoritiesPopulator ap = new DefaultLdapAuthoritiesPopulator(
					ldapTemplate.getContextSource(), groupBase);
			ap.setDefaultRole(defaultRole);
			ap.setGroupSearchFilter(groupMemberAttributeName + "={0}");
			authoritiesPopulator = ap;
		}

		if (userDetailsMapper == null) {
			ArgeoUserDetailsContextMapper audm = new ArgeoUserDetailsContextMapper();
			audm.setUserNatureMappers(userNatureMappers);
			userDetailsMapper = audm;
		}

		if (userDetailsManager == null) {
			LdapUserDetailsManager ludm = new LdapUserDetailsManager(
					ldapTemplate.getContextSource());
			ludm.setGroupSearchBase(groupBase);
			ludm.setUserDetailsMapper(userDetailsMapper);
			ludm.setUsernameMapper(usernameMapper);
			ludm.setGroupMemberAttributeName(groupMemberAttributeName);
			userDetailsManager = ludm;
		}

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
						return ctx.getStringAttribute(usernameAttributeName);
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
		return (List<String>) ldapTemplate.listBindings(groupBase,
				new ContextMapper() {
					public Object mapFromContext(Object ctxArg) {
						String groupName = ((DirContextAdapter) ctxArg)
								.getStringAttribute(groupRoleAttributeName);
						String roleName = convertGroupToRole(groupName);
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
		String group = role;
		if (group.startsWith(rolePrefix)) {
			group = group.substring(rolePrefix.length());
			group = group.toLowerCase();
		}
		return group;
	}

	public String convertGroupToRole(String groupName) {
		groupName = groupName.toUpperCase();

		return rolePrefix + groupName;
	}

	protected Name buildGroupDn(String name) {
		return new DistinguishedName(groupRoleAttributeName + "=" + name + ","
				+ groupBase);
	}

	public void setUserDetailsManager(UserDetailsManager userDetailsManager) {
		this.userDetailsManager = userDetailsManager;
	}

	public void setUserBase(String userBase) {
		this.userBase = userBase;
	}

	public void setUsernameAttributeName(String usernameAttribute) {
		this.usernameAttributeName = usernameAttribute;
	}

	public void setAuthoritiesPopulator(
			LdapAuthoritiesPopulator authoritiesPopulator) {
		this.authoritiesPopulator = authoritiesPopulator;
	}

	protected UserDetails getDetails(String username) {
		return userDetailsManager.loadUserByUsername(username);
	}

	public void setGroupBase(String groupBase) {
		this.groupBase = groupBase;
	}

	public void setGroupRoleAttributeName(String groupRoleAttributeName) {
		this.groupRoleAttributeName = groupRoleAttributeName;
	}

	public void setGroupMemberAttributeName(String groupMemberAttributeName) {
		this.groupMemberAttributeName = groupMemberAttributeName;
	}

	public void setDefaultRole(String defaultRole) {
		this.defaultRole = defaultRole;
	}

	public void setRolePrefix(String rolePrefix) {
		this.rolePrefix = rolePrefix;
	}

	public void setUsernameMapper(LdapUsernameToDnMapper usernameMapper) {
		this.usernameMapper = usernameMapper;
	}

	public void setUserDetailsMapper(UserDetailsContextMapper userDetailsMapper) {
		this.userDetailsMapper = userDetailsMapper;
	}

	public LdapAuthoritiesPopulator getAuthoritiesPopulator() {
		return authoritiesPopulator;
	}

	public UserDetailsContextMapper getUserDetailsMapper() {
		return userDetailsMapper;
	}

	public void setUserNatureMappers(List<UserNatureMapper> userNatureMappers) {
		this.userNatureMappers = userNatureMappers;
	}
}
