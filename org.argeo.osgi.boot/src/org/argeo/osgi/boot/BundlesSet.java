package org.argeo.osgi.boot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/** Intermediary structure used by path matching */
class BundlesSet {
	private String baseUrl = "reference:file";// not used yet
	private final String dir;
	private List<String> includes = new ArrayList<String>();
	private List<String> excludes = new ArrayList<String>();

	public BundlesSet(String def) {
		StringTokenizer st = new StringTokenizer(def, ";");

		if (!st.hasMoreTokens())
			throw new RuntimeException("Base dir not defined.");
		try {
			String dirPath = st.nextToken();

			if (dirPath.startsWith("file:"))
				dirPath = dirPath.substring("file:".length());

			dir = new File(dirPath.replace('/', File.separatorChar)).getCanonicalPath();
			if (OsgiBootUtils.debug)
				OsgiBootUtils.debug("Base dir: " + dir);
		} catch (IOException e) {
			throw new RuntimeException("Cannot convert to absolute path", e);
		}

		while (st.hasMoreTokens()) {
			String tk = st.nextToken();
			StringTokenizer stEq = new StringTokenizer(tk, "=");
			String type = stEq.nextToken();
			String pattern = stEq.nextToken();
			if ("in".equals(type) || "include".equals(type)) {
				includes.add(pattern);
			} else if ("ex".equals(type) || "exclude".equals(type)) {
				excludes.add(pattern);
			} else if ("baseUrl".equals(type)) {
				baseUrl = pattern;
			} else {
				System.err.println("Unkown bundles pattern type " + type);
			}
		}

		// if (excludeSvn && !excludes.contains(EXCLUDES_SVN_PATTERN)) {
		// excludes.add(EXCLUDES_SVN_PATTERN);
		// }
	}

	public String getDir() {
		return dir;
	}

	public List<String> getIncludes() {
		return includes;
	}

	public List<String> getExcludes() {
		return excludes;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

}
