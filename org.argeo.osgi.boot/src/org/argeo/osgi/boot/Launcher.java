package org.argeo.osgi.boot;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.osgi.framework.BundleContext;

/** Command line interface. */
public class Launcher {

	public static void main(String[] args) {
		// Try to load system properties
		String systemPropertiesFilePath = getProperty(OsgiBoot.PROP_ARGEO_OSGI_BOOT_SYSTEM_PROPERTIES_FILE);
		if (systemPropertiesFilePath != null) {
			FileInputStream in;
			try {
				in = new FileInputStream(systemPropertiesFilePath);
				System.getProperties().load(in);
			} catch (IOException e1) {
				throw new RuntimeException("Cannot load system properties from " + systemPropertiesFilePath, e1);
			}
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
					// silent
				}
			}
		}

		// Start main class
		startMainClass();

		// Start Equinox
		BundleContext bundleContext = null;
		try {
			bundleContext = EclipseStarter.startup(args, null);
		} catch (Exception e) {
			throw new RuntimeException("Cannot start Equinox.", e);
		}

		// OSGi bootstrap
		OsgiBoot osgiBoot = new OsgiBoot(bundleContext);
		osgiBoot.bootstrap();
	}

	protected static void startMainClass() {
		String className = getProperty(OsgiBoot.PROP_ARGEO_OSGI_BOOT_APPCLASS);
		if (className == null)
			return;

		String line = System.getProperty(OsgiBoot.PROP_ARGEO_OSGI_BOOT_APPARGS, "");

		String[] uiArgs = readArgumentsFromLine(line);

		try {
			// Launch main method using reflection
			Class<?> clss = Class.forName(className);
			Class<?>[] mainArgsClasses = new Class[] { uiArgs.getClass() };
			Object[] mainArgs = { uiArgs };
			Method mainMethod = clss.getMethod("main", mainArgsClasses);
			mainMethod.invoke(null, mainArgs);
		} catch (Exception e) {
			throw new RuntimeException("Cannot start main class.", e);
		}

	}

	/**
	 * Transform a line into an array of arguments, taking "" as single
	 * arguments. (nested \" are not supported)
	 */
	private static String[] readArgumentsFromLine(String lineOrig) {
		String line = lineOrig.trim();// remove trailing spaces
		List<String> args = new ArrayList<String>();
		StringBuffer curr = new StringBuffer("");
		boolean inQuote = false;
		char[] arr = line.toCharArray();
		for (int i = 0; i < arr.length; i++) {
			char c = arr[i];
			switch (c) {
			case '\"':
				inQuote = !inQuote;
				break;
			case ' ':
				if (!inQuote) {// otherwise, no break: goes to default
					if (curr.length() > 0) {
						args.add(curr.toString());
						curr = new StringBuffer("");
					}
					break;
				}
			default:
				curr.append(c);
				break;
			}
		}

		// Add last arg
		if (curr.length() > 0) {
			args.add(curr.toString());
			curr = null;
		}

		String[] res = new String[args.size()];
		for (int i = 0; i < args.size(); i++) {
			res[i] = args.get(i).toString();
		}
		return res;
	}

	public static String getProperty(String name, String defaultValue) {
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

	public static String getProperty(String name) {
		return getProperty(name, null);
	}

}
