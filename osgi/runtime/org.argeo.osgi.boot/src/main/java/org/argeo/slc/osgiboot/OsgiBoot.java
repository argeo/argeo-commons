package org.argeo.slc.osgiboot;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.argeo.slc.osgiboot.internal.springutil.AntPathMatcher;
import org.argeo.slc.osgiboot.internal.springutil.PathMatcher;
import org.argeo.slc.osgiboot.internal.springutil.SystemPropertyUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

public class OsgiBoot {
	public final static String SYMBOLIC_NAME_OSGI_BOOT = "org.argeo.osgi.boot";
	public final static String SYMBOLIC_NAME_EQUINOX = "org.eclipse.osgi";

	public final static String PROP_ARGEO_OSGI_DATA_DIR = "argeo.osgi.data.dir";

	public final static String PROP_ARGEO_OSGI_START = "argeo.osgi.start";
	public final static String PROP_ARGEO_OSGI_BUNDLES = "argeo.osgi.bundles";
	public final static String PROP_ARGEO_OSGI_LOCATIONS = "argeo.osgi.locations";
	public final static String PROP_ARGEO_OSGI_BASE_URL = "argeo.osgi.baseUrl";
	public final static String PROP_ARGEO_OSGI_MODULES_URL = "argeo.osgi.modulesUrl";

	public final static String PROP_ARGEO_OSGI_BOOT_DEBUG = "argeo.osgi.boot.debug";
	public final static String PROP_ARGEO_OSGI_BOOT_DEFAULT_TIMEOUT = "argeo.osgi.boot.defaultTimeout";
	public final static String PROP_ARGEO_OSGI_BOOT_MODULES_URL_SEPARATOR = "argeo.osgi.boot.modulesUrlSeparator";
	public final static String PROP_ARGEO_OSGI_BOOT_SYSTEM_PROPERTIES_FILE = "argeo.osgi.boot.systemPropertiesFile";

	/** @deprecated */
	public final static String PROP_SLC_OSGI_START = "slc.osgi.start";
	/** @deprecated */
	public final static String PROP_SLC_OSGI_BUNDLES = "slc.osgi.bundles";
	/** @deprecated */
	public final static String PROP_SLC_OSGI_LOCATIONS = "slc.osgi.locations";
	/** @deprecated */
	public final static String PROP_SLC_OSGI_BASE_URL = "slc.osgi.baseUrl";
	/** @deprecated */
	public final static String PROP_SLC_OSGI_MODULES_URL = "slc.osgi.modulesUrl";

	/** @deprecated */
	public final static String PROP_SLC_OSGIBOOT_DEBUG = "slc.osgiboot.debug";
	/** @deprecated */
	public final static String PROP_SLC_OSGIBOOT_DEFAULT_TIMEOUT = "slc.osgiboot.defaultTimeout";
	/** @deprecated */
	public final static String PROP_SLC_OSGIBOOT_MODULES_URL_SEPARATOR = "slc.osgiboot.modulesUrlSeparator";
	/** @deprecated */
	public final static String PROP_SLC_OSGIBOOT_SYSTEM_PROPERTIES_FILE = "slc.osgiboot.systemPropertiesFile";

	public final static String DEFAULT_BASE_URL = "reference:file:";
	public final static String EXCLUDES_SVN_PATTERN = "**/.svn/**";

	private boolean debug = Boolean.valueOf(
			System.getProperty(PROP_ARGEO_OSGI_BOOT_DEBUG, System.getProperty(
					PROP_SLC_OSGIBOOT_DEBUG, "false"))).booleanValue();
	/** Default is 10s (set in constructor) */
	private long defaultTimeout;

	private boolean excludeSvn = true;
	/** Default is ',' (set in constructor) */
	private String modulesUrlSeparator = ",";

	private final BundleContext bundleContext;

