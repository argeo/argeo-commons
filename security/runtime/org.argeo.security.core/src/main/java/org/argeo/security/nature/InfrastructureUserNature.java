package org.argeo.security.nature;

import org.argeo.security.AbstractUserNature;

/**
 * Argeo infrastructure user nature. People with access to the infrastructure
 * must be properly identified.
 */
public class InfrastructureUserNature extends AbstractUserNature {
	private static final long serialVersionUID = 1L;

	private String mobile;
	private String telephoneNumber;
	private String postalAddress;
	private String postalCode;
	private String city;
	private String countryCode;

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

	public String getPostalAddress() {
		return postalAddress;
	}

	public void setPostalAddress(String postalAddress) {
		this.postalAddress = postalAddress;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

}
