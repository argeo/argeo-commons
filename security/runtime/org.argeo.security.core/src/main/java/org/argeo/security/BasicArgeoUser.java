package org.argeo.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BasicArgeoUser implements ArgeoUser, Serializable {
	private static final long serialVersionUID = 1L;

	private String username;
	private List<UserNature> userNatures = new ArrayList<UserNature>();
	private List<String> roles = new ArrayList<String>();

	public BasicArgeoUser() {

	}

	public BasicArgeoUser(ArgeoUser argeoUser) {
		username = argeoUser.getUsername();
		userNatures = new ArrayList<UserNature>(argeoUser.getUserNatures());
		roles = new ArrayList<String>(argeoUser.getRoles());
	}

	public List<UserNature> getUserNatures() {
		return userNatures;
	}

	public List<String> getRoles() {
		return roles;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setUserNatures(List<UserNature> userNatures) {
		this.userNatures = userNatures;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}
}