	public OsgiBoot(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		defaultTimeout = Long.parseLong(getPropertyCompat(
				PROP_ARGEO_OSGI_BOOT_DEFAULT_TIMEOUT,
				PROP_SLC_OSGIBOOT_DEFAULT_TIMEOUT, "10000"));
		modulesUrlSeparator = getPropertyCompat(
				PROP_ARGEO_OSGI_BOOT_MODULES_URL_SEPARATOR,
				PROP_SLC_OSGIBOOT_MODULES_URL_SEPARATOR, ",");
		initSystemProperties();
	}

	protected void initSystemProperties() {
		String osgiInstanceArea = System.getProperty("osgi.instance.area");
		String osgiInstanceAreaDefault = System
				.getProperty("osgi.instance.area.default");
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

	public static String removeFilePrefix(String url) {
		if (url.startsWith("file:"))
			return url.substring("file:".length());
		else if (url.startsWith("reference:file:"))
			return url.substring("reference:file:".length());
		else
			return url;
	}

	public void bootstrap() {
		long begin = System.currentTimeMillis();
		System.out.println();
		info("OSGi bootstrap starting...");
		info("Writable data directory : "
				+ System.getProperty(PROP_ARGEO_OSGI_DATA_DIR)
				+ " (set as system property " + PROP_ARGEO_OSGI_DATA_DIR + ")");
		installUrls(getBundlesUrls());
		installUrls(getLocationsUrls());
		installUrls(getModulesUrls());
		checkUnresolved();
		startBundles();
		long duration = System.currentTimeMillis() - begin;
		info("OSGi bootstra completed in "
				+ Math.round(((double) duration) / 1000) + "s (" + duration
				+ "ms), " + bundleContext.getBundles().length + " bundles");
		System.out.println();
	}

	public void installUrls(List urls) {
		Map installedBundles = getInstalledBundles();
		for (int i = 0; i < urls.size(); i++) {
			String url = (String) urls.get(i);
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
				if ((message.contains("Bundle \"" + SYMBOLIC_NAME_OSGI_BOOT
						+ "\"") || message.contains("Bundle \""
						+ SYMBOLIC_NAME_EQUINOX + "\""))
						&& message.contains("has already been installed")) {
					// silent, in order to avoid warnings: we know that both
					// have already been installed...
				} else {
					warn("Could not install bundle from " + url + ": "
							+ message);
				}
				if (debug)
					e.printStackTrace();
			}
		}

	}

	public void installOrUpdateUrls(Map urls) {
		Map installedBundles = getBundles();

		for (Iterator modules = urls.keySet().iterator(); modules.hasNext();) {
			String moduleName = (String) modules.next();
			String urlStr = (String) urls.get(moduleName);
			if (installedBundles.containsKey(moduleName)) {
				Bundle bundle = (Bundle) installedBundles.get(moduleName);
				InputStream in;
				try {
					URL url = new URL(urlStr);
					in = url.openStream();
					bundle.update(in);
					info("Updated bundle " + moduleName + " from " + urlStr);
				} catch (Exception e) {
					throw new RuntimeException("Cannot update " + moduleName
							+ " from " + urlStr);
				}
				if (in != null)
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			} else {
				try {
					Bundle bundle = bundleContext.installBundle(urlStr);
					if (debug)
						debug("Installed bundle " + bundle.getSymbolicName()
								+ " from " + urlStr);
				} catch (BundleException e) {
					warn("Could not install bundle from " + urlStr + ": "
							+ e.getMessage());
				}
			}
		}

	}

	public void startBundles() {
		String bundlesToStart = getPropertyCompat(PROP_ARGEO_OSGI_START,
				PROP_SLC_OSGI_START);
		startBundles(bundlesToStart);
	}

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

