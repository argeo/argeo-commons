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

import static org.argeo.osgi.boot.OsgiBootUtils.debug;
import static org.argeo.osgi.boot.OsgiBootUtils.warn;

import java.io.File;
import java.io.IOException;
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

import org.argeo.osgi.boot.internal.springutil.AntPathMatcher;
import org.argeo.osgi.boot.internal.springutil.PathMatcher;
import org.argeo.osgi.boot.internal.springutil.SystemPropertyUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;

/**
 * Basic provisioning of an OSGi runtime via file path patterns and system
 * properties. The approach is to generate list of URLs based on various
 * methods, configured via properties.
 */
public class OsgiBoot implements OsgiBootConstants {
	// public final static String PROP_ARGEO_OSGI_DATA_DIR =
	// "argeo.osgi.data.dir";
	public final static String PROP_ARGEO_OSGI_START = "argeo.osgi.start";
	public final static String PROP_ARGEO_OSGI_BUNDLES = "argeo.osgi.bundles";
	public final static String PROP_ARGEO_OSGI_BASE_URL = "argeo.osgi.baseUrl";
	public final static String PROP_ARGEO_OSGI_DISTRIBUTION_URL = "argeo.osgi.distributionUrl";

	// booleans
	public final static String PROP_ARGEO_OSGI_BOOT_DEBUG = "argeo.osgi.boot.debug";
	public final static String PROP_ARGEO_OSGI_BOOT_EXCLUDE_SVN = "argeo.osgi.boot.excludeSvn";

	// public final static String PROP_ARGEO_OSGI_BOOT_DEFAULT_TIMEOUT =
	// "argeo.osgi.boot.defaultTimeout";
	public final static String PROP_ARGEO_OSGI_BOOT_SYSTEM_PROPERTIES_FILE = "argeo.osgi.boot.systemPropertiesFile";
	public final static String PROP_ARGEO_OSGI_BOOT_APPCLASS = "argeo.osgi.boot.appclass";
	public final static String PROP_ARGEO_OSGI_BOOT_APPARGS = "argeo.osgi.boot.appargs";

	public final static String DEFAULT_BASE_URL = "reference:file:";
	public final static String EXCLUDES_SVN_PATTERN = "**/.svn/**";

	// OSGi system properties
	public final static String PROP_OSGI_BUNDLES_DEFAULTSTARTLEVEL = "osgi.bundles.defaultStartLevel";
	public final static String PROP_OSGI_STARTLEVEL = "osgi.startLevel";
	public final static String INSTANCE_AREA_PROP = "osgi.instance.area";
	// public final static String INSTANCE_AREA_DEFAULT_PROP =
	// "osgi.instance.area.default";

	// Symbolic names
	public final static String SYMBOLIC_NAME_OSGI_BOOT = "org.argeo.osgi.boot";
	public final static String SYMBOLIC_NAME_EQUINOX = "org.eclipse.osgi";

	private boolean debug = Boolean.valueOf(System.getProperty(PROP_ARGEO_OSGI_BOOT_DEBUG, "false")).booleanValue();
	/** Exclude svn metadata implicitely(a bit costly) */
	private boolean excludeSvn = Boolean.valueOf(System.getProperty(PROP_ARGEO_OSGI_BOOT_EXCLUDE_SVN, "false"))
			.booleanValue();

	/** Default is 10s */
	private long defaultTimeout = 10000l;

	private final BundleContext bundleContext;

	/*
	 * INITIALIZATION
	 */
	/** Constructor */
	public OsgiBoot(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		// defaultTimeout =
		// Long.parseLong(OsgiBootUtils.getProperty(PROP_ARGEO_OSGI_BOOT_DEFAULT_TIMEOUT,
		// "10000"));
		// initSystemProperties();
	}

	// /**
	// * Set additional system properties, especially ${argeo.osgi.data.dir} as
	// an
	// * OS file path (and not a file:// URL)
	// */
	// private void initSystemProperties() {
	// String osgiInstanceArea = System.getProperty(INSTANCE_AREA_PROP);
	// String osgiInstanceAreaDefault =
	// System.getProperty(INSTANCE_AREA_DEFAULT_PROP);
	// String tempDir = System.getProperty("java.io.tmpdir");
	//
	// File dataDir = null;
	// if (osgiInstanceArea != null) {
	// // within OSGi with -data specified
	// osgiInstanceArea = removeFilePrefix(osgiInstanceArea);
	// dataDir = new File(osgiInstanceArea);
	// } else if (osgiInstanceAreaDefault != null) {
	// // within OSGi without -data specified
	// osgiInstanceAreaDefault = removeFilePrefix(osgiInstanceAreaDefault);
	// dataDir = new File(osgiInstanceAreaDefault);
	// } else {// outside OSGi
	// dataDir = new File(tempDir + File.separator + "argeoOsgiData");
	// }
	// System.setProperty(PROP_ARGEO_OSGI_DATA_DIR, dataDir.getAbsolutePath());
	// }

