/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.osgi.boot;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import org.argeo.osgi.boot.internal.springutil.AntPathMatcher;
import org.argeo.osgi.boot.internal.springutil.PathMatcher;
import org.argeo.osgi.boot.internal.springutil.SystemPropertyUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Basic provisioning of an OSGi runtime via file path patterns and system
 * properties. Java 1.4 compatible.<br>
 * The approach is to generate list of URLs based on various methods, configured
 * via system properties.
 */
public class OsgiBoot {
	public final static String SYMBOLIC_NAME_OSGI_BOOT = "org.argeo.osgi.boot";
	public final static String SYMBOLIC_NAME_EQUINOX = "org.eclipse.osgi";

	public final static String PROP_OSGI_STARTLEVEL = "osgi.startLevel";
	public final static String PROP_OSGI_BUNDLES_DEFAULTSTARTLEVEL = "osgi.bundles.defaultStartLevel";

	public final static String PROP_ARGEO_OSGI_DATA_DIR = "argeo.osgi.data.dir";

	public final static String PROP_ARGEO_OSGI_START = "argeo.osgi.start";
	public final static String PROP_ARGEO_OSGI_BUNDLES = "argeo.osgi.bundles";
	public final static String PROP_ARGEO_OSGI_LOCATIONS = "argeo.osgi.locations";
	public final static String PROP_ARGEO_OSGI_BASE_URL = "argeo.osgi.baseUrl";
	/** @deprecated */
	public final static String PROP_ARGEO_OSGI_MODULES_URL = "argeo.osgi.modulesUrl";
	public final static String PROP_ARGEO_OSGI_DISTRIBUTION_URL = "argeo.osgi.distributionUrl";

	// booleans
	public final static String PROP_ARGEO_OSGI_BOOT_DEBUG = "argeo.osgi.boot.debug";
	public final static String PROP_ARGEO_OSGI_BOOT_EXCLUDE_SVN = "argeo.osgi.boot.excludeSvn";
	public final static String PROP_ARGEO_OSGI_BOOT_INSTALL_IN_LEXICOGRAPHIC_ORDER = "argeo.osgi.boot.installInLexicographicOrder";

	public final static String PROP_ARGEO_OSGI_BOOT_DEFAULT_TIMEOUT = "argeo.osgi.boot.defaultTimeout";
	public final static String PROP_ARGEO_OSGI_BOOT_MODULES_URL_SEPARATOR = "argeo.osgi.boot.modulesUrlSeparator";
	public final static String PROP_ARGEO_OSGI_BOOT_SYSTEM_PROPERTIES_FILE = "argeo.osgi.boot.systemPropertiesFile";
	public final static String PROP_ARGEO_OSGI_BOOT_APPCLASS = "argeo.osgi.boot.appclass";
	public final static String PROP_ARGEO_OSGI_BOOT_APPARGS = "argeo.osgi.boot.appargs";

	public final static String DEFAULT_BASE_URL = "reference:file:";
	public final static String EXCLUDES_SVN_PATTERN = "**/.svn/**";

	// OSGi system properties
	public final static String INSTANCE_AREA_PROP = "osgi.instance.area";
	public final static String INSTANCE_AREA_DEFAULT_PROP = "osgi.instance.area.default";

	private boolean debug = Boolean.valueOf(
			System.getProperty(PROP_ARGEO_OSGI_BOOT_DEBUG, "false"))
			.booleanValue();
	/** Exclude svn metadata implicitely(a bit costly) */
	private boolean excludeSvn = Boolean.valueOf(
			System.getProperty(PROP_ARGEO_OSGI_BOOT_EXCLUDE_SVN, "false"))
			.booleanValue();

	/**
	 * The {@link #installUrls(List)} methods won't follow the list order but
	 * order the urls according to the alphabetical order of the file names
	 * (last part of the URL). The goal is to stay closer from Eclipse PDE way
	 * of installing target platform bundles.
	 */
	private boolean installInLexicographicOrder = Boolean
			.valueOf(
					System.getProperty(
							PROP_ARGEO_OSGI_BOOT_INSTALL_IN_LEXICOGRAPHIC_ORDER,
							"true")).booleanValue();;

	/** Default is 10s (set in constructor) */
	private long defaultTimeout;

	/** Default is ',' (set in constructor) */
	private String modulesUrlSeparator = ",";

	private final BundleContext bundleContext;

