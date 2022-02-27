package org.argeo.api.acr;

import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.function.BiConsumer;

import javax.xml.namespace.QName;

public class ContentUtils {
	public static void traverse(Content content, BiConsumer<Content, Integer> doIt) {
		traverse(content, doIt, 0);
	}

	public static void traverse(Content content, BiConsumer<Content, Integer> doIt, int currentDepth) {
		doIt.accept(content, currentDepth);
		int nextDepth = currentDepth + 1;
		for (Content child : content) {
			traverse(child, doIt, nextDepth);
		}
	}

	public static void print(Content content, PrintStream out, int depth, boolean printText) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < depth; i++) {
			sb.append("  ");
		}
		String prefix = sb.toString();
		out.println(prefix + content.getName());
		for (QName key : content.keySet()) {
			out.println(prefix + " " + key + "=" + content.get(key));
		}
		if (printText) {
			if (content.hasText()) {
				out.println("<![CDATA[" + content.getText().trim() + "]]>");
			}
		}
	}

	public static URI bytesToDataURI(byte[] arr) {
		String base64Str = Base64.getEncoder().encodeToString(arr);
		try {
			final String PREFIX = "data:application/octet-stream;base64,";
			return new URI(PREFIX + base64Str);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Cannot serialize bytes a Base64 data URI", e);
		}

	}

	public static byte[] bytesFromDataURI(URI uri) {
		if (!"data".equals(uri.getScheme()))
			throw new IllegalArgumentException("URI must have 'data' as a scheme");
		String schemeSpecificPart = uri.getSchemeSpecificPart();
		int commaIndex = schemeSpecificPart.indexOf(',');
		String prefix = schemeSpecificPart.substring(0, commaIndex);
		List<String> info = Arrays.asList(prefix.split(";"));
		if (!info.contains("base64"))
			throw new IllegalArgumentException("URI must specify base64");

		String base64Str = schemeSpecificPart.substring(commaIndex + 1);
		return Base64.getDecoder().decode(base64Str);

	}

	public static <T> boolean isString(T t) {
		return t instanceof String;
	}

	/** Singleton. */
	private ContentUtils() {

	}
}
