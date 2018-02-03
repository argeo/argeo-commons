package org.argeo.osgi.boot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.osgi.launch.EquinoxFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

public class OsgiBuilder {
	private Map<Integer, StartLevel> startLevels = new TreeMap<>();
	private List<String> distributionBundles = new ArrayList<>();

	private Map<String, String> configuration = new HashMap<String, String>();
	private Framework framework;

	public OsgiBuilder() {
		// configuration.put("osgi.clean", "true");
		configuration.put(OsgiBoot.CONFIGURATION_AREA_PROP, System.getProperty(OsgiBoot.CONFIGURATION_AREA_PROP));
		configuration.put(OsgiBoot.INSTANCE_AREA_PROP, System.getProperty(OsgiBoot.INSTANCE_AREA_PROP));
	}

	public Framework launch() {
		// start OSGi
		FrameworkFactory frameworkFactory = new EquinoxFactory();
		framework = frameworkFactory.newFramework(configuration);
		try {
			framework.start();
		} catch (BundleException e) {
			throw new OsgiBootException("Cannot start OSGi framework", e);
		}

		BundleContext bc = framework.getBundleContext();
		String osgiData = bc.getProperty(OsgiBoot.INSTANCE_AREA_PROP);
		// String osgiConf = bc.getProperty(OsgiBoot.CONFIGURATION_AREA_PROP);
		String osgiConf = framework.getDataFile("").getAbsolutePath();
		if (OsgiBootUtils.isDebug())
			OsgiBootUtils.debug("OSGi starting - data: " + osgiData + " conf: " + osgiConf);

		OsgiBoot osgiBoot = new OsgiBoot(framework.getBundleContext());
		// install bundles
		for (String distributionBundle : distributionBundles) {
			List<String> bundleUrls = osgiBoot.getDistributionUrls(distributionBundle, null);
			osgiBoot.installUrls(bundleUrls);
		}

		// start bundles
		osgiBoot.startBundles(startLevelsToProperties());

		// if (OsgiBootUtils.isDebug())
		// for (Bundle bundle : bc.getBundles()) {
		// OsgiBootUtils.debug(bundle.getLocation());
		// }
		return framework;
	}

	public OsgiBuilder conf(String key, String value) {
		checkNotLaunched();
		configuration.put(key, value);
		return this;
	}

	public OsgiBuilder install(String uri) {
		// TODO dynamic install
		checkNotLaunched();
		if (!distributionBundles.contains(uri))
			distributionBundles.add(uri);
		return this;
	}

	public OsgiBuilder start(int startLevel, String bundle) {
		// TODO dynamic start
		checkNotLaunched();
		StartLevel sl;
		if (!startLevels.containsKey(startLevel))
			startLevels.put(startLevel, new StartLevel());
		sl = startLevels.get(startLevel);
		sl.add(bundle);
		return this;
	}

	public BundleContext getBc() {
		checkLaunched();
		return framework.getBundleContext();
	}

	//
	// UTILITIES
	//
	private Properties startLevelsToProperties() {
		Properties properties = new Properties();
		for (Integer startLevel : startLevels.keySet()) {
			String property = OsgiBoot.PROP_ARGEO_OSGI_START + "." + startLevel;
			StringBuilder value = new StringBuilder();
			for (String bundle : startLevels.get(startLevel).getBundles()) {
				value.append(bundle);
				value.append(',');
			}
			// TODO remove trailing comma
			properties.put(property, value.toString());
		}
		return properties;
	}

	private void checkLaunched() {
		if (!isLaunched())
			throw new OsgiBootException("OSGi runtime is not launched");
	}

	private void checkNotLaunched() {
		if (isLaunched())
			throw new OsgiBootException("OSGi runtime already launched");
	}

	private boolean isLaunched() {
		return framework != null;
	}

	private static class StartLevel {
		private Set<String> bundles = new HashSet<>();

		public void add(String bundle) {
			String[] b = bundle.split(",");
			Collections.addAll(bundles, b);
		}

		public Set<String> getBundles() {
			return bundles;
		}
	}
}
