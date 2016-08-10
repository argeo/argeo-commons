package org.argeo.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

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
	 * Creates a new {@link Dictionary} with one key-value pair (neither key not
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

	public static void storeAsProperties(Dictionary<String, Object> props, Path path) throws IOException {
		if (props == null)
			throw new IllegalArgumentException("Props cannot be null");
		Properties toStore = new Properties();
		for (Enumeration<String> keys = props.keys(); keys.hasMoreElements();) {
			String key = keys.nextElement();
			toStore.setProperty(key, props.get(key).toString());
		}
		try (OutputStream out = Files.newOutputStream(path)) {
			toStore.store(out, null);
		}
	}

	public static Dictionary<String, Object> loadFromProperties(Path path) throws IOException {
		Properties toLoad = new Properties();
		try (InputStream in = Files.newInputStream(path)) {
			toLoad.load(in);
		}
		Dictionary<String, Object> res = new Hashtable<String, Object>();
		for (Object key : toLoad.keySet())
			res.put(key.toString(), toLoad.get(key));
		return res;
	}

	/** Singleton constructor. */
	private LangUtils() {

	}

}