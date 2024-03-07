package org.argeo.init.osgi;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.TRACE;
import static java.lang.System.Logger.Level.WARNING;

import java.io.File;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.argeo.api.a2.A2Source;
import org.argeo.api.a2.ProvisioningManager;
import org.argeo.api.init.InitConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.Version;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.FrameworkWiring;

/**
 * Basic provisioning of an OSGi runtime via file path patterns and system
 * properties. The approach is to generate list of URLs based on various
 * methods, configured via properties.
 */
public class OsgiBoot {
	private final static Logger logger = System.getLogger(OsgiBoot.class.getName());

	@Deprecated
	final static String PROP_ARGEO_OSGI_BUNDLES = "argeo.osgi.bundles";
	final static String PROP_ARGEO_OSGI_BASE_URL = "argeo.osgi.baseUrl";
	final static String PROP_ARGEO_OSGI_LOCAL_CACHE = "argeo.osgi.localCache";
	final static String PROP_ARGEO_OSGI_DISTRIBUTION_URL = "argeo.osgi.distributionUrl";

	// booleans
	@Deprecated
	final static String PROP_ARGEO_OSGI_BOOT_DEBUG = "argeo.osgi.boot.debug";

	final static String PROP_ARGEO_OSGI_BOOT_SYSTEM_PROPERTIES_FILE = "argeo.osgi.boot.systemPropertiesFile";
	final static String PROP_ARGEO_OSGI_BOOT_APPCLASS = "argeo.osgi.boot.appclass";
	final static String PROP_ARGEO_OSGI_BOOT_APPARGS = "argeo.osgi.boot.appargs";

	@Deprecated
	public final static String DEFAULT_BASE_URL = "reference:file:";
	final static String DEFAULT_MAX_START_LEVEL = "32";

	private final BundleContext bundleContext;
	private final String localCache;
	private final ProvisioningManager provisioningManager;

	/*
	 * INITIALISATION
	 */
	/** Constructor */
	public OsgiBoot(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		Path homePath = Paths.get(System.getProperty("user.home")).toAbsolutePath();
		String homeUri = homePath.toUri().toString();
		localCache = getProperty(PROP_ARGEO_OSGI_LOCAL_CACHE, homeUri + ".m2/repository/");

		provisioningManager = new ProvisioningManager(bundleContext);
		String sources = getProperty(InitConstants.PROP_ARGEO_OSGI_SOURCES);
		if (sources == null) {
			provisioningManager.registerDefaultSource();
		} else {
//			OsgiBootUtils.debug("Found sources " + sources);
			for (String source : sources.split(",")) {
				int qmIndex = source.lastIndexOf('?');
				String queryPart = "";
				if (qmIndex >= 0) {
					queryPart = source.substring(qmIndex);
					source = source.substring(0, qmIndex);
				}
				// TODO centralise in A" package?
				if (source.trim().equals(A2Source.DEFAULT_A2_URI)) {
					if (Files.exists(homePath))
						provisioningManager.registerSource(
								A2Source.SCHEME_A2 + "://" + homePath.toString() + "/.local/share/a2" + queryPart);
					provisioningManager.registerSource(A2Source.SCHEME_A2 + ":///usr/local/share/a2" + queryPart);
//					provisioningManager.registerSource(A2Source.SCHEME_A2 + ":///usr/local/lib/a2" + queryPart);
					provisioningManager.registerSource(A2Source.SCHEME_A2 + ":///usr/share/a2" + queryPart);
//					provisioningManager.registerSource(A2Source.SCHEME_A2 + ":///usr/lib/a2" + queryPart);
				} else if (source.trim().equals(A2Source.DEFAULT_A2_REFERENCE_URI)) {
					if (Files.exists(homePath))
						provisioningManager.registerSource(A2Source.SCHEME_A2_REFERENCE + "://" + homePath.toString()
								+ "/.local/share/a2" + queryPart);
					provisioningManager
							.registerSource(A2Source.SCHEME_A2_REFERENCE + ":///usr/local/share/a2" + queryPart);
//					provisioningManager
//							.registerSource(A2Source.SCHEME_A2_REFERENCE + ":///usr/local/lib/a2" + queryPart);
					provisioningManager.registerSource(A2Source.SCHEME_A2_REFERENCE + ":///usr/share/a2" + queryPart);
//					provisioningManager.registerSource(A2Source.SCHEME_A2_REFERENCE + ":///usr/lib/a2" + queryPart);
				} else {
					provisioningManager.registerSource(source + queryPart);
				}
			}
		}
	}