	/*
	 * HIGH-LEVEL METHODS
	 */
	/** Bootstraps the OSGi runtime */
	public void bootstrap() {
		try {
			long begin = System.currentTimeMillis();
			System.out.println();
			String osgiInstancePath = bundleContext.getProperty(INSTANCE_AREA_PROP);
			OsgiBootUtils
					.info("OSGi bootstrap starting" + (osgiInstancePath != null ? " (" + osgiInstancePath + ")" : ""));
			// OsgiBootUtils.info("Writable data directory : " +
			// System.getProperty(PROP_ARGEO_OSGI_DATA_DIR)
			// + " (set as system property " + PROP_ARGEO_OSGI_DATA_DIR + ")");
			installUrls(getBundlesUrls());
			installUrls(getDistributionUrls());
			startBundles();
			long duration = System.currentTimeMillis() - begin;
			OsgiBootUtils.info("OSGi bootstrap completed in " + Math.round(((double) duration) / 1000) + "s ("
					+ duration + "ms), " + bundleContext.getBundles().length + " bundles");
		} catch (RuntimeException e) {
			OsgiBootUtils.error("OSGi bootstrap FAILED", e);
			throw e;
		}

		// diagnostics
		if (debug) {
			OsgiBootDiagnostics diagnostics = new OsgiBootDiagnostics(bundleContext);
			diagnostics.checkUnresolved();
			Map<String, Set<String>> duplicatePackages = diagnostics.findPackagesExportedTwice();
			if (duplicatePackages.size() > 0) {
				OsgiBootUtils.info("Packages exported twice:");
				Iterator<String> it = duplicatePackages.keySet().iterator();
				while (it.hasNext()) {
					String pkgName = it.next();
					OsgiBootUtils.info(pkgName);
					Set<String> bdles = duplicatePackages.get(pkgName);
					Iterator<String> bdlesIt = bdles.iterator();
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
		List<String> urls = new ArrayList<String>();
		urls.add(url);
		installUrls(urls);
		return (Bundle) getBundlesByLocation().get(url);
	}

	/** Install the bundles at this URL list. */
	public void installUrls(List<String> urls) {
		Map<String, Bundle> installedBundles = getBundlesByLocation();
		for (int i = 0; i < urls.size(); i++) {
			String url = (String) urls.get(i);
			installUrl(url, installedBundles);
		}
	}

	/** Actually install the provided URL */
	protected void installUrl(String url, Map<String, Bundle> installedBundles) {
		try {
			if (installedBundles.containsKey(url)) {
				Bundle bundle = (Bundle) installedBundles.get(url);
				if (debug)
					debug("Bundle " + bundle.getSymbolicName() + " already installed from " + url);
			} else if (url.contains("/" + SYMBOLIC_NAME_EQUINOX + "/")
					|| url.contains("/" + SYMBOLIC_NAME_OSGI_BOOT + "/")) {
				if (debug)
					warn("Skip " + url);
				return;
			} else {
				Bundle bundle = bundleContext.installBundle(url);
				if (url.startsWith("http"))
					OsgiBootUtils
							.info("Installed " + bundle.getSymbolicName() + "-" + bundle.getVersion() + " from " + url);
				else if (debug)
					OsgiBootUtils.debug(
							"Installed " + bundle.getSymbolicName() + "-" + bundle.getVersion() + " from " + url);
			}
		} catch (BundleException e) {
			String message = e.getMessage();
			if ((message.contains("Bundle \"" + SYMBOLIC_NAME_OSGI_BOOT + "\"")
					|| message.contains("Bundle \"" + SYMBOLIC_NAME_EQUINOX + "\""))
					&& message.contains("is already installed")) {
				// silent, in order to avoid warnings: we know that both
				// have already been installed...
			} else {
				OsgiBootUtils.warn("Could not install bundle from " + url + ": " + message);
			}
			if (debug)
				e.printStackTrace();
		}
	}

	/*
	 * START
	 */
	public void startBundles() {
		startBundles(System.getProperties());
	}

	public void startBundles(Properties properties) {
		FrameworkStartLevel frameworkStartLevel = bundleContext.getBundle(0).adapt(FrameworkStartLevel.class);

		// default and active start levels from System properties
		Integer defaultStartLevel = new Integer(
				Integer.parseInt(getProperty(PROP_OSGI_BUNDLES_DEFAULTSTARTLEVEL, "4")));
		Integer activeStartLevel = new Integer(getProperty(PROP_OSGI_STARTLEVEL, "6"));

		SortedMap<Integer, List<String>> startLevels = new TreeMap<Integer, List<String>>();
		computeStartLevels(startLevels, properties, defaultStartLevel);
		// inverts the map for the time being, TODO optimise
		Map<String, Integer> bundleStartLevels = new HashMap<>();
		for (Integer level : startLevels.keySet()) {
			for (String bsn : startLevels.get(level))
				bundleStartLevels.put(bsn, level);
		}
		for (Bundle bundle : bundleContext.getBundles()) {
			String bsn = bundle.getSymbolicName();
			if (bundleStartLevels.containsKey(bsn)) {
				BundleStartLevel bundleStartLevel = bundle.adapt(BundleStartLevel.class);
				Integer level = bundleStartLevels.get(bsn);
				if (bundleStartLevel.getStartLevel() != level || !bundleStartLevel.isPersistentlyStarted()) {
					bundleStartLevel.setStartLevel(level);
					try {
						bundle.start();
					} catch (BundleException e) {
						OsgiBootUtils.error("Cannot mark " + bsn + " as started", e);
					}
					if (getDebug())
						OsgiBootUtils.debug(bsn + " starts at level " + level);
				}
			}
		}
		frameworkStartLevel.setStartLevel(activeStartLevel, (FrameworkEvent event) -> {
			if (getDebug())
				OsgiBootUtils.debug("Framework event: " + event);
			int initialStartLevel = frameworkStartLevel.getInitialBundleStartLevel();
			int startLevel = frameworkStartLevel.getStartLevel();
			OsgiBootUtils.debug("Framework start level: " + startLevel + " (initial: " + initialStartLevel + ")");
		});

		// Iterator<Integer> levels = startLevels.keySet().iterator();
		// while (levels.hasNext()) {
		// Integer level = (Integer) levels.next();
		// boolean allStarted = startBundles(startLevels.get(level));
		// if (!allStarted)
		// OsgiBootUtils.warn("Not all bundles started for level " + level);
		// if (level.equals(activeStartLevel))
		// break;// active start level reached
		// }

	}

	public static void computeStartLevels(SortedMap<Integer, List<String>> startLevels, Properties properties,
			Integer defaultStartLevel) {

		// default (and previously, only behaviour)
		appendToStartLevels(startLevels, defaultStartLevel, properties.getProperty(PROP_ARGEO_OSGI_START, ""));

		// list argeo.osgi.start.* system properties
		Iterator<Object> keys = properties.keySet().iterator();
		final String prefix = PROP_ARGEO_OSGI_START + ".";
		while (keys.hasNext()) {
			String key = keys.next().toString();
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
	private static void appendToStartLevels(SortedMap<Integer, List<String>> startLevels, Integer startLevel,
			String str) {
		if (str == null || str.trim().equals(""))
			return;

		if (!startLevels.containsKey(startLevel))
			startLevels.put(startLevel, new ArrayList<String>());
		String[] bundleNames = str.split(",");
		for (int i = 0; i < bundleNames.length; i++) {
			if (bundleNames[i] != null && !bundleNames[i].trim().equals(""))
				(startLevels.get(startLevel)).add(bundleNames[i]);
		}
	}

	/**
	 * Start the provided list of bundles
	 *
	 * @return whether all bundles are now in active state
	 * @deprecated
	 */
	@Deprecated
	public boolean startBundles(List<String> bundlesToStart) {
		if (bundlesToStart.size() == 0)
			return true;

		// used to monitor ACTIVE states
		List<Bundle> startedBundles = new ArrayList<Bundle>();
		// used to log the bundles not found
		List<String> notFoundBundles = new ArrayList<String>(bundlesToStart);

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
						OsgiBootUtils.warn("Start of bundle " + symbolicName + " failed because of " + e
								+ ", maybe bundle is not yet resolved," + " waiting and trying again.");
						waitForBundleResolvedOrActive(startBegin, bundle);
						bundle.start();
						startedBundles.add(bundle);
					}
					notFoundBundles.remove(symbolicName);
				} catch (Exception e) {
					OsgiBootUtils.warn("Bundle " + symbolicName + " cannot be started: " + e.getMessage());
					if (debug)
						e.printStackTrace();
					// was found even if start failed
					notFoundBundles.remove(symbolicName);
				}
		}

		for (int i = 0; i < notFoundBundles.size(); i++)
			OsgiBootUtils.warn("Bundle '" + notFoundBundles.get(i) + "' not started	 because it was not found.");

		// monitors that all bundles are started
		long beginMonitor = System.currentTimeMillis();
		boolean allStarted = !(startedBundles.size() > 0);
		List<String> notStarted = new ArrayList<String>();
		while (!allStarted && (System.currentTimeMillis() - beginMonitor) < defaultTimeout) {
			notStarted = new ArrayList<String>();
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
				OsgiBootUtils.warn("Bundle '" + notStarted.get(i) + "' not ACTIVE after " + (duration / 1000) + "s");

		return allStarted;
	}

	/** Waits for a bundle to become active or resolved */
	private void waitForBundleResolvedOrActive(long startBegin, Bundle bundle) throws Exception {
		int originalState = bundle.getState();
		if ((originalState == Bundle.RESOLVED) || (originalState == Bundle.ACTIVE))
			return;

		String originalStateStr = OsgiBootUtils.stateAsString(originalState);

		int currentState = bundle.getState();
		while (!(currentState == Bundle.RESOLVED || currentState == Bundle.ACTIVE)) {
			long now = System.currentTimeMillis();
			if ((now - startBegin) > defaultTimeout * 10)
				throw new Exception("Bundle " + bundle.getSymbolicName() + " was not RESOLVED or ACTIVE after "
						+ (now - startBegin) + "ms (originalState=" + originalStateStr + ", currentState="
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
	 * BUNDLE PATTERNS INSTALLATION
	 */
	/**
	 * Computes a list of URLs based on Ant-like include/exclude patterns
	 * defined by ${argeo.osgi.bundles} with the following format:<br>
	 * <code>/base/directory;in=*.jar;in=**;ex=org.eclipse.osgi_*;jar</code><br>
	 * WARNING: <code>/base/directory;in=*.jar,\</code> at the end of a file,
	 * without a new line causes a '.' to be appended with unexpected side
	 * effects.
	 */
	public List<String> getBundlesUrls() {
		String bundlePatterns = getProperty(PROP_ARGEO_OSGI_BUNDLES);
		return getBundlesUrls(bundlePatterns);
	}

	/**
	 * Compute alist of URLs to install based on the provided patterns, with
	 * default base url
	 */
	public List<String> getBundlesUrls(String bundlePatterns) {
		String baseUrl = getProperty(PROP_ARGEO_OSGI_BASE_URL, DEFAULT_BASE_URL);
		return getBundlesUrls(baseUrl, bundlePatterns);
	}

	/** Implements the path matching logic */
	List<String> getBundlesUrls(String baseUrl, String bundlePatterns) {
		List<String> urls = new ArrayList<String>();
		if (bundlePatterns == null)
			return urls;

		bundlePatterns = SystemPropertyUtils.resolvePlaceholders(bundlePatterns);
		if (debug)
			debug(PROP_ARGEO_OSGI_BUNDLES + "=" + bundlePatterns + " (excludeSvn=" + excludeSvn + ")");

		StringTokenizer st = new StringTokenizer(bundlePatterns, ",");
		List<BundlesSet> bundlesSets = new ArrayList<BundlesSet>();
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (new File(token).exists()) {
				String url = locationToUrl(baseUrl, token);
				urls.add(url);
			} else
				bundlesSets.add(new BundlesSet(token));
		}

		// find included
		List<String> included = new ArrayList<String>();
		PathMatcher matcher = new AntPathMatcher();
		for (int i = 0; i < bundlesSets.size(); i++) {
			BundlesSet bundlesSet = (BundlesSet) bundlesSets.get(i);
			for (int j = 0; j < bundlesSet.getIncludes().size(); j++) {
				String pattern = (String) bundlesSet.getIncludes().get(j);
				match(matcher, included, bundlesSet.getDir(), null, pattern);
			}
		}

		// find excluded
		List<String> excluded = new ArrayList<String>();
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
	public List<String> getDistributionUrls() {
		String distributionUrl = getProperty(PROP_ARGEO_OSGI_DISTRIBUTION_URL);
		String baseUrl = getProperty(PROP_ARGEO_OSGI_BASE_URL);
		return getDistributionUrls(distributionUrl, baseUrl);
	}

	public List<String> getDistributionUrls(String distributionUrl, String baseUrl) {
		List<String> urls = new ArrayList<String>();
		if (distributionUrl == null)
			return urls;

		DistributionBundle distributionBundle;
		if (baseUrl != null && !(distributionUrl.startsWith("http") || distributionUrl.startsWith("file"))) {
			// relative url
			distributionBundle = new DistributionBundle(baseUrl, distributionUrl);
		} else {
			distributionBundle = new DistributionBundle(distributionUrl);
			if (baseUrl != null)
				distributionBundle.setBaseUrl(baseUrl);

		}
		distributionBundle.processUrl();
		return distributionBundle.listUrls();
	}

	/*
	 * HIGH LEVEL UTILITIES
	 */
	/** Actually performs the matching logic. */
	protected void match(PathMatcher matcher, List<String> matched, String base, String currentPath, String pattern) {
		if (currentPath == null) {
			// Init
			File baseDir = new File(base.replace('/', File.separatorChar));
			File[] files = baseDir.listFiles();

			if (files == null) {
				if (debug)
					OsgiBootUtils.warn("Base dir " + baseDir + " has no children, exists=" + baseDir.exists()
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
						String newCurrentPath = currentPath + '/' + files[i].getName();
						if (files[i].isDirectory()) {
							if (matcher.matchStart(pattern, newCurrentPath)) {
								// recurse only if start matches
								match(matcher, matched, base, newCurrentPath, pattern);
							} else {
								if (debug)
									debug(newCurrentPath + " does not start match with " + pattern);

							}
						} else {
							boolean nonDirectoryOk = matcher.match(pattern, newCurrentPath);
							if (debug)
								debug(currentPath + " " + (ok ? "" : " not ") + " matched with " + pattern);
							if (nonDirectoryOk)
								matched.add(relativeToFullPath(base, newCurrentPath));
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
	public Map<String, Bundle> getBundlesByLocation() {
		Map<String, Bundle> installedBundles = new HashMap<String, Bundle>();
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
	public Map<String, Bundle> getBundlesBySymbolicName() {
		Map<String, Bundle> namedBundles = new HashMap<String, Bundle>();
		Bundle[] bundles = bundleContext.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			namedBundles.put(bundles[i].getSymbolicName(), bundles[i]);
		}
		return namedBundles;
	}

	/** Creates an URL from a location */
	protected String locationToUrl(String baseUrl, String location) {
		return baseUrl + location;
	}

	/** Transforms a relative path in a full system path. */
	protected String relativeToFullPath(String basePath, String relativePath) {
		return (basePath + '/' + relativePath).replace('/', File.separatorChar);
	}

	// private String removeFilePrefix(String url) {
	// if (url.startsWith("file:"))
	// return url.substring("file:".length());
	// else if (url.startsWith("reference:file:"))
	// return url.substring("reference:file:".length());
	// else
	// return url;
	// }

	/**
	 * Gets a property value
	 * 
	 * @return null when defaultValue is ""
	 */
	public String getProperty(String name, String defaultValue) {
		String value = bundleContext.getProperty(name);
		if (value == null) 
			return defaultValue; // may be null
		else
			return value;

//		if (defaultValue != null)
//			value = System.getProperty(name, defaultValue);
//		else
//			value = System.getProperty(name);
//
//		if (value == null || value.equals(""))
//			return null;
//		else
//			return value;
	}

	public String getProperty(String name) {
		return getProperty(name, null);
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

	// public void setDefaultTimeout(long defaultTimeout) {
	// this.defaultTimeout = defaultTimeout;
	// }

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
		private List<String> includes = new ArrayList<String>();
		private List<String> excludes = new ArrayList<String>();

		public BundlesSet(String def) {
			StringTokenizer st = new StringTokenizer(def, ";");

			if (!st.hasMoreTokens())
				throw new RuntimeException("Base dir not defined.");
			try {
				String dirPath = st.nextToken();

				if (dirPath.startsWith("file:"))
					dirPath = dirPath.substring("file:".length());

				dir = new File(dirPath.replace('/', File.separatorChar)).getCanonicalPath();
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

		public List<String> getIncludes() {
			return includes;
		}

		public List<String> getExcludes() {
			return excludes;
		}

		public String getBaseUrl() {
			return baseUrl;
		}

	}
}
