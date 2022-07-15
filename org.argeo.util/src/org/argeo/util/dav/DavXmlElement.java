package org.argeo.util.dav;

import javax.xml.namespace.QName;

import org.argeo.util.naming.QNamed;

public enum DavXmlElement implements QNamed {
	response, //
	href, //
	collection, //
	prop, //
	resourcetype, //

	// locking
	lockscope, //
	locktype, //
	supportedlock, //
	lockentry, //
	lockdiscovery, //
	write, //
	shared, //
	exclusive, //
	;

	final static String WEBDAV_NAMESPACE_URI = "DAV:";
	final static String WEBDAV_DEFAULT_PREFIX = "D";

	@Override
	public String getNamespace() {
		return WEBDAV_NAMESPACE_URI;
	}

	@Override
	public String getDefaultPrefix() {
		return WEBDAV_DEFAULT_PREFIX;
	}

	public static DavXmlElement toEnum(QName name) {
		for (DavXmlElement e : values()) {
			if (e.qName().equals(name))
				return e;
		}
		return null;
	}
}
