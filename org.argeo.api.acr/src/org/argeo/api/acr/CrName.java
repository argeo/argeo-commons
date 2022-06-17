package org.argeo.api.acr;

/** Standard names. */
public enum CrName implements ContentNameSupplier {

	/*
	 * TYPES
	 */
	COLLECTION, // a collection type

	/*
	 * ATTRIBUTES
	 */
	UUID, // the UUID of a content
	MOUNT,

	/*
	 * ATTRIBUTES FROM FILE SEMANTICS
	 */
	CREATION_TIME, //
	LAST_MODIFIED_TIME, //
	SIZE, //
	FILE_KEY, //
	OWNER, //
	GROUP, //
	PERMISSIONS, //

	/*
	 * CONTENT NAMES
	 */
	ROOT,

	//
	;

	public final static String CR_NAMESPACE_URI = "http://argeo.org/ns/cr";
//	public final static String CR_BASIC_NAMESPACE_URI = CR_NAMESPACE_URI + "/basic";
//	public final static String CR_OWNER_NAMESPACE_URI = CR_NAMESPACE_URI + "/owner";
//	public final static String CR_POSIX_NAMESPACE_URI = CR_NAMESPACE_URI + "/posix";

	public final static String CR_DEFAULT_PREFIX = "cr";

	public final static String LDAP_NAMESPACE_URI = "http://argeo.org/ns/ldap";
	public final static String LDAP_DEFAULT_PREFIX = "ldap";

	
	private final ContentName value;

	CrName() {
		value = toContentName();
	}

	@Override
	public ContentName get() {
		return value;
	}

	@Override
	public String getNamespaceURI() {
		return CR_NAMESPACE_URI;
	}

	@Override
	public String getDefaultPrefix() {
		return CR_DEFAULT_PREFIX;
	}

}
