package org.argeo.security.nature;

import org.argeo.security.UserNature;

public class SimpleUserNature extends UserNature {
	private static final long serialVersionUID = 1L;
	private String email;
	private String firstName;
	private String lastName;
	private String description;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
