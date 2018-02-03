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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;

/** OSGi builder, focusing on ease of use for scripting. */
public class OsgiBuilder {
	private Map<Integer, StartLevel> startLevels = new TreeMap<>();
	private List<String> distributionBundles = new ArrayList<>();

	private Map<String, String> configuration = new HashMap<String, String>();
	private Framework framework;
	private String baseUrl = null;

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
			List<String> bundleUrls = osgiBoot.getDistributionUrls(distributionBundle, baseUrl);
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

	public OsgiBuilder waitForBundle(String bundles) {
		List<String> lst = new ArrayList<>();
		Collections.addAll(lst, bundles.split(","));
		BundleTracker<Object> bt = new BundleTracker<Object>(getBc(), Bundle.ACTIVE, null) {

			@Override
			public Object addingBundle(Bundle bundle, BundleEvent event) {
				if (lst.contains(bundle.getSymbolicName())) {
					return bundle.getSymbolicName();
				} else {
					return null;
				}
			}
		};
		bt.open();
		while (bt.getTrackingCount() != lst.size()) {
			try {
				Thread.sleep(500l);
			} catch (InterruptedException e) {
				break;
			}
		}
		bt.close();
		return this;

	}

	public Object service(String service) {
		return service(service, 0);
	}

	public Object service(String service, long timeout) {
		ServiceTracker<Object, Object> st;
		if (service.contains("(")) {
			try {
				st = new ServiceTracker<>(getBc(), FrameworkUtil.createFilter(service), null);
			} catch (InvalidSyntaxException e) {
				throw new IllegalArgumentException("Badly formatted filter", e);
			}
		} else {
			st = new ServiceTracker<>(getBc(), service, null);
		}
		st.open();
		try {
			return st.waitForService(timeout);
		} catch (InterruptedException e) {
			OsgiBootUtils.error("Interrupted", e);
			return null;
		} finally {
			st.close();
		}

	}

	public void shutdown() {
		checkLaunched();
		try {
			framework.stop();
		} catch (BundleException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.exit(0);
	}

	public Integer getHttpPort() {
		ServiceReference<?> sr = getBc().getServiceReference("org.osgi.service.http.HttpService");
		if (sr == null)
			return -1;
		Object port = sr.getProperty("http.port");
		if (port == null)
			return -1;
		return Integer.parseInt(port.toString());
	}

	public Integer getHttpsPort() {
		ServiceReference<?> sr = getBc().getServiceReference("org.osgi.service.http.HttpService");
		if (sr == null)
			return -1;
		Object port = sr.getProperty("https.port");
		if (port == null)
			return -1;
		return Integer.parseInt(port.toString());
	}

	public Object spring(String bundle) {
		return service("(&(Bundle-SymbolicName=" + bundle + ")"
				+ "(objectClass=org.springframework.context.ApplicationContext))");
	}

	//
	// BEAN
	//

	public BundleContext getBc() {
		checkLaunched();
		return framework.getBundleContext();
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
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