	/*
	 * INITIALIZATION
	 */
	/** Constructor */
	public OsgiBoot(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		defaultTimeout = Long.parseLong(OsgiBootUtils.getProperty(
				PROP_ARGEO_OSGI_BOOT_DEFAULT_TIMEOUT, "10000"));
		modulesUrlSeparator = OsgiBootUtils.getProperty(
				PROP_ARGEO_OSGI_BOOT_MODULES_URL_SEPARATOR, ",");
		initSystemProperties();
	}

	/**
	 * Set additional system properties, especially ${argeo.osgi.data.dir} as an
	 * OS file path (and not a file:// URL)
	 */
	protected void initSystemProperties() {
		String osgiInstanceArea = System.getProperty(INSTANCE_AREA_PROP);
		String osgiInstanceAreaDefault = System
				.getProperty(INSTANCE_AREA_DEFAULT_PROP);
		String tempDir = System.getProperty("java.io.tmpdir");

		File dataDir = null;
		if (osgiInstanceArea != null) {
			// within OSGi with -data specified
			osgiInstanceArea = removeFilePrefix(osgiInstanceArea);
			dataDir = new File(osgiInstanceArea);
		} else if (osgiInstanceAreaDefault != null) {
			// within OSGi without -data specified
			osgiInstanceAreaDefault = removeFilePrefix(osgiInstanceAreaDefault);
			dataDir = new File(osgiInstanceAreaDefault);
		} else {// outside OSGi
			dataDir = new File(tempDir + File.separator + "argeoOsgiData");
		}
		System.setProperty(PROP_ARGEO_OSGI_DATA_DIR, dataDir.getAbsolutePath());
	}

	/*
	 * HIGH-LEVEL METHODS
	 */
	/** Bootstraps the OSGi runtime */
	public void bootstrap() {
		long begin = System.currentTimeMillis();
		System.out.println();
		OsgiBootUtils.info("OSGi bootstrap starting...");
		OsgiBootUtils.info("Writable data directory : "
				+ System.getProperty(PROP_ARGEO_OSGI_DATA_DIR)
				+ " (set as system property " + PROP_ARGEO_OSGI_DATA_DIR + ")");
		installUrls(getBundlesUrls());
		installUrls(getLocationsUrls());
		installUrls(getModulesUrls());
		installUrls(getDistributionUrls());
		checkUnresolved();
		startBundles();
		long duration = System.currentTimeMillis() - begin;
		OsgiBootUtils.info("OSGi bootstrap completed in "
				+ Math.round(((double) duration) / 1000) + "s (" + duration
				+ "ms), " + bundleContext.getBundles().length + " bundles");

		// display packages exported twice
		if (debug) {
			Map /* <String,Set<String>> */duplicatePackages = findPackagesExportedTwice();
			if (duplicatePackages.size() > 0) {
				OsgiBootUtils.info("Packages exported twice:");
				Iterator it = duplicatePackages.keySet().iterator();
				while (it.hasNext()) {
					String pkgName = it.next().toString();
					OsgiBootUtils.info(pkgName);
					Set bdles = (Set) duplicatePackages.get(pkgName);
					Iterator bdlesIt = bdles.iterator();
					while (bdlesIt.hasNext())
						OsgiBootUtils.info("  " + bdlesIt.next());
				}
			}
		}

		System.out.println();
	}

	/*
	 * INSTALLATION
	 */
	/** Install a single url. Convenience method. */
	public Bundle installUrl(String url) {
		List urls = new ArrayList();
		urls.add(url);
		installUrls(urls);
		return (Bundle) getBundlesByLocation().get(url);
	}

	/** Install the bundles at this URL list. */
	public void installUrls(List urls) {
		Map installedBundles = getBundlesByLocation();

		if (installInLexicographicOrder) {
			SortedMap map = new TreeMap();
			// reorder
			for (int i = 0; i < urls.size(); i++) {
				String url = (String) urls.get(i);
				int index = url.lastIndexOf('/');
				String fileName;
				if (index >= 0)
					fileName = url.substring(index + 1);
				else
					fileName = url;
				map.put(fileName, url);
			}

			// install
			Iterator keys = map.keySet().iterator();
			while (keys.hasNext()) {
				Object key = keys.next();
				String url = map.get(key).toString();
				installUrl(url, installedBundles);
			}
		} else {
			for (int i = 0; i < urls.size(); i++) {
				String url = (String) urls.get(i);
				installUrl(url, installedBundles);
			}
		}

	}

