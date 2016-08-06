package org.argeo.util;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

public class LangUtils {
	/*
	 * NON-API OSGi
	 */
	/**
	 * Returns an array with the names of the provided classes. Useful when
	 * registering services with multiple interfaces in OSGi.
	 */
	public static String[] names(Class<?>... clzz) {
		String[] res = new String[clzz.length];
		for (int i = 0; i < clzz.length; i++)
			res[i] = clzz[i].getName();
		return res;
	}

	/*
	 * DICTIONARY
	 */

	/**
	 * Creates a new {@link Dictionary} with one key-value pair (neith key not
	 * value should be null)
	 */
	public static Dictionary<String, Object> init(String key, Object value) {
		assert key != null;
		assert value != null;
		Hashtable<String, Object> props = new Hashtable<>();
		props.put(key, value);
		return props;
	}

	/**
	 * Wraps the keys of the provided {@link Dictionary} as an {@link Iterable}.
	 */
	public static Iterable<String> keys(Dictionary<String, ?> props) {
		assert props != null;
		return new DictionaryKeys(props);
	}

	public static String toJson(Dictionary<String, ?> props) {
		return toJson(props, false);
	}

	public static String toJson(Dictionary<String, ?> props, boolean pretty) {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		if (pretty)
			sb.append('\n');
		Enumeration<String> keys = props.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			if (pretty)
				sb.append(' ');
			sb.append('\"').append(key).append('\"');
			if (pretty)
				sb.append(" : ");
			else
				sb.append(':');
			sb.append('\"').append(props.get(key)).append('\"');
			if (keys.hasMoreElements())
				sb.append(", ");
			if (pretty)
				sb.append('\n');
		}
		sb.append('}');
		return sb.toString();
	}

	/** Singleton constructor. */
	private LangUtils() {

	}

}
