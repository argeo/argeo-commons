package org.argeo.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

/** Utilities around Java basic features. */
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
	 * MAP
	 */
	/**
	 * Creates a new {@link Dictionary} with one key-value pair. Key should not be
	 * null, but if the value is null, it returns an empty {@link Dictionary}.
	 */
	public static Map<String, Object> map(String key, Object value) {
		assert key != null;
		HashMap<String, Object> props = new HashMap<>();
		if (value != null)
			props.put(key, value);
		return props;
	}

	/*
	 * DICTIONARY
	 */

	/**
	 * Creates a new {@link Dictionary} with one key-value pair. Key should not be
	 * null, but if the value is null, it returns an empty {@link Dictionary}.
	 */
	public static Dictionary<String, Object> dict(String key, Object value) {
		assert key != null;
		Hashtable<String, Object> props = new Hashtable<>();
		if (value != null)
			props.put(key, value);
		return props;
	}

	/** @deprecated Use {@link #dict(String, Object)} instead. */
	@Deprecated
	public static Dictionary<String, Object> dico(String key, Object value) {
		return dict(key, value);
	}

	/** Converts a {@link Dictionary} to a {@link Map} of strings. */
	public static Map<String, String> dictToStringMap(Dictionary<String, ?> properties) {
		if (properties == null) {
			return null;
		}
		Map<String, String> res = new HashMap<>(properties.size());
		Enumeration<String> keys = properties.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			res.put(key, properties.get(key).toString());
		}
		return res;
	}

	/**
	 * Get a string property from this map, expecting to find it, or
	 * <code>null</code> if not found.
	 */
	public static String get(Map<String, ?> map, String key) {
		Object res = map.get(key);
		if (res == null)
			return null;
		return res.toString();
	}

	/**
	 * Get a string property from this map, expecting to find it.
	 * 
	 * @throws IllegalArgumentException if the key was not found
	 */
	public static String getNotNull(Map<String, ?> map, String key) {
		Object res = map.get(key);
		if (res == null)
			throw new IllegalArgumentException("Map " + map + " should contain key " + key);
		return res.toString();
	}

	/**
	 * Wraps the keys of the provided {@link Dictionary} as an {@link Iterable}.
	 */
	public static Iterable<String> keys(Dictionary<String, ?> props) {
		assert props != null;
		return new DictionaryKeys(props);
	}

	static String toJson(Dictionary<String, ?> props) {
		return toJson(props, false);
	}

	static String toJson(Dictionary<String, ?> props, boolean pretty) {
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

	static void storeAsProperties(Dictionary<String, Object> props, Path path) throws IOException {
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

	static void appendAsLdif(String dnBase, String dnKey, Dictionary<String, Object> props, Path path)
			throws IOException {
		if (props == null)
			throw new IllegalArgumentException("Props cannot be null");
		Object dnValue = props.get(dnKey);
		String dnStr = dnKey + '=' + dnValue + ',' + dnBase;
		LdapName dn;
		try {
			dn = new LdapName(dnStr);
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Cannot interpret DN " + dnStr, e);
		}
		if (dnValue == null)
			throw new IllegalArgumentException("DN key " + dnKey + " must have a value");
		try (Writer writer = Files.newBufferedWriter(path, StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
			writer.append("\ndn: ");
			writer.append(dn.toString());
			writer.append('\n');
			for (Enumeration<String> keys = props.keys(); keys.hasMoreElements();) {
				String key = keys.nextElement();
				Object value = props.get(key);
				writer.append(key);
				writer.append(": ");
				// FIXME deal with binary and multiple values
				writer.append(value.toString());
				writer.append('\n');
			}
		}
	}

	static Dictionary<String, Object> loadFromProperties(Path path) throws IOException {
		Properties toLoad = new Properties();
		try (InputStream in = Files.newInputStream(path)) {
			toLoad.load(in);
		}
		Dictionary<String, Object> res = new Hashtable<String, Object>();
		for (Object key : toLoad.keySet())
			res.put(key.toString(), toLoad.get(key));
		return res;
	}

	/*
	 * COLLECTIONS
	 */
	/**
	 * Convert a comma-separated separated {@link String} or a {@link String} array
	 * to a {@link List} of {@link String}, trimming them. Useful to quickly
	 * interpret OSGi services properties.
	 * 
	 * @return a {@link List} containing the trimmed {@link String}s, or an empty
	 *         {@link List} if the argument was <code>null</code>.
	 */
	public static List<String> toStringList(Object value) {
		List<String> values = new ArrayList<>();
		if (value == null)
			return values;
		String[] arr;
		if (value instanceof String) {
			arr = ((String) value).split(",");
		} else if (value instanceof String[]) {
			arr = (String[]) value;
		} else {
			throw new IllegalArgumentException("Unsupported value type " + value.getClass());
		}
		for (String str : arr) {
			values.add(str.trim());
		}
		return values;
	}

	/*
	 * EXCEPTIONS
	 */
	/**
	 * Chain the messages of all causes (one per line, <b>starts with a line
	 * return</b>) without all the stack
	 */
	public static String chainCausesMessages(Throwable t) {
		StringBuffer buf = new StringBuffer();
		chainCauseMessage(buf, t);
		return buf.toString();
	}

	/** Recursive chaining of messages */
	private static void chainCauseMessage(StringBuffer buf, Throwable t) {
		buf.append('\n').append(' ').append(t.getClass().getCanonicalName()).append(": ").append(t.getMessage());
		if (t.getCause() != null)
			chainCauseMessage(buf, t.getCause());
	}

	/*
	 * TIME
	 */
	/** Formats time elapsed since start. */
	public static String since(ZonedDateTime start) {
		ZonedDateTime now = ZonedDateTime.now();
		return duration(start, now);
	}

	/** Formats a duration. */
	public static String duration(Temporal start, Temporal end) {
		long count = ChronoUnit.DAYS.between(start, end);
		if (count != 0)
			return count > 1 ? count + " days" : count + " day";
		count = ChronoUnit.HOURS.between(start, end);
		if (count != 0)
			return count > 1 ? count + " hours" : count + " hours";
		count = ChronoUnit.MINUTES.between(start, end);
		if (count != 0)
			return count > 1 ? count + " minutes" : count + " minute";
		count = ChronoUnit.SECONDS.between(start, end);
		return count > 1 ? count + " seconds" : count + " second";
	}

	/** Singleton constructor. */
	private LangUtils() {

	}

}
