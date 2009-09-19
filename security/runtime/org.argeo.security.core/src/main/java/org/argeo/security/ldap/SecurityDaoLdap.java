package org.argeo.security.ldap;

import java.util.ArrayList;
import java.util.List;

import javax.naming.Name;

import org.argeo.security.ArgeoSecurityDao;
import org.argeo.security.ArgeoUser;
import org.argeo.security.core.ArgeoUserDetails;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UserDetailsManager;

public class SecurityDaoLdap implements ArgeoSecurityDao {
	// private final static Log log = LogFactory.getLog(UserDaoLdap.class);

	private UserDetailsManager userDetailsManager;
	private ArgeoLdapAuthoritiesPopulator authoritiesPopulator;
	private String userBase = "ou=users";
	private String usernameAttribute = "uid";

	private final LdapTemplate ldapTemplate;

	public SecurityDaoLdap(ContextSource contextSource) {
		ldapTemplate = new LdapTemplate(contextSource);
	}

	public void create(ArgeoUser user) {
		userDetailsManager.createUser(new ArgeoUserDetails(user));
	}

	public ArgeoUser getUser(String uname) {
		return (ArgeoUser) userDetailsManager.loadUserByUsername(uname);
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
			UserDetails userDetails = userDetailsManager
					.loadUserByUsername(username);
			lst.add((ArgeoUser) userDetails);
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

	public void deleteRole(String role) {
		if(true)
			throw new UnsupportedOperationException();
		
		Name dn = buildRoleDn(role);
		DirContextAdapter context = new DirContextAdapter();
		context.setAttributeValues("objectClass", new String[] { "top",
				"groupOfUniqueNames" });
		context.setAttributeValue("cn", role);
		ldapTemplate.bind(dn, context, null);
	}
	
	protected Name buildRoleDn(String name) {
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
}