	/** Actually install the provided URL */
	protected void installUrl(String url, Map installedBundles) {
		try {
			if (installedBundles.containsKey(url)) {
				Bundle bundle = (Bundle) installedBundles.get(url);
				// bundle.update();
				if (debug)
					debug("Bundle " + bundle.getSymbolicName()
							+ " already installed from " + url);
			} else {
				Bundle bundle = bundleContext.installBundle(url);
				if (debug)
					debug("Installed bundle " + bundle.getSymbolicName()
							+ " from " + url);
			}
		} catch (BundleException e) {
			String message = e.getMessage();
			if ((message.contains("Bundle \"" + SYMBOLIC_NAME_OSGI_BOOT + "\"") || message
					.contains("Bundle \"" + SYMBOLIC_NAME_EQUINOX + "\""))
					&& message.contains("has already been installed")) {
				// silent, in order to avoid warnings: we know that both
				// have already been installed...
			} else {
				OsgiBootUtils.warn("Could not install bundle from " + url
						+ ": " + message);
			}
			if (debug)
				e.printStackTrace();
		}
	}

	/*
	 * START
	 */
	public void startBundles() {
		// default and active start levels from System properties
		Integer defaultStartLevel = new Integer(Integer.parseInt(OsgiBootUtils
				.getProperty(PROP_OSGI_BUNDLES_DEFAULTSTARTLEVEL, "4")));
		Integer activeStartLevel = new Integer(OsgiBootUtils.getProperty(
				PROP_OSGI_STARTLEVEL, "6"));

		SortedMap/* <Integer, List<String>> */startLevels = new TreeMap();
		computeStartLevels(startLevels, System.getProperties(),
				defaultStartLevel);

		Iterator/* <Integer> */levels = startLevels.keySet().iterator();
		while (levels.hasNext()) {
			Integer level = (Integer) levels.next();
			boolean allStarted = startBundles((List) startLevels.get(level));
			if (!allStarted)
				OsgiBootUtils
						.warn("Not all bundles started for level " + level);
			if (level.equals(activeStartLevel))
				break;// active start level reached
		}

	}

	public static void computeStartLevels(
			SortedMap/* <Integer, List<String>> */startLevels,
			Properties properties, Integer defaultStartLevel) {

		// default (and previously, only behaviour)
		appendToStartLevels(startLevels, defaultStartLevel,
				properties.getProperty(PROP_ARGEO_OSGI_START, ""));

		// list argeo.osgi.start.* system properties
		Iterator/* <String> */keys = properties.keySet().iterator();
		final String prefix = PROP_ARGEO_OSGI_START + ".";
		while (keys.hasNext()) {
			String key = (String) keys.next();
			if (key.startsWith(prefix)) {
				Integer startLevel;
				String suffix = key.substring(prefix.length());
				String[] tokens = suffix.split("\\.");
				if (tokens.length > 0 && !tokens[0].trim().equals(""))
					try {
						// first token is start level
						startLevel = new Integer(tokens[0]);
					} catch (NumberFormatException e) {
						startLevel = defaultStartLevel;
					}
				else
					startLevel = defaultStartLevel;

				// append bundle names
				String bundleNames = properties.getProperty(key);
				appendToStartLevels(startLevels, startLevel, bundleNames);
			}
		}
	}

	/** Append a comma-separated list of bundles to the start levels. */
	private static void appendToStartLevels(
			SortedMap/* <Integer, List<String>> */startLevels,
			Integer startLevel, String str) {
		if (str == null || str.trim().equals(""))
			return;

		if (!startLevels.containsKey(startLevel))
			startLevels.put(startLevel, new ArrayList());
		String[] bundleNames = str.split(",");
		for (int i = 0; i < bundleNames.length; i++) {
			if (bundleNames[i] != null && !bundleNames[i].trim().equals(""))
				((List) startLevels.get(startLevel)).add(bundleNames[i]);
		}
	}

	/**
	 * Convenience method accepting a comma-separated list of bundle to start
	 * 
	 * @deprecated
	 */
	public void startBundles(String bundlesToStartStr) {
		if (bundlesToStartStr == null)
			return;

		StringTokenizer st = new StringTokenizer(bundlesToStartStr, ",");
		List bundlesToStart = new ArrayList();
		while (st.hasMoreTokens()) {
			String name = st.nextToken().trim();
			bundlesToStart.add(name);
		}
		startBundles(bundlesToStart);
	}

