package org.argeo.api.acr;

/** Standard names. */
public enum CrName implements QNamed {

	/*
	 * TYPES
	 */
//	collection, // a collection type

	/*
	 * ATTRIBUTES
	 */
	uuid, // the UUID of a content
	mount, // a mount point
//	cc, // content class

	/*
	 * ATTRIBUTES FROM FILE SEMANTICS
	 */
//	creationTime, //
//	lastModifiedTime, //
//	size, //
	fileKey, //
//	owner, //
//	group, //
	permissions, //

	/*
	 * CONTENT NAMES
	 */
	root,

	//
	;

	

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
		return ArgeoNamespace.CR_NAMESPACE_URI;
	}

	@Override
	public String getDefaultPrefix() {
		return ArgeoNamespace.CR_DEFAULT_PREFIX;
	}

}
