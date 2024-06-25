package org.argeo.api.init;

/** Supported init constants. */
public interface InitConstants {
	/** Read-only configuration area */
	String PROP_ARGEO_CONFIG_AREA = "argeo.configArea";
	/** Read-write persistent data area */
	String PROP_ARGEO_STATE_AREA = "argeo.stateArea";
	/** Read-write cache area */
	String PROP_ARGEO_CACHE_AREA = "argeo.cacheArea";

	String PROP_ARGEO_OSGI_SOURCES = "argeo.osgi.sources";
	String PROP_ARGEO_OSGI_START = "argeo.osgi.start";

	String PROP_OSGI_USE_SYSTEM_PROPERTIES = "osgi.framework.useSystemProperties";

	String PROP_OSGI_INSTANCE_AREA = "osgi.instance.area";
	String PROP_OSGI_CONFIGURATION_AREA = "osgi.configuration.area";
	String PROP_OSGI_SHARED_CONFIGURATION_AREA = "osgi.sharedConfiguration.area";
	String PROP_OSGI_SHARED_CONFIGURATION_AREA_RO = "osgi.sharedConfiguration.area.readOnly";
	String PROP_ARGEO_OSGI_MAX_START_LEVEL = "argeo.osgi.maxStartLevel";
	String PROP_OSGI_BUNDLES_DEFAULTSTARTLEVEL = "osgi.bundles.defaultStartLevel";
	String PROP_OSGI_STARTLEVEL = "osgi.startLevel";

	// FOREIGN RUNTIME PROPERTIES
	/**
	 * UUID of the parent framework. It is set by the parent runtime and marks a
	 * nested runtime.
	 */
	String PROP_ARGEO_OSGI_PARENT_UUID = "argeo.osgi.parent.uuid";
//	@Deprecated
//	String PROP_ARGEO_OSGI_PARENT_CATEGORIES = "argeo.osgi.parent.categories";
	/** The A2 categories to export from the parent. */
	String PROP_ARGEO_OSGI_EXPORT_CATEGORIES = "argeo.osgi.export.categories";
	String PROP_ARGEO_OSGI_EXPORT_ENABLED = "argeo.osgi.export.enabled";

	// Symbolic names
	String SYMBOLIC_NAME_INIT = "org.argeo.init";
	String SYMBOLIC_NAME_EQUINOX = "org.eclipse.osgi";

}