	ProvisioningManager getProvisioningManager() {
		return provisioningManager;
	}

	/*
	 * HIGH-LEVEL METHODS
	 */
	/**
	 * Bootstraps the OSGi runtime using these properties, which MUST be consistent
	 * with {@link BundleContext#getProperty(String)}. If these properties are
	 * <code>null</code>, system properties are used instead.
	 */
	public void bootstrap(Map<String, String> properties) {
		try {
			long begin = System.currentTimeMillis();

			// notify start
			String osgiInstancePath = getProperty(InitConstants.PROP_OSGI_INSTANCE_AREA);
			String osgiConfigurationPath = getProperty(InitConstants.PROP_OSGI_CONFIGURATION_AREA);
			String osgiSharedConfigurationPath = getProperty(InitConstants.PROP_OSGI_CONFIGURATION_AREA);
			logger.log(DEBUG, () -> "OSGi bootstrap starting" //
					+ (osgiInstancePath != null ? " data: " + osgiInstancePath + "" : "") //
					+ (osgiConfigurationPath != null ? " state: " + osgiConfigurationPath + "" : "") //
					+ (osgiSharedConfigurationPath != null ? " config: " + osgiSharedConfigurationPath + "" : "") //
			);

			// legacy install bundles
			installUrls(getBundlesUrls());
			installUrls(getDistributionUrls());

			// A2 install bundles
			provisioningManager.install(null);

			// Make sure fragments are properly considered by refreshing
			refreshFramework();

			// start bundles
//			if (properties != null && !Boolean.parseBoolean(properties.get(PROP_OSGI_USE_SYSTEM_PROPERTIES)))
			startBundles(properties);
//			else
//				startBundles();

			// complete
			long duration = System.currentTimeMillis() - begin;
			logger.log(DEBUG, () -> "OSGi bootstrap completed in " + Math.round(((double) duration) / 1000) + "s ("
					+ duration + "ms), " + bundleContext.getBundles().length + " bundles");
		} catch (RuntimeException e) {
			logger.log(ERROR, "OSGi bootstrap FAILED", e);
			throw e;
		}

		// diagnostics
		if (logger.isLoggable(TRACE)) {
			OsgiBootDiagnostics diagnostics = new OsgiBootDiagnostics(bundleContext);
			diagnostics.checkUnresolved();
			Map<String, Set<String>> duplicatePackages = diagnostics.findPackagesExportedTwice();
			if (duplicatePackages.size() > 0) {
				logger.log(TRACE, "Packages exported twice:");
				Iterator<String> it = duplicatePackages.keySet().iterator();
				while (it.hasNext()) {
					String pkgName = it.next();
					logger.log(TRACE, pkgName);
					Set<String> bdles = duplicatePackages.get(pkgName);
					Iterator<String> bdlesIt = bdles.iterator();
					while (bdlesIt.hasNext())
						logger.log(TRACE, "  " + bdlesIt.next());
				}
			}
		}
		System.out.println();
	}

	/**
	 * Calls {@link #bootstrap(Map)} with <code>null</code>.
	 * 
	 * @see #bootstrap(Map)
	 */
	@Deprecated
	public void bootstrap() {
		bootstrap(null);
	}

	public void update() {
		provisioningManager.update();
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
//		refreshFramework();
	}

