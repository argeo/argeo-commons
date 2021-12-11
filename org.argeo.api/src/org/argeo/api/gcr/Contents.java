package org.argeo.api.gcr;

import java.io.PrintStream;
import java.util.function.BiConsumer;

public class Contents {
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
		for (String key : content.keySet()) {
			out.println(prefix + " " + key + "=" + content.get(key));
		}
		if (printText) {
			if (content.hasText()) {
				out.println("<![CDATA[" + content.getText().trim() + "]]>");
			}
		}
	}

	public static <T> boolean isString(T t) {
		return t instanceof String;
	}

	/** Singleton. */
	private Contents() {

	}
}
