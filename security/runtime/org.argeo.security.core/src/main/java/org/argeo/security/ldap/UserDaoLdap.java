package org.argeo.security.ldap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.security.ArgeoUser;
import org.argeo.security.BasicArgeoUser;
import org.argeo.security.core.ArgeoUserDetails;
import org.argeo.security.dao.UserDao;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.ldap.populator.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UserDetailsManager;

public class UserDaoLdap implements UserDao {
	private final static Log log = LogFactory.getLog(UserDaoLdap.class);

	private UserDetailsManager userDetailsManager;
	private DefaultLdapAuthoritiesPopulator authoritiesPopulator;
	private String userBase = "ou=users";

	private final LdapTemplate ldapTemplate;

	public UserDaoLdap(ContextSource contextSource) {
		ldapTemplate = new LdapTemplate(contextSource);
	}

	public void create(ArgeoUser user) {
		userDetailsManager.createUser((UserDetails) user);
	}

	public ArgeoUser getUser(String uname) {
		return (ArgeoUser) userDetailsManager.loadUserByUsername(uname);
	}

	@SuppressWarnings("unchecked")
	public List<ArgeoUser> listUsers() {
		List<String> usernames = (List<String>) ldapTemplate.listBindings(
				new DistinguishedName(userBase), new UserContextMapper());
		List<ArgeoUser> lst = new ArrayList<ArgeoUser>();
		for (String username : usernames) {
			UserDetails userDetails = userDetailsManager
					.loadUserByUsername(username);
			lst.add((ArgeoUser) userDetails);
		}
		return lst;
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

	public void setUserDetailsManager(UserDetailsManager userDetailsManager) {
		this.userDetailsManager = userDetailsManager;
	}

	public void setAuthoritiesPopulator(
			DefaultLdapAuthoritiesPopulator authoritiesPopulator) {
		this.authoritiesPopulator = authoritiesPopulator;
	}

	public void setUserBase(String userBase) {
		this.userBase = userBase;
	}

	class UserContextMapper implements ContextMapper {
		public Object mapFromContext(Object ctxArg) {
			DirContextAdapter ctx = (DirContextAdapter) ctxArg;
			// BasicArgeoUser user = new BasicArgeoUser();
			return ctx.getStringAttribute("uid");

			// log.debug("dn# " + ctx.getDn());
			// log.debug("NameInNamespace# " + ctx.getNameInNamespace());
			// log.debug("toString# " + ctx.toString());

			// Set<String> roles = authoritiesPopulator.getGroupMembershipRoles(
			// ctx.composeName(user.getUsername(), userBase), user
			// .getUsername());
			// user.setRoles(new ArrayList<String>(roles));
			// GrantedAuthority[] auths = authoritiesPopulator
			// .getGrantedAuthorities(ldapTemplate.,
			// user.getUsername());
			// for (GrantedAuthority auth : auths) {
			// user.getRoles().add(auth.getAuthority());
			// }
			// return user;
		}
	}

}
