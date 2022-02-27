package org.argeo.cms.osgi;

import org.osgi.resource.Namespace;

/** Namespace defining which resources can be published. Typically use to expose icon of scripts to the web. */
public class PublishNamespace extends Namespace {

	public static final String CMS_PUBLISH_NAMESPACE = "cms.publish";
	public static final String PKG = "pkg";
	public static final String FILE = "file";

	private PublishNamespace() {
		// empty
	}

}
