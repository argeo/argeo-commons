package org.argeo.security.ldap;

import java.util.ArrayList;
import java.util.List;

import org.argeo.security.ArgeoUser;
import org.argeo.security.core.ArgeoUserDetails;
import org.argeo.security.dao.UserDao;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UserDetailsManager;

public class UserDaoLdap implements UserDao {
	// private final static Log log = LogFactory.getLog(UserDaoLdap.class);

	private UserDetailsManager userDetailsManager;
	private String userBase = "ou=users";
	private String usernameAttribute = "uid";

	private final LdapTemplate ldapTemplate;

	public UserDaoLdap(ContextSource contextSource) {
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

	public void addRoles(String username, List<String> roles) {
		GrantedAuthority[] auths = new GrantedAuthority[roles.size()];
		for (int i = 0; i < roles.size(); i++)
			auths[i] = new GrantedAuthorityImpl(roles.get(i));
		ArgeoUserDetails user = (ArgeoUserDetails) userDetailsManager
				.loadUserByUsername(username);
		throw new UnsupportedOperationException();
		//userDetailsManager.
	}

	public void removeRoles(String username, List<String> roles) {
		throw new UnsupportedOperationException();
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
}
