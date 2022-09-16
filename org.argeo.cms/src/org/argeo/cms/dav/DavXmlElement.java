package org.argeo.cms.dav;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.argeo.api.acr.QNamed;

public enum DavXmlElement implements QNamed {
	response, //
	multistatus, //
	href, //
	/** MUST be the same as DName.collection */
	collection, //
	prop, //
	resourcetype, //

	// propfind
	propfind, //
	allprop, //
	propname, //
	include, //
	propstat, //

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

//	private final QName value;
//
//	private DavXmlElement() {
//		this.value = new ContentName(getNamespace(), localName(), RuntimeNamespaceContext.getNamespaceContext());
//	}
//
//	@Override
//	public QName qName() {
//		return value;
//	}

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

	public void setSimpleValue(XMLStreamWriter xsWriter, String value) throws XMLStreamException {
		if (value == null) {
			emptyElement(xsWriter);
			return;
		}
		startElement(xsWriter);
		xsWriter.writeCData(value);
		xsWriter.writeEndElement();
	}

	public void emptyElement(XMLStreamWriter xsWriter) throws XMLStreamException {
		xsWriter.writeEmptyElement(WEBDAV_NAMESPACE_URI, name());
	}

	public void startElement(XMLStreamWriter xsWriter) throws XMLStreamException {
		xsWriter.writeStartElement(WEBDAV_NAMESPACE_URI, name());
	}
}
