package org.argeo.security.jcr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;
import org.argeo.security.ArgeoUser;
import org.argeo.security.UserNature;

public class JcrArgeoUser implements ArgeoUser {
	/** Cached for performance reasons. */
	private final String username;
	private final Node home;
	private final List<String> roles;
	private final Boolean enabled;
	private final String password;

	public JcrArgeoUser(Node home, String password, List<String> roles,
			Boolean enabled) {
		this.home = home;
		this.password = password;
		this.roles = Collections.unmodifiableList(new ArrayList<String>(roles));
		this.enabled = enabled;
		try {
			username = home.getSession().getUserID();
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot find JCR user id", e);
		}

	}

	public String getUsername() {
		return username;
	}

	public Map<String, UserNature> getUserNatures() {
		throw new UnsupportedOperationException("deprecated");
	}

	public void updateUserNatures(Map<String, UserNature> userNatures) {
		throw new UnsupportedOperationException("deprecated");
	}

	public List<String> getRoles() {
		return roles;
	}

	public String getPassword() {
		return password;
	}

	public Node getHome() {
		return home;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof ArgeoUser))
			return false;
		return ((ArgeoUser) obj).getUsername().equals(username);
	}

	@Override
	public int hashCode() {
		return username.hashCode();
	}

	public String toString() {
		return getUsername() + "@" + getHome();
	}
}
