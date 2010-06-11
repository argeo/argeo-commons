package org.argeo.slc.osgiboot;

public class OsgiBootUtils {

	public static void info(Object obj) {
		System.out.println("# OSGiBOOT      # " + obj);
	}

	public static void debug(Object obj) {
			System.out.println("# OSGiBOOT DBG  # " + obj);
	}

	public static void warn(Object obj) {
		System.out.println("# OSGiBOOT WARN # " + obj);
		// Because of a weird bug under Windows when starting it in a forked VM
		// if (System.getProperty("os.name").contains("Windows"))
		// System.out.println("# WARN " + obj);
		// else
		// System.err.println("# WARN " + obj);
	}

	//FIXE: returns null when defaultValue is ""
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

	public static String getPropertyCompat(String name, String oldName) {
		return getPropertyCompat(name, oldName, null);
	}

	public static String getPropertyCompat(String name, String oldName,
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

	public static void warnDeprecated(String name, String oldName) {
		warn("Property '" + oldName
				+ "' is deprecated and will be removed soon, use '" + name
				+ "' instead.");
	}	
	
}
