package org.argeo.cms.geo;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Geo data utilities. */
public class GeoJsonUtils {

	/** Add these properties to all features. */
	public static void addProperties(JsonNode tree, Map<String, String> map) {
		for (JsonNode feature : tree.get("features")) {
			ObjectNode properties = (ObjectNode) feature.get("properties");
			for (String key : map.keySet()) {
				properties.put(key, map.get(key));
			}
		}
	}

	/** Singleton. */
	private GeoJsonUtils() {
	}
}