	public void startBundles(List bundlesToStart) {
		if (bundlesToStart.size() == 0)
			return;

		// used to log the bundles not found
		List notFoundBundles = new ArrayList(bundlesToStart);

		Bundle[] bundles = bundleContext.getBundles();
		long startBegin = System.currentTimeMillis();
		for (int i = 0; i < bundles.length; i++) {
			Bundle bundle = bundles[i];
			String symbolicName = bundle.getSymbolicName();
			if (bundlesToStart.contains(symbolicName))
				try {
					try {
						bundle.start();
					} catch (Exception e) {
						warn("Start of bundle " + symbolicName
								+ " failed because of " + e
								+ ", maybe bundle is not yet resolved,"
								+ " waiting and trying again.");
						waitForBundleResolvedOrActive(startBegin, bundle);
						bundle.start();
					}
					notFoundBundles.remove(symbolicName);
				} catch (Exception e) {
					warn("Bundle " + symbolicName + " cannot be started: "
							+ e.getMessage());
					if (debug)
						e.printStackTrace();
					// was found even if start failed
					notFoundBundles.remove(symbolicName);
				}
		}

		for (int i = 0; i < notFoundBundles.size(); i++)
			warn("Bundle " + notFoundBundles.get(i)
					+ " not started because it was not found.");
	}

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
			warn("Unresolved bundles " + unresolvedBundles);
		}
	}

	protected void waitForBundleResolvedOrActive(long startBegin, Bundle bundle)
			throws Exception {
		int originalState = bundle.getState();
		if ((originalState == Bundle.RESOLVED)
				|| (originalState == Bundle.ACTIVE))
			return;

		String originalStateStr = stateAsString(originalState);

		int currentState = bundle.getState();
		while (!(currentState == Bundle.RESOLVED || currentState == Bundle.ACTIVE)) {
			long now = System.currentTimeMillis();
			if ((now - startBegin) > defaultTimeout)
				throw new Exception("Bundle " + bundle.getSymbolicName()
						+ " was not RESOLVED or ACTIVE after "
						+ (now - startBegin) + "ms (originalState="
						+ originalStateStr + ", currentState="
						+ stateAsString(currentState) + ")");

			try {
				Thread.sleep(100l);
			} catch (InterruptedException e) {
				// silent
			}
			currentState = bundle.getState();
		}
	}

	public static String stateAsString(int state) {
		switch (state) {
		case Bundle.UNINSTALLED:
			return "UNINSTALLED";
		case Bundle.INSTALLED:
			return "INSTALLED";
		case Bundle.RESOLVED:
			return "RESOLVED";
		case Bundle.STARTING:
			return "STARTING";
		case Bundle.ACTIVE:
			return "ACTIVE";
		case Bundle.STOPPING:
			return "STOPPING";
		default:
			return Integer.toString(state);
		}
	}

	/** Key is location */
	public Map getInstalledBundles() {
		Map installedBundles = new HashMap();

		Bundle[] bundles = bundleContext.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			installedBundles.put(bundles[i].getLocation(), bundles[i]);
		}
		return installedBundles;
	}

	/** Key is symbolic name */
	public Map getBundles() {
		Map namedBundles = new HashMap();
		Bundle[] bundles = bundleContext.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			namedBundles.put(bundles[i].getSymbolicName(), bundles[i]);
		}
		return namedBundles;
	}

	public List getLocationsUrls() {
		String baseUrl = getPropertyCompat(PROP_ARGEO_OSGI_BASE_URL,
				PROP_SLC_OSGI_BASE_URL, DEFAULT_BASE_URL);
		String bundleLocations = getPropertyCompat(PROP_ARGEO_OSGI_LOCATIONS,
				PROP_SLC_OSGI_LOCATIONS);
		return getLocationsUrls(baseUrl, bundleLocations);
	}

	public List getModulesUrls() {
		List urls = new ArrayList();
		String modulesUrlStr = getPropertyCompat(PROP_ARGEO_OSGI_MODULES_URL,
				PROP_SLC_OSGI_MODULES_URL);
		if (modulesUrlStr == null)
			return urls;

		String baseUrl = getPropertyCompat(PROP_ARGEO_OSGI_BASE_URL,
				PROP_SLC_OSGI_BASE_URL);

		Map installedBundles = getBundles();

		BufferedReader reader = null;
		try {
			URL modulesUrl = new URL(modulesUrlStr);
			reader = new BufferedReader(new InputStreamReader(modulesUrl
					.openStream()));
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
					String bundleVersion = bundle.getHeaders().get(
							Constants.BUNDLE_VERSION).toString();
					int comp = compareVersions(bundleVersion, moduleVersion);
					if (comp > 0) {
						warn("Installed version " + bundleVersion
								+ " of bundle " + moduleName
								+ " is newer than  provided version "
								+ moduleVersion);
					} else if (comp < 0) {
						urls.add(url);
						info("Updated bundle " + moduleName + " with version "
								+ moduleVersion + " (old version was "
								+ bundleVersion + ")");
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

	/**
	 * @return ==0: versions are identical, <0: tested version is newer, >0:
	 *         currentVersion is newer.
	 */
	protected int compareVersions(String currentVersion, String testedVersion) {
		List cToks = new ArrayList();
		StringTokenizer cSt = new StringTokenizer(currentVersion, ".");
		while (cSt.hasMoreTokens())
			cToks.add(cSt.nextToken());
		List tToks = new ArrayList();
		StringTokenizer tSt = new StringTokenizer(currentVersion, ".");
		while (tSt.hasMoreTokens())
			tToks.add(tSt.nextToken());

		int comp = 0;
		comp: for (int i = 0; i < cToks.size(); i++) {
			if (tToks.size() <= i) {
				// equals until then, tested shorter
				comp = 1;
				break comp;
			}

			String c = (String) cToks.get(i);
			String t = (String) tToks.get(i);

			try {
				int cInt = Integer.parseInt(c);
				int tInt = Integer.parseInt(t);
				if (cInt == tInt)
					continue comp;
				else {
					comp = (cInt - tInt);
					break comp;
				}
			} catch (NumberFormatException e) {
				if (c.equals(t))
					continue comp;
				else {
					comp = c.compareTo(t);
					break comp;
				}
			}
		}

		if (comp == 0 && tToks.size() > cToks.size()) {
			// equals until then, current shorter
			comp = -1;
		}

		return comp;
	}

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

	public List getBundlesUrls() {
		String baseUrl = getPropertyCompat(PROP_ARGEO_OSGI_BASE_URL,
				PROP_SLC_OSGI_BASE_URL, DEFAULT_BASE_URL);
		String bundlePatterns = getPropertyCompat(PROP_ARGEO_OSGI_BUNDLES,
				PROP_SLC_OSGI_BUNDLES);
		return getBundlesUrls(baseUrl, bundlePatterns);
	}

	public List getBundlesUrls(String baseUrl, String bundlePatterns) {
		List urls = new ArrayList();

		List bundlesSets = new ArrayList();
		if (bundlePatterns == null)
			return urls;
		bundlePatterns = SystemPropertyUtils
				.resolvePlaceholders(bundlePatterns);
		if (debug)
			debug(PROP_ARGEO_OSGI_BUNDLES + "=" + bundlePatterns
					+ " (excludeSvn=" + excludeSvn + ")");

		StringTokenizer st = new StringTokenizer(bundlePatterns, ",");
		while (st.hasMoreTokens()) {
			bundlesSets.add(new BundlesSet(st.nextToken()));
		}

		List included = new ArrayList();
		PathMatcher matcher = new AntPathMatcher();
		for (int i = 0; i < bundlesSets.size(); i++) {
			BundlesSet bundlesSet = (BundlesSet) bundlesSets.get(i);
			for (int j = 0; j < bundlesSet.getIncludes().size(); j++) {
				String pattern = (String) bundlesSet.getIncludes().get(j);
				match(matcher, included, bundlesSet.getDir(), null, pattern);
			}
		}

		List excluded = new ArrayList();
		for (int i = 0; i < bundlesSets.size(); i++) {
			BundlesSet bundlesSet = (BundlesSet) bundlesSets.get(i);
			for (int j = 0; j < bundlesSet.getExcludes().size(); j++) {
				String pattern = (String) bundlesSet.getExcludes().get(j);
				match(matcher, excluded, bundlesSet.getDir(), null, pattern);
			}
		}

		for (int i = 0; i < included.size(); i++) {
			String fullPath = (String) included.get(i);
			if (!excluded.contains(fullPath))
				urls.add(locationToUrl(baseUrl, fullPath));
		}

		return urls;
	}

	protected void match(PathMatcher matcher, List matched, String base,
			String currentPath, String pattern) {
		if (currentPath == null) {
			// Init
			File baseDir = new File(base.replace('/', File.separatorChar));
			File[] files = baseDir.listFiles();

			if (files == null) {
				warn("Base dir " + baseDir + " has no children, exists="
						+ baseDir.exists() + ", isDirectory="
						+ baseDir.isDirectory());
				return;
			}

			for (int i = 0; i < files.length; i++)
				match(matcher, matched, base, files[i].getName(), pattern);
		} else {
			String fullPath = base + '/' + currentPath;
			if (matched.contains(fullPath))
				return;// don't try deeper if already matched

			boolean ok = matcher.match(pattern, currentPath);
			if (debug)
				debug(currentPath + " " + (ok ? "" : " not ")
						+ " matched with " + pattern);
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

	protected static void info(Object obj) {
		System.out.println("# OSGiBOOT      # " + obj);
	}

	protected void debug(Object obj) {
		if (debug)
			System.out.println("# OSGiBOOT DBG  # " + obj);
	}

	protected void warn(Object obj) {
		System.out.println("# OSGiBOOT WARN # " + obj);
		// Because of a weird bug under Windows when starting it in a forked VM
		// if (System.getProperty("os.name").contains("Windows"))
		// System.out.println("# WARN " + obj);
		// else
		// System.err.println("# WARN " + obj);
	}

	protected String getProperty(String name, String defaultValue) {
		final String value;
		if (defaultValue != null)
			value = System.getProperty(name, defaultValue);
		else
			value = System.getProperty(name);

		if (value == null || value.equals(""))
			return null;
		else
			return value;
	}

	protected String getProperty(String name) {
		return getProperty(name, null);
	}

	protected String getPropertyCompat(String name, String oldName) {
		return getPropertyCompat(name, oldName, null);
	}

	protected String getPropertyCompat(String name, String oldName,
			String defaultValue) {
		String res = null;

		if (defaultValue != null) {
			res = getProperty(name, defaultValue);
			if (res.equals(defaultValue)) {
				res = getProperty(oldName, defaultValue);
				if (!res.equals(defaultValue))
					warnDeprecated(name, oldName);
			}
		} else {
			res = getProperty(name, null);
			if (res == null) {
				res = getProperty(oldName, null);
				if (res != null)
					warnDeprecated(name, oldName);
			}
		}
		return res;
	}

	protected void warnDeprecated(String name, String oldName) {
		warn("Property '" + oldName
				+ "' is deprecated and will be removed soon, use '" + name
				+ "' instead.");
	}

	public boolean getDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}

	/** Whether to exclude Subversion directories (true by default) */
	public boolean isExcludeSvn() {
		return excludeSvn;
	}

	public void setExcludeSvn(boolean excludeSvn) {
		this.excludeSvn = excludeSvn;
	}

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

	public void setDefaultTimeout(long defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
	}

	public void setModulesUrlSeparator(String modulesUrlSeparator) {
		this.modulesUrlSeparator = modulesUrlSeparator;
	}

}
