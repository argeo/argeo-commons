package org.argeo.api.init;

/** Supported init constants. */
public interface InitConstants {

	String PROP_ARGEO_OSGI_SOURCES = "argeo.osgi.sources";
	String PROP_ARGEO_OSGI_START = "argeo.osgi.start";
	String PROP_OSGI_INSTANCE_AREA = "osgi.instance.area";
	String PROP_OSGI_CONFIGURATION_AREA = "osgi.configuration.area";
	String PROP_OSGI_SHARED_CONFIGURATION_AREA = "osgi.sharedConfiguration.area";
	String PROP_ARGEO_OSGI_MAX_START_LEVEL = "argeo.osgi.maxStartLevel";

	// OSGi standard properties
	String PROP_OSGI_BUNDLES_DEFAULTSTARTLEVEL = "osgi.bundles.defaultStartLevel";
	String PROP_OSGI_STARTLEVEL = "osgi.startLevel";
	String PROP_OSGI_USE_SYSTEM_PROPERTIES = "osgi.framework.useSystemProperties";

	// Symbolic names
	String SYMBOLIC_NAME_INIT = "org.argeo.init";
	String SYMBOLIC_NAME_EQUINOX = "org.eclipse.osgi";

}
