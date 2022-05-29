package org.argeo.init.osgi;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.StringTokenizer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/** Utilities, mostly related to logging. */
public class OsgiBootUtils {
	private final static Logger logger = System.getLogger(OsgiBootUtils.class.getName());

//	/** ISO8601 (as per log4j) and difference to UTC */
//	private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS Z");

//	static boolean debug = System.getProperty(OsgiBoot.PROP_ARGEO_OSGI_BOOT_DEBUG) == null ? false
//			: !System.getProperty(OsgiBoot.PROP_ARGEO_OSGI_BOOT_DEBUG).trim().equals("false");

	@Deprecated
	public static void info(Object obj) {
//		System.out.println("# OSGiBOOT      # " + dateFormat.format(new Date()) + " # " + obj);
		logger.log(Level.INFO, () -> Objects.toString(obj));
	}

	@Deprecated
	public static void debug(Object obj) {
//		if (debug)
//			System.out.println("# OSGiBOOT DBG  # " + dateFormat.format(new Date()) + " # " + obj);
		logger.log(Level.TRACE, () -> Objects.toString(obj));
	}

	@Deprecated
	public static void warn(Object obj) {
//		System.out.println("# OSGiBOOT WARN # " + dateFormat.format(new Date()) + " # " + obj);
		logger.log(Level.WARNING, () -> Objects.toString(obj));
	}

	@Deprecated
	public static void error(Object obj, Throwable e) {
//		System.err.println("# OSGiBOOT ERR  # " + dateFormat.format(new Date()) + " # " + obj);
//		if (e != null)
//			e.printStackTrace();
		logger.log(Level.ERROR, () -> Objects.toString(obj), e);
	}

	@Deprecated
	public static boolean isDebug() {
//		return debug;
		return logger.isLoggable(Level.TRACE);
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

	/**
	 * @return ==0: versions are identical, &lt;0: tested version is newer, &gt;0:
	 *         currentVersion is newer.
	 */
	public static int compareVersions(String currentVersion, String testedVersion) {
		List<String> cToks = new ArrayList<String>();
		StringTokenizer cSt = new StringTokenizer(currentVersion, ".");
		while (cSt.hasMoreTokens())
			cToks.add(cSt.nextToken());
		List<String> tToks = new ArrayList<String>();
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

	public static Framework launch(Map<String, String> configuration) {
		Optional<FrameworkFactory> frameworkFactory = ServiceLoader.load(FrameworkFactory.class).findFirst();
		if (frameworkFactory.isEmpty())
			throw new IllegalStateException("No framework factory found");
		return launch(frameworkFactory.get(), configuration);
	}

	/** Launch an OSGi framework. */
	public static Framework launch(FrameworkFactory frameworkFactory, Map<String, String> configuration) {
		// start OSGi
		Framework framework = frameworkFactory.newFramework(configuration);
		try {
			framework.start();
		} catch (BundleException e) {
			throw new IllegalStateException("Cannot start OSGi framework", e);
		}
		return framework;
	}

	@Deprecated
	public static Map<String, String> equinoxArgsToConfiguration(String[] args) {
		// FIXME implement it
		return new HashMap<>();
	}

}