	/**
	 * Start the provided list of bundles
	 * 
	 * @return whether all bundlesa are now in active state
	 */
	public boolean startBundles(List bundlesToStart) {
		if (bundlesToStart.size() == 0)
			return true;

		// used to monitor ACTIVE states
		List/* <Bundle> */startedBundles = new ArrayList();
		// used to log the bundles not found
		List/* <String> */notFoundBundles = new ArrayList(bundlesToStart);

		Bundle[] bundles = bundleContext.getBundles();
		long startBegin = System.currentTimeMillis();
		for (int i = 0; i < bundles.length; i++) {
			Bundle bundle = bundles[i];
			String symbolicName = bundle.getSymbolicName();
			if (bundlesToStart.contains(symbolicName))
				try {
					try {
						bundle.start();
						if (debug)
							debug("Bundle " + symbolicName + " started");
					} catch (Exception e) {
						OsgiBootUtils.warn("Start of bundle " + symbolicName
								+ " failed because of " + e
								+ ", maybe bundle is not yet resolved,"
								+ " waiting and trying again.");
						waitForBundleResolvedOrActive(startBegin, bundle);
						bundle.start();
						startedBundles.add(bundle);
					}
					notFoundBundles.remove(symbolicName);
				} catch (Exception e) {
					OsgiBootUtils.warn("Bundle " + symbolicName
							+ " cannot be started: " + e.getMessage());
					if (debug)
						e.printStackTrace();
					// was found even if start failed
					notFoundBundles.remove(symbolicName);
				}
		}

		for (int i = 0; i < notFoundBundles.size(); i++)
			OsgiBootUtils.warn("Bundle '" + notFoundBundles.get(i)
					+ "' not started because it was not found.");

		// monitors that all bundles are started
		long beginMonitor = System.currentTimeMillis();
		boolean allStarted = !(startedBundles.size() > 0);
		List/* <String> */notStarted = new ArrayList();
		while (!allStarted
				&& (System.currentTimeMillis() - beginMonitor) < defaultTimeout) {
			notStarted = new ArrayList();
			allStarted = true;
			for (int i = 0; i < startedBundles.size(); i++) {
				Bundle bundle = (Bundle) startedBundles.get(i);
				// TODO check behaviour of lazs bundles
				if (bundle.getState() != Bundle.ACTIVE) {
					allStarted = false;
					notStarted.add(bundle.getSymbolicName());
				}
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// silent
			}
		}
		long duration = System.currentTimeMillis() - beginMonitor;

		if (!allStarted)
			for (int i = 0; i < notStarted.size(); i++)
				OsgiBootUtils.warn("Bundle '" + notStarted.get(i)
						+ "' not ACTIVE after " + (duration / 1000) + "s");

		return allStarted;
	}

	/*
	 * DIAGNOSTICS
	 */
	/** Check unresolved bundles */
	protected void checkUnresolved() {
		// Refresh
		ServiceReference packageAdminRef = bundleContext
				.getServiceReference(PackageAdmin.class.getName());
		PackageAdmin packageAdmin = (PackageAdmin) bundleContext
				.getService(packageAdminRef);
		packageAdmin.resolveBundles(null);

		Bundle[] bundles = bundleContext.getBundles();
		List /* Bundle */unresolvedBundles = new ArrayList();
		for (int i = 0; i < bundles.length; i++) {
			int bundleState = bundles[i].getState();
			if (!(bundleState == Bundle.ACTIVE
					|| bundleState == Bundle.RESOLVED || bundleState == Bundle.STARTING))
				unresolvedBundles.add(bundles[i]);
		}

		if (unresolvedBundles.size() != 0) {
			OsgiBootUtils.warn("Unresolved bundles " + unresolvedBundles);
		}
	}

