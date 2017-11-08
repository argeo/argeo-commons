package org.argeo.naming;

/**
 * Standard LDAP attributes as per:<br>
 * - <a href= "https://www.ldap.com/ldap-oid-reference">Standard LDAP</a><br>
 * - <a href=
 * "https://github.com/krb5/krb5/blob/master/src/plugins/kdb/ldap/libkdb_ldap/kerberos.schema">Kerberos
 * LDAP (partial)</a>
 */
public enum LdapAttrs implements SpecifiedName {
	/** */
	uid("0.9.2342.19200300.100.1.1", "RFC 4519"),
	/** */
	mail("0.9.2342.19200300.100.1.3", "RFC 4524"),
	/** */
	info("0.9.2342.19200300.100.1.4", "RFC 4524"),
	/** */
	drink("0.9.2342.19200300.100.1.5", "RFC 4524"),
	/** */
	roomNumber("0.9.2342.19200300.100.1.6", "RFC 4524"),
	/** */
	photo("0.9.2342.19200300.100.1.7", "RFC 2798"),
	/** */
	userClass("0.9.2342.19200300.100.1.8", "RFC 4524"),
	/** */
	host("0.9.2342.19200300.100.1.9", "RFC 4524"),
	/** */
	manager("0.9.2342.19200300.100.1.10", "RFC 4524"),
	/** */
	documentIdentifier("0.9.2342.19200300.100.1.11", "RFC 4524"),
	/** */
	documentTitle("0.9.2342.19200300.100.1.12", "RFC 4524"),
	/** */
	documentVersion("0.9.2342.19200300.100.1.13", "RFC 4524"),
	/** */
	documentAuthor("0.9.2342.19200300.100.1.14", "RFC 4524"),
	/** */
	documentLocation("0.9.2342.19200300.100.1.15", "RFC 4524"),
	/** */
	homePhone("0.9.2342.19200300.100.1.20", "RFC 4524"),
	/** */
	secretary("0.9.2342.19200300.100.1.21", "RFC 4524"),
	/** */
	dc("0.9.2342.19200300.100.1.25", "RFC 4519"),
	/** */
	associatedDomain("0.9.2342.19200300.100.1.37", "RFC 4524"),
	/** */
	associatedName("0.9.2342.19200300.100.1.38", "RFC 4524"),
	/** */
	homePostalAddress("0.9.2342.19200300.100.1.39", "RFC 4524"),
	/** */
	personalTitle("0.9.2342.19200300.100.1.40", "RFC 4524"),
	/** */
	mobile("0.9.2342.19200300.100.1.41", "RFC 4524"),
	/** */
	pager("0.9.2342.19200300.100.1.42", "RFC 4524"),
	/** */
	co("0.9.2342.19200300.100.1.43", "RFC 4524"),
	/** */
	uniqueIdentifier("0.9.2342.19200300.100.1.44", "RFC 4524"),
	/** */
	organizationalStatus("0.9.2342.19200300.100.1.45", "RFC 4524"),
	/** */
	buildingName("0.9.2342.19200300.100.1.48", "RFC 4524"),
	/** */
	audio("0.9.2342.19200300.100.1.55", "RFC 2798"),
	/** */
	documentPublisher("0.9.2342.19200300.100.1.56", "RFC 4524"),
	/** */
	jpegPhoto("0.9.2342.19200300.100.1.60", "RFC 2798"),
	/** */
	vendorName("1.3.6.1.1.4", "RFC 3045"),
	/** */
	vendorVersion("1.3.6.1.1.5", "RFC 3045"),
	/** */
	entryUUID("1.3.6.1.1.16.4", "RFC 4530"),
	/** */
	entryDN("1.3.6.1.1.20", "RFC 5020"),
	/** */
	labeledURI("1.3.6.1.4.1.250.1.57", "RFC 2798"),
	/** */
	numSubordinates("1.3.6.1.4.1.453.16.2.103", "draft-ietf-boreham-numsubordinates"),
	/** */
	namingContexts("1.3.6.1.4.1.1466.101.120.5", "RFC 4512"),
	/** */
	altServer("1.3.6.1.4.1.1466.101.120.6", "RFC 4512"),
	/** */
	supportedExtension("1.3.6.1.4.1.1466.101.120.7", "RFC 4512"),
	/** */
	supportedControl("1.3.6.1.4.1.1466.101.120.13", "RFC 4512"),
	/** */
	supportedSASLMechanisms("1.3.6.1.4.1.1466.101.120.14", "RFC 4512"),
	/** */
	supportedLDAPVersion("1.3.6.1.4.1.1466.101.120.15", "RFC 4512"),
	/** */
	ldapSyntaxes("1.3.6.1.4.1.1466.101.120.16", "RFC 4512"),
	/** */
	supportedAuthPasswordSchemes("1.3.6.1.4.1.4203.1.3.3", "RFC 3112"),
	/** */
	authPassword("1.3.6.1.4.1.4203.1.3.4", "RFC 3112"),
	/** */
	supportedFeatures("1.3.6.1.4.1.4203.1.3.5", "RFC 4512"),
	/** */
	inheritable("1.3.6.1.4.1.7628.5.4.1", "draft-ietf-ldup-subentry"),
	/** */
	blockInheritance("1.3.6.1.4.1.7628.5.4.2", "draft-ietf-ldup-subentry"),
	/** */
	objectClass("2.5.4.0", "RFC 4512"),
	/** */
	aliasedObjectName("2.5.4.1", "RFC 4512"),
	/** */
	cn("2.5.4.3", "RFC 4519"),
	/** */
	sn("2.5.4.4", "RFC 4519"),
	/** */
	serialNumber("2.5.4.5", "RFC 4519"),
	/** */
	c("2.5.4.6", "RFC 4519"),
	/** */
	l("2.5.4.7", "RFC 4519"),
	/** */
	st("2.5.4.8", "RFC 4519"),
	/** */
	street("2.5.4.9", "RFC 4519"),
	/** */
	o("2.5.4.10", "RFC 4519"),
	/** */
	ou("2.5.4.11", "RFC 4519"),
	/** */
	title("2.5.4.12", "RFC 4519"),
	/** */
	description("2.5.4.13", "RFC 4519"),
	/** */
	searchGuide("2.5.4.14", "RFC 4519"),
	/** */
	businessCategory("2.5.4.15", "RFC 4519"),
	/** */
	postalAddress("2.5.4.16", "RFC 4519"),
	/** */
	postalCode("2.5.4.17", "RFC 4519"),
	/** */
	postOfficeBox("2.5.4.18", "RFC 4519"),
	/** */
	physicalDeliveryOfficeName("2.5.4.19", "RFC 4519"),
	/** */
	telephoneNumber("2.5.4.20", "RFC 4519"),
	/** */
	telexNumber("2.5.4.21", "RFC 4519"),
	/** */
	teletexTerminalIdentifier("2.5.4.22", "RFC 4519"),
	/** */
	facsimileTelephoneNumber("2.5.4.23", "RFC 4519"),
	/** */
	x121Address("2.5.4.24", "RFC 4519"),
	/** */
	internationalISDNNumber("2.5.4.25", "RFC 4519"),
	/** */
	registeredAddress("2.5.4.26", "RFC 4519"),
	/** */
	destinationIndicator("2.5.4.27", "RFC 4519"),
	/** */
	preferredDeliveryMethod("2.5.4.28", "RFC 4519"),
	/** */
	member("2.5.4.31", "RFC 4519"),
	/** */
	owner("2.5.4.32", "RFC 4519"),
	/** */
	roleOccupant("2.5.4.33", "RFC 4519"),
	/** */
	seeAlso("2.5.4.34", "RFC 4519"),
	/** */
	userPassword("2.5.4.35", "RFC 4519"),
	/** */
	userCertificate("2.5.4.36", "RFC 4523"),
	/** */
	cACertificate("2.5.4.37", "RFC 4523"),
	/** */
	authorityRevocationList("2.5.4.38", "RFC 4523"),
	/** */
	certificateRevocationList("2.5.4.39", "RFC 4523"),
	/** */
	crossCertificatePair("2.5.4.40", "RFC 4523"),
	/** */
	name("2.5.4.41", "RFC 4519"),
	/** */
	givenName("2.5.4.42", "RFC 4519"),
	/** */
	initials("2.5.4.43", "RFC 4519"),
	/** */
	generationQualifier("2.5.4.44", "RFC 4519"),
	/** */
	x500UniqueIdentifier("2.5.4.45", "RFC 4519"),
	/** */
	dnQualifier("2.5.4.46", "RFC 4519"),
	/** */
	enhancedSearchGuide("2.5.4.47", "RFC 4519"),
	/** */
	distinguishedName("2.5.4.49", "RFC 4519"),
	/** */
	uniqueMember("2.5.4.50", "RFC 4519"),
	/** */
	houseIdentifier("2.5.4.51", "RFC 4519"),
	/** */
	supportedAlgorithms("2.5.4.52", "RFC 4523"),
	/** */
	deltaRevocationList("2.5.4.53", "RFC 4523"),
	/** */
	createTimestamp("2.5.18.1", "RFC 4512"),
	/** */
	modifyTimestamp("2.5.18.2", "RFC 4512"),
	/** */
	creatorsName("2.5.18.3", "RFC 4512"),
	/** */
	modifiersName("2.5.18.4", "RFC 4512"),
	/** */
	subschemaSubentry("2.5.18.10", "RFC 4512"),
	/** */
	dITStructureRules("2.5.21.1", "RFC 4512"),
	/** */
	dITContentRules("2.5.21.2", "RFC 4512"),
	/** */
	matchingRules("2.5.21.4", "RFC 4512"),
	/** */
	attributeTypes("2.5.21.5", "RFC 4512"),
	/** */
	objectClasses("2.5.21.6", "RFC 4512"),
	/** */
	nameForms("2.5.21.7", "RFC 4512"),
	/** */
	matchingRuleUse("2.5.21.8", "RFC 4512"),
	/** */
	structuralObjectClass("2.5.21.9", "RFC 4512"),
	/** */
	governingStructureRule("2.5.21.10", "RFC 4512"),
	/** */
	carLicense("2.16.840.1.113730.3.1.1", "RFC 2798"),
	/** */
	departmentNumber("2.16.840.1.113730.3.1.2", "RFC 2798"),
	/** */
	employeeNumber("2.16.840.1.113730.3.1.3", "RFC 2798"),
	/** */
	employeeType("2.16.840.1.113730.3.1.4", "RFC 2798"),
	/** */
	changeNumber("2.16.840.1.113730.3.1.5", "draft-good-ldap-changelog"),
	/** */
	targetDN("2.16.840.1.113730.3.1.6", "draft-good-ldap-changelog"),
	/** */
	changeType("2.16.840.1.113730.3.1.7", "draft-good-ldap-changelog"),
	/** */
	changes("2.16.840.1.113730.3.1.8", "draft-good-ldap-changelog"),
	/** */
	newRDN("2.16.840.1.113730.3.1.9", "draft-good-ldap-changelog"),
	/** */
	deleteOldRDN("2.16.840.1.113730.3.1.10", "draft-good-ldap-changelog"),
	/** */
	newSuperior("2.16.840.1.113730.3.1.11", "draft-good-ldap-changelog"),
	/** */
	ref("2.16.840.1.113730.3.1.34", "RFC 3296"),
	/** */
	changelog("2.16.840.1.113730.3.1.35", "draft-good-ldap-changelog"),
	/** */
	preferredLanguage("2.16.840.1.113730.3.1.39", "RFC 2798"),
	/** */
	userSMIMECertificate("2.16.840.1.113730.3.1.40", "RFC 2798"),
	/** */
	userPKCS12("2.16.840.1.113730.3.1.216", "RFC 2798"),
	/** */
	displayName("2.16.840.1.113730.3.1.241", "RFC 2798"),
	
	// Sun memberOf
	memberOf("1.2.840.113556.1.2.102","389 DS memberOf"),

	// KERBEROS (partial)
	krbPrincipalName("2.16.840.1.113719.1.301.6.8.1", "Novell Kerberos Schema Definitions"),

	// RFC 2985 and RFC 3039 (partial)
	dateOfBirth("1.3.6.1.5.5.7.9.1", "RFC 2985"),
	/** */
	placeOfBirth("1.3.6.1.5.5.7.9.2", "RFC 2985"),
	/** */
	gender("1.3.6.1.5.5.7.9.3", "RFC 2985"),
	/** */
	countryOfCitizenship("1.3.6.1.5.5.7.9.4", "RFC 2985"),
	/** */
	countryOfResidence("1.3.6.1.5.5.7.9.5", "RFC 2985"),
	//
	;

	public final static String DN = "dn";

	private final static String LDAP_ = "ldap:";

	private final String oid, spec;

	LdapAttrs(String oid, String spec) {
		this.oid = oid;
		this.spec = spec;
	}

	@Override
	public String getID() {
		return oid;
	}

	@Override
	public String getSpec() {
		return spec;
	}

	public String property() {
		return new StringBuilder(LDAP_).append(name()).toString();
	}

}
