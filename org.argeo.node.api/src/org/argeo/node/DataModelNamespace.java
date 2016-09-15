package org.argeo.node;

import org.osgi.resource.Namespace;

/** CMS Data Model capability namespace. */
public class DataModelNamespace extends Namespace {

	public static final String CMS_DATA_MODEL_NAMESPACE = "cms.datamodel";
	public static final String CAPABILITY_NAME_ATTRIBUTE = "name";
	public static final String CAPABILITY_CND_ATTRIBUTE = "cnd";
	/** If 'true', indicates that no repository should be published */
	public static final String CAPABILITY_ABSTRACT_ATTRIBUTE = "abstract";
	/**
	 * If 'true', indicates that code using this data model should be prepared
	 * to have it stored in a different JCR repository than the node
	 */
	public static final String CAPABILITY_STANDALONE_ATTRIBUTE = "standalone";

	private DataModelNamespace() {
		// empty
	}

}
