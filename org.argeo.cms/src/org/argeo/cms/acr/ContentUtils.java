package org.argeo.cms.acr;

import java.io.PrintStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.StringTokenizer;
import java.util.function.BiConsumer;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentRepository;
import org.argeo.api.acr.ContentSession;
import org.argeo.api.acr.DName;
import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsSession;
import org.argeo.api.cms.directory.CmsDirectory;
import org.argeo.api.cms.directory.CmsUserManager;
import org.argeo.api.cms.directory.HierarchyUnit;
import org.argeo.api.cms.directory.UserDirectory;
import org.argeo.cms.util.CurrentSubject;
import org.osgi.service.useradmin.Role;

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
		String txt = "";
		if (printText) {
			if (content.hasText()) {
				final int MAX_LENGTH = 64;
				txt = content.getText().trim();
				if (txt.length() > MAX_LENGTH)
					txt = txt.substring(0, 64) + " ...";
				txt = " : " + txt;
			}
		}
		out.println(prefix + content.getName() + txt);
		for (QName key : content.keySet()) {
			out.println(prefix + " " + key + "=" + content.get(key));
		}
	}

//	public static <T> boolean isString(T t) {
//		return t instanceof String;
//	}

	public static final char SLASH = '/';
	public static final String SLASH_STRING = Character.toString(SLASH);
	public static final String EMPTY = "";

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
			return new String[] { EMPTY, path };

		return new String[] { parentIndex != 0 ? path.substring(0, parentIndex) : "" + SLASH,
				path.substring(parentIndex + 1) };
	}

	public static String toPath(List<String> segments) {
		// TODO checks
		StringJoiner sj = new StringJoiner("/");
		segments.forEach((s) -> sj.add(s));
		return sj.toString();
	}

	public static List<String> toPathSegments(String path) {
		List<String> res = new ArrayList<>();
		if (EMPTY.equals(path) || Content.ROOT_PATH.equals(path))
			return res;
		collectPathSegments(path, res);
		return res;
	}

	private static void collectPathSegments(String path, List<String> segments) {
		String[] parent = getParentPath(path);
		if (EMPTY.equals(parent[1])) // root
			return;
		segments.add(0, parent[1]);
		if (EMPTY.equals(parent[0])) // end
			return;
		collectPathSegments(parent[0], segments);
	}

	public static void checkDoubleSlash(String path) {
		if (path.contains(SLASH + "" + SLASH))
			throw new IllegalArgumentException("Path " + path + " contains //");
	}

	/** The last element of a path. */
	public static String lastPathElement(String path) {
		if (path.charAt(path.length() - 1) == '/')
			throw new IllegalArgumentException("Path " + path + " cannot end with '/'");
		int index = path.lastIndexOf('/');
		if (index < 0)
			return path;
		return path.substring(index + 1);
	}

	/*
	 * DIRECTORY
	 */

	public static Content roleToContent(CmsUserManager userManager, ContentSession contentSession, Role role) {
		UserDirectory userDirectory = userManager.getDirectory(role);
		String path = directoryPath(userDirectory) + userDirectory.getRolePath(role);
		Content content = contentSession.get(path);
		return content;
	}

	public static Content hierarchyUnitToContent(ContentSession contentSession, HierarchyUnit hierarchyUnit) {
		CmsDirectory directory = hierarchyUnit.getDirectory();
		StringJoiner relativePath = new StringJoiner(SLASH_STRING);
		buildHierarchyUnitPath(hierarchyUnit, relativePath);
		String path = directoryPath(directory) + relativePath.toString();
		Content content = contentSession.get(path);
		return content;
	}

	/** The path to this {@link CmsDirectory}. Ends with a /. */
	private static String directoryPath(CmsDirectory directory) {
		return CmsContentRepository.DIRECTORY_BASE + SLASH + directory.getName() + SLASH;
	}

	/** Recursively build a relative path of a {@link HierarchyUnit}. */
	private static void buildHierarchyUnitPath(HierarchyUnit current, StringJoiner relativePath) {
		if (current.getParent() == null) // directory
			return;
		buildHierarchyUnitPath(current.getParent(), relativePath);
		relativePath.add(current.getHierarchyUnitName());
	}

	/*
	 * CONSUMER UTILS
	 */

	public static Content createCollections(ContentSession session, String path) {
		if (session.exists(path)) {
			Content content = session.get(path);
			if (!content.isContentClass(DName.collection.qName())) {
				throw new IllegalStateException("Content " + path + " already exists, but is not a collection");
			} else {
				return content;
			}
		} else {
			String[] parentPath = getParentPath(path);
			Content parent = createCollections(session, parentPath[0]);
			Content content = parent.add(parentPath[1], DName.collection.qName());
			return content;
		}
	}

	public static ContentSession openDataAdminSession(ContentRepository contentRepository) {
		LoginContext loginContext;
		try {
			loginContext = CmsAuth.DATA_ADMIN.newLoginContext();
			loginContext.login();
		} catch (LoginException e1) {
			throw new RuntimeException("Could not login as data admin", e1);
		} finally {
		}

		ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(ContentUtils.class.getClassLoader());
			return CurrentSubject.callAs(loginContext.getSubject(), () -> contentRepository.get());
		} finally {
			Thread.currentThread().setContextClassLoader(currentCl);
		}
	}

	public static ContentSession openSession(ContentRepository contentRepository, CmsSession cmsSession) {
		return CurrentSubject.callAs(cmsSession.getSubject(), () -> contentRepository.get());
	}

	/**
	 * Constructs a relative path between a base path and a given path.
	 * 
	 * @throws IllegalArgumentException if the base path is not an ancestor of the
	 *                                  path
	 */
	public static String relativize(String basePath, String path) throws IllegalArgumentException {
		Objects.requireNonNull(basePath);
		Objects.requireNonNull(path);
		if (!path.startsWith(basePath))
			throw new IllegalArgumentException(basePath + " is not an ancestor of " + path);
		String relativePath = path.substring(basePath.length());
		if (relativePath.length() > 0 && relativePath.charAt(0) == '/')
			relativePath = relativePath.substring(1);
		return relativePath;
	}

	/** A path in the node repository */
	public static String getDataPath(Content node) {
		// TODO make it more configurable?
		StringBuilder buf = new StringBuilder(CmsConstants.PATH_API_ACR);
		buf.append(node.getPath());
		return buf.toString();
	}

	/** A path in the node repository */
	public static String getDataPathForUrl(Content node) {
		return cleanPathForUrl(getDataPath(node));
	}

	/** Clean reserved URL characters for use in HTTP links. */
	public static String cleanPathForUrl(String path) {
		StringTokenizer st = new StringTokenizer(path, "/");
		StringBuilder sb = new StringBuilder();
		while (st.hasMoreElements()) {
			sb.append('/');
			String encoded = URLEncoder.encode(st.nextToken(), StandardCharsets.UTF_8);
			encoded = encoded.replace("+", "%20");
			sb.append(encoded);

		}
		return sb.toString();
	}

	/** Singleton. */
	private ContentUtils() {

	}

}
