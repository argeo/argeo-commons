package org.argeo.api.acr;

/** Standard names. */
public enum CrName implements QNamed {

	/*
	 * TYPES
	 */
	collection, // a collection type

	/*
	 * ATTRIBUTES
	 */
	uuid, // the UUID of a content
	mount, // a mount point
	cc, // content class

	/*
	 * ATTRIBUTES FROM FILE SEMANTICS
	 */
	creationTime, //
	lastModifiedTime, //
	size, //
	fileKey, //
	owner, //
	group, //
	permissions, //

	/*
	 * CONTENT NAMES
	 */
	root,

	//
	;

	public final static String CR_NAMESPACE_URI = "http://www.argeo.org/ns/cr";
	public final static String CR_DEFAULT_PREFIX = "cr";

	public final static String LDAP_NAMESPACE_URI = "http://www.argeo.org/ns/ldap";
	public final static String LDAP_DEFAULT_PREFIX = "ldap";

	public final static String ROLE_NAMESPACE_URI = "http://www.argeo.org/ns/role";
	public final static String ROLE_DEFAULT_PREFIX = "role";

//	private final ContentName value;

//	CrName() {
//		value = new ContentName(CR_NAMESPACE_URI, name(), RuntimeNamespaceContext.getNamespaceContext());
//	}
//
//	public QName qName() {
//		return value;
//	}

	@Override
	public String getNamespace() {
		return CR_NAMESPACE_URI;
	}

	@Override
	public String getDefaultPrefix() {
		return CR_DEFAULT_PREFIX;
	}

}
