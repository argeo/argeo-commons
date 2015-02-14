package org.argeo.security.jcr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** Used to create a new user */
public class NewUserDetails extends User {
	private static final long serialVersionUID = -8331941336984083297L;

	public NewUserDetails(String username, char[] password) {
		this(username, password, null);
	}

	public NewUserDetails(String username, char[] password, String[] roles) {
		super(username, new String(password), false, false, false, false,
				rolesToAuthorities(roles));
	}

	/** To be overriden */
	public void mapToProfileNode(Node userProfile) throws RepositoryException {
		// does nothing by default
	}

	private static Collection<GrantedAuthority> rolesToAuthorities(
			String[] roles) {
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		if (roles != null)
			for (String role : roles) {
				authorities.add(new SimpleGrantedAuthority(role));
			}
		return authorities;
	}
}
