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

	// RFC3744 (ACL) properties used as CR attr
	owner, //
	group, //

	// RFC3253 (versioning) properties used as CR attr
	checkedOut("checked-out"), //
	checkedIn("checked-in"), //
	//
	;

	public final static String WEBDAV_NAMESPACE_URI = "DAV:";
	public final static String WEBDAV_DEFAULT_PREFIX = "D";

	private final String localName;

	private DName(String localName) {
		assert localName != null;
		this.localName = localName;
	}

	private DName() {
		this.localName = null;
	}

	@Override
	public String localName() {
		if (localName != null)
			return localName;
		else
			return name();
	}

	@Override
	public String getNamespace() {
		return WEBDAV_NAMESPACE_URI;
	}

	@Override
	public String getDefaultPrefix() {
		return WEBDAV_DEFAULT_PREFIX;
	}

}