	/** List packages exported twice. */
	public Map findPackagesExportedTwice() {
		ServiceReference paSr = bundleContext
				.getServiceReference(PackageAdmin.class.getName());
		PackageAdmin packageAdmin = (PackageAdmin) bundleContext
				.getService(paSr);

		// find packages exported twice
		Bundle[] bundles = bundleContext.getBundles();
		Map /* <String,Set<String>> */exportedPackages = new TreeMap();
		for (int i = 0; i < bundles.length; i++) {
			Bundle bundle = bundles[i];
			ExportedPackage[] pkgs = packageAdmin.getExportedPackages(bundle);
			if (pkgs != null)
				for (int j = 0; j < pkgs.length; j++) {
					String pkgName = pkgs[j].getName();
					if (!exportedPackages.containsKey(pkgName)) {
						exportedPackages.put(pkgName, new TreeSet());
					}
					((Set) exportedPackages.get(pkgName)).add(bundle
							.getSymbolicName() + "_" + bundle.getVersion());
				}
		}
		Map /* <String,Set<String>> */duplicatePackages = new TreeMap();
		Iterator it = exportedPackages.keySet().iterator();
		while (it.hasNext()) {
			String pkgName = it.next().toString();
			Set bdles = (Set) exportedPackages.get(pkgName);
			if (bdles.size() > 1)
				duplicatePackages.put(pkgName, bdles);
		}
		return duplicatePackages;
	}

	/** Waits for a bundle to become active or resolved */
	protected void waitForBundleResolvedOrActive(long startBegin, Bundle bundle)
			throws Exception {
		int originalState = bundle.getState();
		if ((originalState == Bundle.RESOLVED)
				|| (originalState == Bundle.ACTIVE))
			return;

		String originalStateStr = OsgiBootUtils.stateAsString(originalState);

		int currentState = bundle.getState();
		while (!(currentState == Bundle.RESOLVED || currentState == Bundle.ACTIVE)) {
			long now = System.currentTimeMillis();
			if ((now - startBegin) > defaultTimeout * 10)
				throw new Exception("Bundle " + bundle.getSymbolicName()
						+ " was not RESOLVED or ACTIVE after "
						+ (now - startBegin) + "ms (originalState="
						+ originalStateStr + ", currentState="
						+ OsgiBootUtils.stateAsString(currentState) + ")");

			try {
				Thread.sleep(100l);
			} catch (InterruptedException e) {
				// silent
			}
			currentState = bundle.getState();
		}
	}

	/*
	 * EXPLICIT LOCATIONS INSTALLATION
	 */
	/** Gets the list of resolved explicit URL locations. */
	public List getLocationsUrls() {
		String baseUrl = OsgiBootUtils.getProperty(PROP_ARGEO_OSGI_BASE_URL,
				DEFAULT_BASE_URL);
		String bundleLocations = OsgiBootUtils
				.getProperty(PROP_ARGEO_OSGI_LOCATIONS);
		return getLocationsUrls(baseUrl, bundleLocations);
	}

	/**
	 * Gets a list of URLs based on explicit locations, resolving placeholder
	 * ${...} containing system properties, e.g. ${user.home}.
	 */
	public List getLocationsUrls(String baseUrl, String bundleLocations) {
		List urls = new ArrayList();

		if (bundleLocations == null)
			return urls;
		bundleLocations = SystemPropertyUtils
				.resolvePlaceholders(bundleLocations);
		if (debug)
			debug(PROP_ARGEO_OSGI_LOCATIONS + "=" + bundleLocations);

		StringTokenizer st = new StringTokenizer(bundleLocations,
				File.pathSeparator);
		while (st.hasMoreTokens()) {
			urls.add(locationToUrl(baseUrl, st.nextToken().trim()));
		}
		return urls;
	}

	/*
	 * BUNDLE PATTERNS INSTALLATION
	 */
	/**
	 * Computes a list of URLs based on Ant-like incluide/exclude patterns
	 * defined by ${argeo.osgi.bundles} with the following format:<br>
	 * <code>/base/directory;in=*.jar;in=**;ex=org.eclipse.osgi_*;jar</code><br>
	 * WARNING: <code>/base/directory;in=*.jar,\</code> at the end of a file,
	 * without a new line causes a '.' to be appended with unexpected side
	 * effects.
	 */
	public List getBundlesUrls() {
		String bundlePatterns = OsgiBootUtils
				.getProperty(PROP_ARGEO_OSGI_BUNDLES);
		return getBundlesUrls(bundlePatterns);
	}

	/**
	 * Compute alist of URLs to install based on the provided patterns, with
	 * default base url
	 */
	public List getBundlesUrls(String bundlePatterns) {
		String baseUrl = OsgiBootUtils.getProperty(PROP_ARGEO_OSGI_BASE_URL,
				DEFAULT_BASE_URL);
		return getBundlesUrls(baseUrl, bundlePatterns);
	}

