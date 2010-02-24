package org.argeo.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SimpleArgeoUser implements ArgeoUser, Serializable {
	private static final long serialVersionUID = 1L;

	private String username;
	private String password;
	private List<UserNature> userNatures = new ArrayList<UserNature>();
	private List<String> roles = new ArrayList<String>();

	public SimpleArgeoUser() {

	}

	public SimpleArgeoUser(ArgeoUser argeoUser) {
		username = argeoUser.getUsername();
		password = argeoUser.getPassword();
		userNatures = new ArrayList<UserNature>(argeoUser.getUserNatures());
		roles = new ArrayList<String>(argeoUser.getRoles());
	}

	public List<UserNature> getUserNatures() {
		return userNatures;
	}

	public void updateUserNatures(List<UserNature> userNaturesData) {
		UserNature.updateUserNaturesWithCheck(userNatures, userNaturesData);
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

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
