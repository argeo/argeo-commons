package org.argeo.cms.acr;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;

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

	public static final char SLASH = '/';
	public static final String ROOT_SLASH = "" + SLASH;

	/**
	 * Split a path (with '/' separator) in an array of length 2, the first part
	 * being the parent path (which could be either absolute or relative), the
	 * second one being the last segment, (guaranteed to be without a '/').
	 */
	public static String[] getParentPath(String path) {
		if (path == null)
			throw new IllegalArgumentException("Path cannot be null");
		if (path.length() == 0)
			throw new IllegalArgumentException("Path cannot be empty");
		checkDoubleSlash(path);
		int parentIndex = path.lastIndexOf(SLASH);
		if (parentIndex == path.length() - 1) {// trailing '/'
			path = path.substring(0, path.length() - 1);
			parentIndex = path.lastIndexOf(SLASH);
		}

		if (parentIndex == -1) // no '/'
			return new String[] { "", path };

		return new String[] { parentIndex != 0 ? path.substring(0, parentIndex) : "" + SLASH,
				path.substring(parentIndex + 1) };
	}

	public static List<String> toPathSegments(String path) {
		List<String> res = new ArrayList<>();
		if ("".equals(path) || ROOT_SLASH.equals(path))
			return res;
		collectPathSegments(path, res);
		return res;
	}

	private static void collectPathSegments(String path, List<String> segments) {
		String[] parent = getParentPath(path);
		if ("".equals(parent[1])) // root
			return;
		segments.add(0, parent[1]);
		if ("".equals(parent[0])) // end
			return;
		collectPathSegments(parent[0], segments);
	}

	public static void checkDoubleSlash(String path) {
		if (path.contains(SLASH + "" + SLASH))
			throw new IllegalArgumentException("Path " + path + " contains //");
	}

	/** Singleton. */
	private ContentUtils() {

	}

}
