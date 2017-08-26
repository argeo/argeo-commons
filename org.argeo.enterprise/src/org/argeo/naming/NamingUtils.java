package org.argeo.naming;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NamingUtils {
	private final static DateTimeFormatter ldapDateTimeFormatter = DateTimeFormatter
			.ofPattern("uuuuMMddHHmmss[,S][.S]X");

	public static OffsetDateTime ldapDateToInstant(String ldapDate) {
		return OffsetDateTime.parse(ldapDate, ldapDateTimeFormatter);
	}

	public static String getQueryValue(Map<String, List<String>> query, String key) {
		if (!query.containsKey(key))
			return null;
		List<String> val = query.get(key);
		if (val.size() == 1)
			return val.get(0);
		else
			throw new IllegalArgumentException("There are " + val.size() + " value(s) for " + key);
	}

	public static Map<String, List<String>> queryToMap(URI uri) {
		return queryToMap(uri.getQuery());
	}

	private static Map<String, List<String>> queryToMap(String queryPart) {
		try {
			final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
			if (queryPart == null)
				return query_pairs;
			final String[] pairs = queryPart.split("&");
			for (String pair : pairs) {
				final int idx = pair.indexOf("=");
				final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name())
						: pair;
				if (!query_pairs.containsKey(key)) {
					query_pairs.put(key, new LinkedList<String>());
				}
				final String value = idx > 0 && pair.length() > idx + 1
						? URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name()) : null;
				query_pairs.get(key).add(value);
			}
			return query_pairs;
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException("Cannot convert " + queryPart + " to map", e);
		}
	}

	private NamingUtils() {

	}
}
