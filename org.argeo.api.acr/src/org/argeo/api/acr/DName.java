package org.argeo.api.acr;

/**
 * Name for core concepts with the same semantics as defined in the WebDav
 * standard and extensions.
 * 
 * @see "http://www.webdav.org/specs/rfc4918.html"
 * @see "http://www.webdav.org/specs/rfc3744.html"
 */
public enum DName implements QNamed

{
	// RFC4918 (WebDav) properties used as CR attr
	creationdate, //
	displayname, //
	getcontentlanguage, //
	getcontentlength, //
	getcontenttype, //
	getetag, //
	getlastmodified, //
	resourcetype, //

	// RFC4918 (WebDav) value used as CR class
	collection, //

	// RFC3744 (ACL) properties uase as CR attr
	owner, //
	group, //
	//
	;

	public final static String WEBDAV_NAMESPACE_URI = "DAV:";
	public final static String WEBDAV_DEFAULT_PREFIX = "D";

	@Override
	public String getNamespace() {
		return WEBDAV_NAMESPACE_URI;
	}

	@Override
	public String getDefaultPrefix() {
		return WEBDAV_DEFAULT_PREFIX;
	}

}
