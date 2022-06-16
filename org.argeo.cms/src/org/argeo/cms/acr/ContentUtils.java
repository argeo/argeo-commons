package org.argeo.cms.acr;

import java.io.PrintStream;
import java.util.function.BiConsumer;

import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.api.cms.CmsSession;

/** Utilities and routines around {@link Content}. */
public class ContentUtils {
	public static void traverse(Content content, BiConsumer<Content, Integer> doIt) {
		traverse(content, doIt, (Integer) null);
	}

	public static void traverse(Content content, BiConsumer<Content, Integer> doIt, Integer maxDepth) {
		doTraverse(content, doIt, 0, maxDepth);
	}

	private static void doTraverse(Content content, BiConsumer<Content, Integer> doIt, int currentDepth,
			Integer maxDepth) {
		doIt.accept(content, currentDepth);
		if (maxDepth != null && currentDepth == maxDepth)
			return;
		int nextDepth = currentDepth + 1;
		for (Content child : content) {
			doTraverse(child, doIt, nextDepth, maxDepth);
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

//	public static <T> boolean isString(T t) {
//		return t instanceof String;
//	}

	/**
	 * Split a path (with '/' separator) in an array of length 2, the first part
	 * being the parent path (which could be either absolute or relative), the
	 * second one being the last segment, (guaranteed to be with '/').
	 */
	public static String[] getParentPath(String path) {
		int parentIndex = path.lastIndexOf('/');
		// TODO make it more robust
		return new String[] { parentIndex != 0 ? path.substring(0, parentIndex) : "/",
				path.substring(parentIndex + 1) };
	}

	/** Singleton. */
	private ContentUtils() {

	}

}
