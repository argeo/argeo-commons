package org.argeo.slc.osgiboot;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.osgi.framework.BundleContext;

public class Launcher {

	public static void main(String[] args) {
		// Try to load system properties
		String systemPropertiesFilePath = OsgiBootUtils.getPropertyCompat(
				OsgiBoot.PROP_ARGEO_OSGI_BOOT_SYSTEM_PROPERTIES_FILE,
				OsgiBoot.PROP_SLC_OSGIBOOT_SYSTEM_PROPERTIES_FILE);
		if (systemPropertiesFilePath != null) {
			FileInputStream in;
			try {
				in = new FileInputStream(systemPropertiesFilePath);
				System.getProperties().load(in);
			} catch (IOException e1) {
				throw new RuntimeException(
						"Cannot load system properties from "
								+ systemPropertiesFilePath, e1);
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
		// Properties config = System.getProperties();
		// String className = config.getProperty("slc.osgiboot.appclass");
		String className = OsgiBootUtils.getPropertyCompat(
				OsgiBoot.PROP_ARGEO_OSGI_BOOT_APPCLASS,
				OsgiBoot.PROP_SLC_OSGIBOOT_APPCLASS);
		if (className == null)
			return;

		// should use OsgiBootUtils.getPropertyCompat(), but it does not 
		// work for "" as default value
		// so no warning displayed if PROP_SLC_OSGIBOOT_APPARGS is used
		// FIXME: change OsgiBootUtils.getPropertyCompat()
		String line = System.getProperty(OsgiBoot.PROP_ARGEO_OSGI_BOOT_APPARGS, 
				System.getProperty(OsgiBoot.PROP_SLC_OSGIBOOT_APPARGS, ""));		
		
		String[] uiArgs = readArgumentsFromLine(line);
		
		try {
			// Launch main method using reflection
			Class clss = Class.forName(className);
			Class[] mainArgsClasses = new Class[] { uiArgs.getClass() };
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
		// System.out.println("line=" + line);
		List args = new Vector();
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
			// System.out.println("res[i]=" + res[i]);
		}
		return res;
	}

}
