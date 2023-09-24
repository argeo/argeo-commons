package org.argeo.cms.acr.json;

import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.api.acr.QNamed;

import jakarta.json.stream.JsonGenerator;

/** Utilities around ACR and the JSON format. */
public class AcrJsonUtils {
	public static void writeAttr(JsonGenerator g, Content content, String attr) {
		writeAttr(g, content, NamespaceUtils.parsePrefixedName(attr));
	}

	public static void writeAttr(JsonGenerator g, Content content, QNamed attr) {
		writeAttr(g, content, attr.qName());
	}

	public static void writeAttr(JsonGenerator g, Content content, QName attr) {
		// String value = content.attr(attr);
		Object value = content.get(attr);
		if (value != null) {
			// TODO specify NamespaceContext
			String key = NamespaceUtils.toPrefixedName(attr);
			if (value instanceof Double v)
				g.write(key, v);
			else if (value instanceof Long v)
				g.write(key, v);
			else if (value instanceof Integer v)
				g.write(key, v);
			else if (value instanceof Boolean v)
				g.write(key, v);
			else
				g.write(key, value.toString());
		}
	}

	/** singleton */
	private AcrJsonUtils() {
	}
}