	/** Actually install the provided URL */
	protected void installUrl(String url, Map<String, Bundle> installedBundles) {
		try {
			if (installedBundles.containsKey(url)) {
				Bundle bundle = (Bundle) installedBundles.get(url);
				logger.log(TRACE, () -> "Bundle " + bundle.getSymbolicName() + " already installed from " + url);
			} else if (url.contains("/" + InitConstants.SYMBOLIC_NAME_EQUINOX + "/")
					|| url.contains("/" + InitConstants.SYMBOLIC_NAME_INIT + "/")) {
				if (logger.isLoggable(TRACE))
					logger.log(WARNING, "Skip " + url);
				return;
			} else {
				Bundle bundle = bundleContext.installBundle(url);
				if (url.startsWith("http"))
					logger.log(DEBUG,
							() -> "Installed " + bundle.getSymbolicName() + "-" + bundle.getVersion() + " from " + url);
				else
					logger.log(TRACE,
							() -> "Installed " + bundle.getSymbolicName() + "-" + bundle.getVersion() + " from " + url);
				assert bundle.getSymbolicName() != null;
				// uninstall previous versions
				bundles: for (Bundle b : bundleContext.getBundles()) {
					if (b.getSymbolicName() == null)
						continue bundles;
					if (bundle.getSymbolicName().equals(b.getSymbolicName())) {
						Version bundleV = bundle.getVersion();
						Version bV = b.getVersion();
						if (bV == null)
							continue bundles;
						if (bundleV.getMajor() == bV.getMajor() && bundleV.getMinor() == bV.getMinor()) {
							if (bundleV.getMicro() > bV.getMicro()) {
								// uninstall older bundles
								b.uninstall();
								logger.log(TRACE, () -> "Uninstalled " + b);
							} else if (bundleV.getMicro() < bV.getMicro()) {
								// uninstall just installed bundle if newer
								bundle.uninstall();
								logger.log(TRACE, () -> "Uninstalled " + bundle);
								break bundles;
							} else {
								// uninstall any other with same major/minor
								if (!bundleV.getQualifier().equals(bV.getQualifier())) {
									b.uninstall();
									logger.log(TRACE, () -> "Uninstalled " + b);
								}
							}
						}
					}
				}
			}
		} catch (BundleException e) {
			final String ALREADY_INSTALLED = "is already installed";
			String message = e.getMessage();
			if ((message.contains("Bundle \"" + InitConstants.SYMBOLIC_NAME_INIT + "\"")
					|| message.contains("Bundle \"" + InitConstants.SYMBOLIC_NAME_EQUINOX + "\""))
					&& message.contains(ALREADY_INSTALLED)) {
				// silent, in order to avoid warnings: we know that both
				// have already been installed...
			} else {
				if (message.contains(ALREADY_INSTALLED)) {
					if (logger.isLoggable(TRACE))
						logger.log(WARNING, "Duplicate install from " + url + ": " + message);
				} else
					logger.log(WARNING, "Could not install bundle from " + url + ": " + message);
			}
			if (logger.isLoggable(TRACE) && !message.contains(ALREADY_INSTALLED))
				e.printStackTrace();
		}
	}

	/*
	 * START
	 */

	/**
	 * Start bundles based on these properties.
	 * 
	 * @see OsgiBoot#doStartBundles(Map)
	 */
	public void startBundles(Map<String, String> properties) {
		Map<String, String> map = new TreeMap<>();
		// first use properties
		if (properties != null) {
			for (String key : properties.keySet()) {
				String property = key;
				if (property.startsWith(InitConstants.PROP_ARGEO_OSGI_START)) {
					map.put(property, properties.get(property));
				}
			}
		}
		// then try all start level until a maximum
		int maxStartLevel = Integer
				.parseInt(getProperty(InitConstants.PROP_ARGEO_OSGI_MAX_START_LEVEL, DEFAULT_MAX_START_LEVEL));
		for (int i = 1; i <= maxStartLevel; i++) {
			String key = InitConstants.PROP_ARGEO_OSGI_START + "." + i;
			String value = getProperty(key);
			if (value != null)
				map.put(key, value);

		}
		// finally, override with system properties
		for (Object key : System.getProperties().keySet()) {
			if (key.toString().startsWith(InitConstants.PROP_ARGEO_OSGI_START)) {
				map.put(key.toString(), System.getProperty(key.toString()));
			}
		}
		// start
		doStartBundles(map);
	}

	void startBundles(Properties properties) {
		Map<String, String> map = new TreeMap<>();
		// first use properties
		if (properties != null) {
			for (Object key : properties.keySet()) {
				String property = key.toString();
				if (property.startsWith(InitConstants.PROP_ARGEO_OSGI_START)) {
					map.put(property, properties.get(property).toString());
				}
			}
		}
		startBundles(map);
	}

