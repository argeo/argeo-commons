package org.argeo.cms.internal.kernel;

import org.osgi.resource.Namespace;

/** CMS Data Model capability namespace. */
class DataModelNamespace extends Namespace {

	public static final String CMS_DATA_MODEL_NAMESPACE = "cms.datamodel";
	public static final String CAPABILITY_NAME_ATTRIBUTE = "name";
	public static final String CAPABILITY_CND_ATTRIBUTE = "cnd";

	private DataModelNamespace() {
		// empty
	}

}
