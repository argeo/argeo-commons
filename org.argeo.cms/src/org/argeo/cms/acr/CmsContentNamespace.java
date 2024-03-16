package org.argeo.cms.acr;

import static java.lang.System.Logger.Level.ERROR;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Objects;

import org.argeo.api.acr.ArgeoNamespace;
import org.argeo.api.acr.spi.ContentNamespace;

/** Content namespaces supported by CMS. */
public enum CmsContentNamespace implements ContentNamespace {
	//
	// ARGEO
	//
	CR(ArgeoNamespace.CR_DEFAULT_PREFIX, ArgeoNamespace.CR_NAMESPACE_URI, "cr.xsd", null),
	//
	SLC("slc", "http://www.argeo.org/ns/slc", null, null),
	//
	ARGEO("argeo", "http://www.argeo.org/ns/argeo", null, null),
	//
	// EXTERNAL
	//
	XSD("xs", "http://www.w3.org/2001/XMLSchema", "XMLSchema.xsd", "http://www.w3.org/2001/XMLSchema.xsd"),
	//
	XML("xml", "http://www.w3.org/XML/1998/namespace", "xml.xsd", "http://www.w3.org/2001/xml.xsd"),
	//
	XLINK("xlink", "http://www.w3.org/1999/xlink", "xlink.xsd", "https://www.w3.org/1999/xlink.xsd"),
	//
	WEBDAV("D", "DAV:", null, "https://raw.githubusercontent.com/lookfirst/sardine/master/webdav.xsd"),
	//
	XSLT("xsl", "http://www.w3.org/1999/XSL/Transform", "schema-for-xslt20.xsd",
			"https://www.w3.org/2007/schema-for-xslt20.xsd"),
	//
	SVG("svg", "http://www.w3.org/2000/svg", "SVG.xsd",
			"https://raw.githubusercontent.com/oreillymedia/HTMLBook/master/schema/svg/SVG.xsd"),
	//
	DSML("dsml", "urn:oasis:names:tc:DSML:2:0:core", "DSMLv2.xsd",
			"https://www.oasis-open.org/committees/dsml/docs/DSMLv2.xsd"),
	//
	;

	private final static String RESOURCE_BASE = "/org/argeo/cms/acr/schemas/";

	private String defaultPrefix;
	private String namespace;
	private URL resource;
	private URL publicUrl;

	CmsContentNamespace(String defaultPrefix, String namespace, String resourceFileName, String publicUrl) {
		Objects.requireNonNull(namespace);
		this.defaultPrefix = defaultPrefix;
		Objects.requireNonNull(namespace);
		this.namespace = namespace;
		if (resourceFileName != null) {
			// resource = getClass().getResource(RESOURCE_BASE + resourceFileName);
			try {
				// FIXME workaround when in nested OSGi frameworks
				resource = URI.create("platform:/plugin/org.argeo.cms" + RESOURCE_BASE + resourceFileName).toURL();
			} catch (MalformedURLException e) {
				resource = null;
				System.getLogger(CmsContentNamespace.class.getName()).log(ERROR,
						"Cannot load " + resourceFileName + ": " + e.getMessage());
				// throw new IllegalArgumentException("Cannot convert " + resourceFileName + "
				// to URL");
			}
			// Objects.requireNonNull(resource);
		}
		if (publicUrl != null)
			try {
				this.publicUrl = new URL(publicUrl);
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException("Cannot interpret public URL", e);
			}
	}

	@Override
	public String getDefaultPrefix() {
		return defaultPrefix;
	}

	@Override
	public String getNamespaceURI() {
		return namespace;
	}

	@Override
	public URL getSchemaResource() {
		return resource;
	}

	public URL getPublicUrl() {
		return publicUrl;
	}

}
