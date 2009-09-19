package org.argeo.security.nature;

import org.argeo.security.UserNature;

public class CoworkerNature extends UserNature {
	private static final long serialVersionUID = 1L;
	private String description;
	private String mobile;
	private String telephoneNumber;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getTelephoneNumber() {
		return telephoneNumber;
	}

	public void setTelephoneNumber(String telephoneNumber) {
		this.telephoneNumber = telephoneNumber;
	}

}
