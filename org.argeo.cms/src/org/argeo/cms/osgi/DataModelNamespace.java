package org.argeo.cms.osgi;

import org.osgi.resource.Namespace;

/** CMS Data Model capability namespace. */
public class DataModelNamespace extends Namespace {

	public static final String CMS_DATA_MODEL_NAMESPACE = "cms.datamodel";
	public static final String NAME = "name";
	public static final String CND = "cnd";
	/** If 'true', indicates that no repository should be published */
	public static final String ABSTRACT = "abstract";

	private DataModelNamespace() {
		// empty
	}

}
