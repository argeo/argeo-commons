package org.argeo.cms.integration;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** A serializable descriptor of a token. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenDescriptor implements Serializable {
	private static final long serialVersionUID = -6607393871416803324L;

	private String token;
	private String username;
	private String expiryDate;
//	private Set<String> roles;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

//	public Set<String> getRoles() {
//		return roles;
//	}
//
//	public void setRoles(Set<String> roles) {
//		this.roles = roles;
//	}

	public String getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(String expiryDate) {
		this.expiryDate = expiryDate;
	}

}
