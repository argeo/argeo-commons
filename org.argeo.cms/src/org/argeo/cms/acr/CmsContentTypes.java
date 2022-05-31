package org.argeo.cms.acr;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public enum CmsContentTypes {
	CR_2("cr", "http://argeo.org/ns/cr", "cr.xsd", null),
	//
	XSD_2001("xs", "http://www.w3.org/2001/XMLSchema", "XMLSchema.xsd", "http://www.w3.org/2001/XMLSchema.xsd"),
	//
	XML_1998("xml", "http://www.w3.org/XML/1998/namespace", "xml.xsd", "http://www.w3.org/2001/xml.xsd"),
	//
	XLINK_1999("xlink", "http://www.w3.org/1999/xlink", "xlink.xsd", "http://www.w3.org/XML/2008/06/xlink.xsd"),
	//
	XSLT_2_0("xsl", "http://www.w3.org/1999/XSL/Transform", "schema-for-xslt20.xsd", "https://www.w3.org/2007/schema-for-xslt20.xsd"),
	//
	SVG_1_1("svg", "http://www.w3.org/2000/svg", "SVG.xsd",
			"https://raw.githubusercontent.com/oreillymedia/HTMLBook/master/schema/svg/SVG.xsd"),
	//
	DOCBOOK_5_0_1("dbk","http://docbook.org/ns/docbook","docbook.xsd","http://docbook.org/xml/5.0.1/xsd/docbook.xsd"),
	//
	XML_EVENTS_2001("ev", "http://www.w3.org/2001/xml-events", "xml-events-attribs-1.xsd",
			"http://www.w3.org/MarkUp/SCHEMA/xml-events-attribs-1.xsd"),
	//
	XFORMS_2002("xforms", "http://www.w3.org/2002/xforms", "XForms-11-Schema.xsd",
			"https://www.w3.org/MarkUp/Forms/2007/XForms-11-Schema.xsd"),
	//
	DSML_v2("dsml", "urn:oasis:names:tc:DSML:2:0:core", "DSMLv2.xsd",
			"https://www.oasis-open.org/committees/dsml/docs/DSMLv2.xsd"),
	//
	;

	private final static String RESOURCE_BASE = "/org/argeo/cms/acr/schemas/";

	private String defaultPrefix;
	private String namespace;
	private URL resource;
	private URL publicUrl;

	CmsContentTypes(String defaultPrefix, String namespace, String resourceFileName, String publicUrl) {
		Objects.requireNonNull(namespace);
		this.defaultPrefix = defaultPrefix;
		Objects.requireNonNull(namespace);
		this.namespace = namespace;
		resource = getClass().getResource(RESOURCE_BASE + resourceFileName);
		Objects.requireNonNull(resource);
		if (publicUrl != null)
			try {
				this.publicUrl = new URL(publicUrl);
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException("Cannot interpret public URL", e);
			}
	}

	public String getDefaultPrefix() {
		return defaultPrefix;
	}

	public String getNamespace() {
		return namespace;
	}

	public URL getResource() {
		return resource;
	}

	public URL getPublicUrl() {
		return publicUrl;
	}

}