	/** Implements the path matching logic */
	List getBundlesUrls(String baseUrl, String bundlePatterns) {
		List urls = new ArrayList();
		if (bundlePatterns == null)
			return urls;

		bundlePatterns = SystemPropertyUtils
				.resolvePlaceholders(bundlePatterns);
		if (debug)
			debug(PROP_ARGEO_OSGI_BUNDLES + "=" + bundlePatterns
					+ " (excludeSvn=" + excludeSvn + ")");

		StringTokenizer st = new StringTokenizer(bundlePatterns, ",");
		List bundlesSets = new ArrayList();
		while (st.hasMoreTokens()) {
			bundlesSets.add(new BundlesSet(st.nextToken()));
		}

		// find included
		List included = new ArrayList();
		PathMatcher matcher = new AntPathMatcher();
		for (int i = 0; i < bundlesSets.size(); i++) {
			BundlesSet bundlesSet = (BundlesSet) bundlesSets.get(i);
			for (int j = 0; j < bundlesSet.getIncludes().size(); j++) {
				String pattern = (String) bundlesSet.getIncludes().get(j);
				match(matcher, included, bundlesSet.getDir(), null, pattern);
			}
		}

		// find excluded
		List excluded = new ArrayList();
		for (int i = 0; i < bundlesSets.size(); i++) {
			BundlesSet bundlesSet = (BundlesSet) bundlesSets.get(i);
			for (int j = 0; j < bundlesSet.getExcludes().size(); j++) {
				String pattern = (String) bundlesSet.getExcludes().get(j);
				match(matcher, excluded, bundlesSet.getDir(), null, pattern);
			}
		}

		// construct list
		for (int i = 0; i < included.size(); i++) {
			String fullPath = (String) included.get(i);
			if (!excluded.contains(fullPath))
				urls.add(locationToUrl(baseUrl, fullPath));
		}

		return urls;
	}

	/*
	 * DISTRIBUTION JAR INSTALLATION
	 */
	public List getDistributionUrls() {
		List urls = new ArrayList();
		String distributionUrl = OsgiBootUtils
				.getProperty(PROP_ARGEO_OSGI_DISTRIBUTION_URL);
		if (distributionUrl == null)
			return urls;
		String baseUrl = OsgiBootUtils.getProperty(PROP_ARGEO_OSGI_BASE_URL);

		DistributionBundle distributionBundle;
		if (baseUrl != null
				&& !(distributionUrl.startsWith("http") || distributionUrl
						.startsWith("file"))) {
			// relative url
			distributionBundle = new DistributionBundle(baseUrl,
					distributionUrl);
		} else {
			distributionBundle = new DistributionBundle(distributionUrl);
			if (baseUrl != null)
				distributionBundle.setBaseUrl(baseUrl);

		}
		distributionBundle.processUrl();
		return distributionBundle.listUrls();
	}