	/**
	 * Start bundle based on keys starting with
	 * {@link InitConstants#PROP_ARGEO_OSGI_START}.
	 */
	protected void doStartBundles(Map<String, String> properties) {
		FrameworkStartLevel frameworkStartLevel = bundleContext.getBundle(0).adapt(FrameworkStartLevel.class);

		// default and active start levels from System properties
		int initialStartLevel = frameworkStartLevel.getInitialBundleStartLevel();
		int defaultStartLevel = Integer.parseInt(getProperty(InitConstants.PROP_OSGI_BUNDLES_DEFAULTSTARTLEVEL, "4"));
		int activeStartLevel = Integer.parseInt(getProperty(InitConstants.PROP_OSGI_STARTLEVEL, "6"));
		if (logger.isLoggable(TRACE)) {
			logger.log(TRACE,
					"OSGi default start level: "
							+ getProperty(InitConstants.PROP_OSGI_BUNDLES_DEFAULTSTARTLEVEL, "<not set>") + ", using "
							+ defaultStartLevel);
			logger.log(TRACE, "OSGi active start level: " + getProperty(InitConstants.PROP_OSGI_STARTLEVEL, "<not set>")
					+ ", using " + activeStartLevel);
			logger.log(TRACE, "Framework start level: " + frameworkStartLevel.getStartLevel() + " (initial: "
					+ initialStartLevel + ")");
		}

		SortedMap<Integer, List<String>> startLevels = new TreeMap<Integer, List<String>>();
		computeStartLevels(startLevels, properties, defaultStartLevel);
		// inverts the map for the time being, TODO optimise
		Map<String, Integer> bundleStartLevels = new HashMap<>();
		for (Integer level : startLevels.keySet()) {
			for (String bsn : startLevels.get(level))
				bundleStartLevels.put(bsn, level);
		}

		// keep only bundles with the highest version
		Map<String, Bundle> startableBundles = new HashMap<>();
		bundles: for (Bundle bundle : bundleContext.getBundles()) {
			if (bundle.getVersion() == null)
				continue bundles;
			String bsn = bundle.getSymbolicName();
			if (!startableBundles.containsKey(bsn)) {
				startableBundles.put(bsn, bundle);
			} else {
				if (bundle.getVersion().compareTo(startableBundles.get(bsn).getVersion()) > 0) {
					startableBundles.put(bsn, bundle);
				}
			}
		}

		for (Bundle bundle : startableBundles.values()) {
			String bsn = bundle.getSymbolicName();
			if (bundleStartLevels.containsKey(bsn)) {
				BundleStartLevel bundleStartLevel = bundle.adapt(BundleStartLevel.class);
				Integer level = bundleStartLevels.get(bsn);
				if (bundleStartLevel.getStartLevel() != level || !bundleStartLevel.isPersistentlyStarted()) {
					bundleStartLevel.setStartLevel(level);
					try {
						bundle.start();
					} catch (BundleException e) {
						logger.log(ERROR, "Cannot mark " + bsn + " as started", e);
					}
					logger.log(TRACE, () -> bsn + " v" + bundle.getVersion() + " starts at level " + level);
				}
			}
		}

		logger.log(TRACE, () -> "About to set framework start level to " + activeStartLevel + " ...");

		frameworkStartLevel.setStartLevel(activeStartLevel, (FrameworkEvent event) -> {
			if (event.getType() == FrameworkEvent.ERROR) {
				logger.log(ERROR, "Start sequence failed", event.getThrowable());
			} else {
				logger.log(TRACE, () -> "Framework started at level " + frameworkStartLevel.getStartLevel());
			}
		});

//		// Start the framework level after level
//		int currentStartLevel = frameworkStartLevel.getStartLevel();
//		stages: for (int stage = currentStartLevel + 1; stage <= activeStartLevel; stage++) {
//			if (OsgiBootUtils.isDebug())
//				OsgiBootUtils.debug("Starting stage " + stage + "...");
//			final int nextStage = stage;
//			final CompletableFuture<FrameworkEvent> stageCompleted = new CompletableFuture<>();
//			frameworkStartLevel.setStartLevel(nextStage, (FrameworkEvent event) -> {
//				stageCompleted.complete(event);
//			});
//			FrameworkEvent event;
//			try {
//				event = stageCompleted.get();
//			} catch (InterruptedException | ExecutionException e) {
//				throw new IllegalStateException("Cannot continue start", e);
//			}
//			if (event.getThrowable() != null) {
//				OsgiBootUtils.error("Stage " + nextStage + " failed, aborting start.", event.getThrowable());
//				break stages;
//			}
//		}
	}

