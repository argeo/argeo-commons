package org.argeo.util.naming;

/**
 * Standard LDAP object classes as per
 * <a href="https://www.ldap.com/ldap-oid-reference">https://www.ldap.com/ldap-
 * oid-reference</a>
 */
public enum LdapObjs implements SpecifiedName {
	account("0.9.2342.19200300.100.4.5", "RFC 4524"),
	/** */
	document("0.9.2342.19200300.100.4.6", "RFC 4524"),
	/** */
	room("0.9.2342.19200300.100.4.7", "RFC 4524"),
	/** */
	documentSeries("0.9.2342.19200300.100.4.9", "RFC 4524"),
	/** */
	domain("0.9.2342.19200300.100.4.13", "RFC 4524"),
	/** */
	rFC822localPart("0.9.2342.19200300.100.4.14", "RFC 4524"),
	/** */
	domainRelatedObject("0.9.2342.19200300.100.4.17", "RFC 4524"),
	/** */
	friendlyCountry("0.9.2342.19200300.100.4.18", "RFC 4524"),
	/** */
	simpleSecurityObject("0.9.2342.19200300.100.4.19", "RFC 4524"),
	/** */
	uidObject("1.3.6.1.1.3.1", "RFC 4519"),
	/** */
	extensibleObject("1.3.6.1.4.1.1466.101.120.111", "RFC 4512"),
	/** */
	dcObject("1.3.6.1.4.1.1466.344", "RFC 4519"),
	/** */
	authPasswordObject("1.3.6.1.4.1.4203.1.4.7", "RFC 3112"),
	/** */
	namedObject("1.3.6.1.4.1.5322.13.1.1", "draft-howard-namedobject"),
	/** */
	inheritableLDAPSubEntry("1.3.6.1.4.1.7628.5.6.1.1", "draft-ietf-ldup-subentry"),
	/** */
	top("2.5.6.0", "RFC 4512"),
	/** */
	alias("2.5.6.1", "RFC 4512"),
	/** */
	country("2.5.6.2", "RFC 4519"),
	/** */
	locality("2.5.6.3", "RFC 4519"),
	/** */
	organization("2.5.6.4", "RFC 4519"),
	/** */
	organizationalUnit("2.5.6.5", "RFC 4519"),
	/** */
	person("2.5.6.6", "RFC 4519"),
	/** */
	organizationalPerson("2.5.6.7", "RFC 4519"),
	/** */
	organizationalRole("2.5.6.8", "RFC 4519"),
	/** */
	groupOfNames("2.5.6.9", "RFC 4519"),
	/** */
	residentialPerson("2.5.6.10", "RFC 4519"),
	/** */
	applicationProcess("2.5.6.11", "RFC 4519"),
	/** */
	device("2.5.6.14", "RFC 4519"),
	/** */
	strongAuthenticationUser("2.5.6.15", "RFC 4523"),
	/** */
	certificationAuthority("2.5.6.16", "RFC 4523"),
	// /** Should be certificationAuthority-V2 */
	// certificationAuthority_V2("2.5.6.16.2", "RFC 4523") {
	// },
	/** */
	groupOfUniqueNames("2.5.6.17", "RFC 4519"),
	/** */
	userSecurityInformation("2.5.6.18", "RFC 4523"),
	/** */
	cRLDistributionPoint("2.5.6.19", "RFC 4523"),
	/** */
	pkiUser("2.5.6.21", "RFC 4523"),
	/** */
	pkiCA("2.5.6.22", "RFC 4523"),
	/** */
	deltaCRL("2.5.6.23", "RFC 4523"),
	/** */
	subschema("2.5.20.1", "RFC 4512"),
	/** */
	ldapSubEntry("2.16.840.1.113719.2.142.6.1.1", "draft-ietf-ldup-subentry"),
	/** */
	changeLogEntry("2.16.840.1.113730.3.2.1", "draft-good-ldap-changelog"),
	/** */
	inetOrgPerson("2.16.840.1.113730.3.2.2", "RFC 2798"),
	/** */
	referral("2.16.840.1.113730.3.2.6", "RFC 3296");

	private final static String LDAP_ = "ldap:";
	private final String oid, spec;

	private LdapObjs(String oid, String spec) {
		this.oid = oid;
		this.spec = spec;
	}

	public String getOid() {
		return oid;
	}

	public String getSpec() {
		return spec;
	}

	public String property() {
		return new StringBuilder(LDAP_).append(name()).toString();
	}

}
