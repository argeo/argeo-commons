package org.argeo.geotools.osgi;

import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.geotools.GeoToolsConstants;
import org.geotools.data.DataStore;
import org.geotools.data.FileDataStoreFinder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Publishes as OSGi services (typically {@link DataStore}) resources contained
 * or references within the bundle declaring this activator.
 */
public class GisResourcesActivator implements BundleActivator {
	private final static Log log = LogFactory
			.getLog(GisResourcesActivator.class);

	/**
	 * Keep track of the registered datastores in order to dispose them when the
	 * bundle is stopped.
	 */
	private Map<String, DataStore> registeredDataStores = new HashMap<String, DataStore>();

	@SuppressWarnings("unchecked")
	public void start(BundleContext context) throws Exception {
		Bundle bundle = context.getBundle();

		// TODO deal with other data types
		// shapefiles
		Enumeration<URL> resources = bundle.findEntries("/", "*.shp", true);
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			DataStore ds = FileDataStoreFinder.getDataStore(url);
			Properties props = new Properties();
			String alias = url.getPath();
			props.setProperty(GeoToolsConstants.ALIAS_KEY, alias);
			context.registerService(DataStore.class.getName(), ds, props);
			registeredDataStores.put(alias, ds);
			if (log.isDebugEnabled())
				log.debug("Registered data store " + alias + ": " + ds);
		}
	}

	public void stop(BundleContext context) throws Exception {
		for (String alias : registeredDataStores.keySet()) {
			DataStore ds = registeredDataStores.get(alias);
			try {
				ds.dispose();
				if (log.isDebugEnabled())
					log.debug("Disposed data store " + alias + ": " + ds);
			} catch (Exception e) {
				log.warn("Could not dispose data store " + ds + ": "
						+ e.getMessage());
			}
		}

	}

}
