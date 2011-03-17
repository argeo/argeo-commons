package org.argeo.security.ldap.jcr;

import javax.jcr.Session;

import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.ldap.UserDetailsContextMapper;

public class JcrUserDetailsContextMapper implements UserDetailsContextMapper {
	private Session session;

	public UserDetails mapUserFromContext(DirContextOperations ctx,
			String username, GrantedAuthority[] authority) {
		// TODO Auto-generated method stub
		return null;
	}

	public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
		// TODO Auto-generated method stub

	}

}