	/*
	 * MODULES LIST INSTALLATION (${argeo.osgi.modulesUrl})
	 */
	/**
	 * Downloads a list of URLs in CSV format from ${argeo.osgi.modulesUrl}:<br>
	 * <code>Bundle-SymbolicName,Bundle-Version,url</code>)<br>
	 * If ${argeo.osgi.baseUrl} is set, URLs will be considered relative paths
	 * and be concatenated with the base URL, typically the root of a Maven
	 * repository.
	 * 
	 * @deprecated
	 */
	public List getModulesUrls() {
		List urls = new ArrayList();
		String modulesUrlStr = OsgiBootUtils
				.getProperty(PROP_ARGEO_OSGI_MODULES_URL);
		if (modulesUrlStr == null)
			return urls;

		String baseUrl = OsgiBootUtils.getProperty(PROP_ARGEO_OSGI_BASE_URL);

		Map installedBundles = getBundlesBySymbolicName();

		BufferedReader reader = null;
		try {
			URL modulesUrl = new URL(modulesUrlStr);
			reader = new BufferedReader(new InputStreamReader(
					modulesUrl.openStream()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line,
						modulesUrlSeparator);
				String moduleName = st.nextToken();
				String moduleVersion = st.nextToken();
				String url = st.nextToken();
				if (baseUrl != null)
					url = baseUrl + url;

				if (installedBundles.containsKey(moduleName)) {
					Bundle bundle = (Bundle) installedBundles.get(moduleName);
					String bundleVersion = bundle.getHeaders()
							.get(Constants.BUNDLE_VERSION).toString();
					int comp = OsgiBootUtils.compareVersions(bundleVersion,
							moduleVersion);
					if (comp > 0) {
						OsgiBootUtils.warn("Installed version " + bundleVersion
								+ " of bundle " + moduleName
								+ " is newer than  provided version "
								+ moduleVersion);
					} else if (comp < 0) {
						urls.add(url);
						OsgiBootUtils.info("Updated bundle " + moduleName
								+ " with version " + moduleVersion
								+ " (old version was " + bundleVersion + ")");
					} else {
						// do nothing
					}
				} else {
					urls.add(url);
				}
			}
		} catch (Exception e1) {
			throw new RuntimeException("Cannot read url " + modulesUrlStr, e1);
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return urls;
	}

	/*
	 * HIGH LEVEL UTILITIES
	 */
	/** Actually performs the matching logic. */
	protected void match(PathMatcher matcher, List matched, String base,
			String currentPath, String pattern) {
		if (currentPath == null) {
			// Init
			File baseDir = new File(base.replace('/', File.separatorChar));
			File[] files = baseDir.listFiles();

			if (files == null) {
				if (debug)
					OsgiBootUtils.warn("Base dir " + baseDir
							+ " has no children, exists=" + baseDir.exists()
							+ ", isDirectory=" + baseDir.isDirectory());
				return;
			}

			for (int i = 0; i < files.length; i++)
				match(matcher, matched, base, files[i].getName(), pattern);
		} else {
			String fullPath = base + '/' + currentPath;
			if (matched.contains(fullPath))
				return;// don't try deeper if already matched

			boolean ok = matcher.match(pattern, currentPath);
			// if (debug)
			// debug(currentPath + " " + (ok ? "" : " not ")
			// + " matched with " + pattern);
			if (ok) {
				matched.add(fullPath);
				return;
			} else {
				String newFullPath = relativeToFullPath(base, currentPath);
				File newFile = new File(newFullPath);
				File[] files = newFile.listFiles();
				if (files != null) {
					for (int i = 0; i < files.length; i++) {
						String newCurrentPath = currentPath + '/'
								+ files[i].getName();
						if (files[i].isDirectory()) {
							if (matcher.matchStart(pattern, newCurrentPath)) {
								// recurse only if start matches
								match(matcher, matched, base, newCurrentPath,
										pattern);
							} else {
								if (debug)
									debug(newCurrentPath
											+ " does not start match with "
											+ pattern);

							}
						} else {
							boolean nonDirectoryOk = matcher.match(pattern,
									newCurrentPath);
							if (debug)
								debug(currentPath + " " + (ok ? "" : " not ")
										+ " matched with " + pattern);
							if (nonDirectoryOk)
								matched.add(relativeToFullPath(base,
										newCurrentPath));
						}
					}
				}
			}
		}
	}

	protected void matchFile() {

	}

	/*
	 * LOW LEVEL UTILITIES
	 */
	/**
	 * The bundles already installed. Key is location (String) , value is a
	 * {@link Bundle}
	 */
	public Map getBundlesByLocation() {
		Map installedBundles = new HashMap();
		Bundle[] bundles = bundleContext.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			installedBundles.put(bundles[i].getLocation(), bundles[i]);
		}
		return installedBundles;
	}