	private static void computeStartLevels(SortedMap<Integer, List<String>> startLevels, Map<String, String> properties,
			Integer defaultStartLevel) {

		// default (and previously, only behaviour)
		appendToStartLevels(startLevels, defaultStartLevel,
				properties.getOrDefault(InitConstants.PROP_ARGEO_OSGI_START, ""));

		// list argeo.osgi.start.* system properties
		Iterator<String> keys = properties.keySet().iterator();
		final String prefix = InitConstants.PROP_ARGEO_OSGI_START + ".";
		while (keys.hasNext()) {
			String key = keys.next();
			if (key.startsWith(prefix)) {
				Integer startLevel;
				String suffix = key.substring(prefix.length());
				String[] tokens = suffix.split("\\.");
				if (tokens.length > 0 && !tokens[0].trim().equals(""))
					try {
						// first token is start level
						startLevel = Integer.parseInt(tokens[0]);
					} catch (NumberFormatException e) {
						startLevel = defaultStartLevel;
					}
				else
					startLevel = defaultStartLevel;

				// append bundle names
				String bundleNames = properties.get(key);
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

	/*
	 * BUNDLE PATTERNS INSTALLATION
	 */
	/**
	 * Computes a list of URLs based on Ant-like include/exclude patterns defined by
	 * ${argeo.osgi.bundles} with the following format:<br>
	 * <code>/base/directory;in=*.jar;in=**;ex=org.eclipse.osgi_*;jar</code><br>
	 * WARNING: <code>/base/directory;in=*.jar,\</code> at the end of a file,
	 * without a new line causes a '.' to be appended with unexpected side effects.
	 */
	public List<String> getBundlesUrls() {
		String bundlePatterns = getProperty(PROP_ARGEO_OSGI_BUNDLES);
		return getBundlesUrls(bundlePatterns);
	}

	/**
	 * Compute a list of URLs to install based on the provided patterns, with
	 * default base url
	 */
	public List<String> getBundlesUrls(String bundlePatterns) {
		String baseUrl = getProperty(PROP_ARGEO_OSGI_BASE_URL, DEFAULT_BASE_URL);
		return getBundlesUrls(baseUrl, bundlePatterns);
	}

	/** Implements the path matching logic */
	@Deprecated
	public List<String> getBundlesUrls(String baseUrl, String bundlePatterns) {
		List<String> urls = new ArrayList<String>();
		if (bundlePatterns == null)
			return urls;

//		bundlePatterns = SystemPropertyUtils.resolvePlaceholders(bundlePatterns);
		logger.log(TRACE, () -> PROP_ARGEO_OSGI_BUNDLES + "=" + bundlePatterns);

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
//		PathMatcher matcher = new AntPathMatcher();
		for (int i = 0; i < bundlesSets.size(); i++) {
			BundlesSet bundlesSet = (BundlesSet) bundlesSets.get(i);
			for (int j = 0; j < bundlesSet.getIncludes().size(); j++) {
				String pattern = (String) bundlesSet.getIncludes().get(j);
				match(included, bundlesSet.getDir(), null, pattern);
			}
		}

		// find excluded
		List<String> excluded = new ArrayList<String>();
		for (int i = 0; i < bundlesSets.size(); i++) {
			BundlesSet bundlesSet = (BundlesSet) bundlesSets.get(i);
			for (int j = 0; j < bundlesSet.getExcludes().size(); j++) {
				String pattern = (String) bundlesSet.getExcludes().get(j);
				match(excluded, bundlesSet.getDir(), null, pattern);
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
		if (distributionUrl.startsWith("http") || distributionUrl.startsWith("file")) {
			distributionBundle = new DistributionBundle(distributionUrl);
			if (baseUrl != null)
				distributionBundle.setBaseUrl(baseUrl);
		} else {
			// relative url
			if (baseUrl == null) {
				baseUrl = localCache;
			}

			if (distributionUrl.contains(":")) {
				// TODO make it safer
				String[] parts = distributionUrl.trim().split(":");
				String[] categoryParts = parts[0].split("\\.");
				String artifactId = parts[1];
				String version = parts[2];
				StringBuilder sb = new StringBuilder();
				for (String categoryPart : categoryParts) {
					sb.append(categoryPart).append('/');
				}
				sb.append(artifactId).append('/');
				sb.append(version).append('/');
				sb.append(artifactId).append('-').append(version).append(".jar");
				distributionUrl = sb.toString();
			}

			distributionBundle = new DistributionBundle(baseUrl, distributionUrl, localCache);
		}
		distributionBundle.processUrl();
		return distributionBundle.listUrls();
	}

	/*
	 * HIGH LEVEL UTILITIES
	 */
	/** Actually performs the matching logic. */
	protected void match(List<String> matched, String base, String currentPath, String pattern) {
		if (currentPath == null) {
			// Init
			File baseDir = new File(base.replace('/', File.separatorChar));
			File[] files = baseDir.listFiles();

			if (files == null) {
				if (logger.isLoggable(TRACE))
					logger.log(Level.WARNING, "Base dir " + baseDir + " has no children, exists=" + baseDir.exists()
							+ ", isDirectory=" + baseDir.isDirectory());
				return;
			}

			for (int i = 0; i < files.length; i++)
				match(matched, base, files[i].getName(), pattern);
		} else {
			PathMatcher matcher = FileSystems.getDefault().getPathMatcher(pattern);
			String fullPath = base + '/' + currentPath;
			if (matched.contains(fullPath))
				return;// don't try deeper if already matched

			boolean ok = matcher.matches(Paths.get(currentPath));
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
//							if (matcher.matchStart(pattern, newCurrentPath)) {
							// FIXME recurse only if start matches ?
							match(matched, base, newCurrentPath, pattern);
//							} else {
//								if (OsgiBootUtils.isDebug())
//									debug(newCurrentPath + " does not start match with " + pattern);
//
//							}
						} else {
							boolean nonDirectoryOk = matcher.matches(Paths.get(newCurrentPath));
							logger.log(TRACE,
									() -> currentPath + " " + (ok ? "" : " not ") + " matched with " + pattern);
							if (nonDirectoryOk)
								matched.add(relativeToFullPath(base, newCurrentPath));
						}
					}
				}
			}
		}
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

	private void refreshFramework() {
		Bundle systemBundle = bundleContext.getBundle(0);
		FrameworkWiring frameworkWiring = systemBundle.adapt(FrameworkWiring.class);
		// TODO deal with refresh breaking native loading (e.g SWT)
		frameworkWiring.refreshBundles(null);
	}

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
	}

	public String getProperty(String name) {
		return getProperty(name, null);
	}

	/*
	 * PLAIN OSGI LAUNCHER
	 */
	/** Launch an OSGi framework. OSGi Boot initialisation is NOT performed. */
	public static Framework defaultOsgiLaunch(Map<String, String> configuration) {
		Optional<FrameworkFactory> frameworkFactory = ServiceLoader.load(FrameworkFactory.class).findFirst();
		if (frameworkFactory.isEmpty())
			throw new IllegalStateException("No framework factory found");
		return defaultOsgiLaunch(frameworkFactory.get(), configuration);
	}

	/** Launch an OSGi framework. OSGi Boot initialisation is NOT performed. */
	public static Framework defaultOsgiLaunch(FrameworkFactory frameworkFactory, Map<String, String> configuration) {
		// start OSGi
		Framework framework = frameworkFactory.newFramework(configuration);
		try {
			framework.start();
		} catch (BundleException e) {
			throw new IllegalStateException("Cannot start OSGi framework", e);
		}
		return framework;
	}

	/*
	 * BEAN METHODS
	 */

	public BundleContext getBundleContext() {
		return bundleContext;
	}

}