	/**
	 * The bundles already installed. Key is symbolic name (String) , value is a
	 * {@link Bundle}
	 */
	public Map getBundlesBySymbolicName() {
		Map namedBundles = new HashMap();
		Bundle[] bundles = bundleContext.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			namedBundles.put(bundles[i].getSymbolicName(), bundles[i]);
		}
		return namedBundles;
	}

	/** Creates an URL from a location */
	protected String locationToUrl(String baseUrl, String location) {
		int extInd = location.lastIndexOf('.');
		String ext = null;
		if (extInd > 0)
			ext = location.substring(extInd);

		if (baseUrl.startsWith("reference:") && ".jar".equals(ext))
			return "file:" + location;
		else
			return baseUrl + location;
	}

	/** Transforms a relative path in a full system path. */
	protected String relativeToFullPath(String basePath, String relativePath) {
		return (basePath + '/' + relativePath).replace('/', File.separatorChar);
	}

	private String removeFilePrefix(String url) {
		if (url.startsWith("file:"))
			return url.substring("file:".length());
		else if (url.startsWith("reference:file:"))
			return url.substring("reference:file:".length());
		else
			return url;
	}

	/**
	 * Convenience method to avoid cluttering the code with
	 * OsgiBootUtils.debug()
	 */
	protected void debug(Object obj) {
		OsgiBootUtils.debug(obj);
	}

	/*
	 * BEAN METHODS
	 */

	public boolean getDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}

	public void setInstallInLexicographicOrder(
			boolean installInAlphabeticalOrder) {
		this.installInLexicographicOrder = installInAlphabeticalOrder;
	}

	public boolean isInstallInLexicographicOrder() {
		return installInLexicographicOrder;
	}

	public void setDefaultTimeout(long defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
	}

	public void setModulesUrlSeparator(String modulesUrlSeparator) {
		this.modulesUrlSeparator = modulesUrlSeparator;
	}

	public boolean isExcludeSvn() {
		return excludeSvn;
	}

	public void setExcludeSvn(boolean excludeSvn) {
		this.excludeSvn = excludeSvn;
	}

	/*
	 * INTERNAL CLASSES
	 */

	/** Intermediary structure used by path matching */
	protected class BundlesSet {
		private String baseUrl = "reference:file";// not used yet
		private final String dir;
		private List includes = new ArrayList();
		private List excludes = new ArrayList();

		public BundlesSet(String def) {
			StringTokenizer st = new StringTokenizer(def, ";");

			if (!st.hasMoreTokens())
				throw new RuntimeException("Base dir not defined.");
			try {
				String dirPath = st.nextToken();

				if (dirPath.startsWith("file:"))
					dirPath = dirPath.substring("file:".length());

				dir = new File(dirPath.replace('/', File.separatorChar))
						.getCanonicalPath();
				if (debug)
					debug("Base dir: " + dir);
			} catch (IOException e) {
				throw new RuntimeException("Cannot convert to absolute path", e);
			}

			while (st.hasMoreTokens()) {
				String tk = st.nextToken();
				StringTokenizer stEq = new StringTokenizer(tk, "=");
				String type = stEq.nextToken();
				String pattern = stEq.nextToken();
				if ("in".equals(type) || "include".equals(type)) {
					includes.add(pattern);
				} else if ("ex".equals(type) || "exclude".equals(type)) {
					excludes.add(pattern);
				} else if ("baseUrl".equals(type)) {
					baseUrl = pattern;
				} else {
					System.err.println("Unkown bundles pattern type " + type);
				}
			}

			if (excludeSvn && !excludes.contains(EXCLUDES_SVN_PATTERN)) {
				excludes.add(EXCLUDES_SVN_PATTERN);
			}
		}

		public String getDir() {
			return dir;
		}

		public List getIncludes() {
			return includes;
		}

		public List getExcludes() {
			return excludes;
		}

		public String getBaseUrl() {
			return baseUrl;
		}

	}

	/* @deprecated Doesn't seem to be used anymore. */
	// public void installOrUpdateUrls(Map urls) {
	// Map installedBundles = getBundles();
	//
	// for (Iterator modules = urls.keySet().iterator(); modules.hasNext();) {
	// String moduleName = (String) modules.next();
	// String urlStr = (String) urls.get(moduleName);
	// if (installedBundles.containsKey(moduleName)) {
	// Bundle bundle = (Bundle) installedBundles.get(moduleName);
	// InputStream in;
	// try {
	// URL url = new URL(urlStr);
	// in = url.openStream();
	// bundle.update(in);
	// OsgiBootUtils.info("Updated bundle " + moduleName
	// + " from " + urlStr);
	// } catch (Exception e) {
	// throw new RuntimeException("Cannot update " + moduleName
	// + " from " + urlStr);
	// }
	// if (in != null)
	// try {
	// in.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// } else {
	// try {
	// Bundle bundle = bundleContext.installBundle(urlStr);
	// if (debug)
	// debug("Installed bundle " + bundle.getSymbolicName()
	// + " from " + urlStr);
	// } catch (BundleException e) {
	// OsgiBootUtils.warn("Could not install bundle from "
	// + urlStr + ": " + e.getMessage());
	// }
	// }
	// }
	//
	// }

}
