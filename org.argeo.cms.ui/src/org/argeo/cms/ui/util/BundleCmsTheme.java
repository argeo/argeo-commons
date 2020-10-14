package org.argeo.cms.ui.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Simplifies the theming of an app (only RAP is supported at this stage).<br>
 * 
 * Additional fonts listed in <code>/fonts.txt</code>.<br>
 * Additional (standard CSS) header in <code>/header.css</code>.<br>
 * RAP specific CSS files in <code>/rap/*.css</code>.<br>
 * All images added as additional resources based on extensions
 * <code>/ ** /*.{png,gif,jpeg,...}</code>.<br>
 */
public class BundleCmsTheme extends AbstractCmsTheme {
	public final static String DEFAULT_CMS_THEME_BUNDLE = "org.argeo.theme.argeo2";

	public final static String CMS_THEME_PROPERTY = "argeo.cms.theme";
	public final static String CMS_THEME_BUNDLE_PROPERTY = "argeo.cms.theme.bundle";

//	private final static Log log = LogFactory.getLog(BundleCmsTheme.class);

	private String themeId;
	private Set<String> webCssPaths = new TreeSet<>();
	private Set<String> rapCssPaths = new TreeSet<>();
	private Set<String> swtCssPaths = new TreeSet<>();
	private Set<String> imagesPaths = new TreeSet<>();

	private String headerCss;
	private List<String> fonts = new ArrayList<>();

	private String basePath;
	private String styleCssPath;
//	private String webCssPath;
//	private String rapCssPath;
//	private String swtCssPath;
	private Bundle themeBundle;

	public BundleCmsTheme() {

	}

	public void init(BundleContext bundleContext, Map<String, String> properties) {
		initResources(bundleContext, null);
	}

	public void destroy(BundleContext bundleContext, Map<String, String> properties) {

	}

	@Deprecated
	public BundleCmsTheme(BundleContext bundleContext) {
		this(bundleContext, null);
	}

	@Deprecated
	public BundleCmsTheme(BundleContext bundleContext, String symbolicName) {
		initResources(bundleContext, symbolicName);
	}

	private void initResources(BundleContext bundleContext, String symbolicName) {
		if (symbolicName == null) {
			themeBundle = bundleContext.getBundle();
//			basePath = "/theme/";
//			cssPath = basePath;
		} else {
			themeBundle = findThemeBundle(bundleContext, symbolicName);
		}
		basePath = "/";
		styleCssPath = "/style/";
//		webCssPath = "/css/";
//		rapCssPath = "/rap/";
//		swtCssPath = "/swt/";
//		this.themeId = RWT.DEFAULT_THEME_ID;
		this.themeId = themeBundle.getSymbolicName();
		webCssPaths = addCss(themeBundle, "/css/");
		rapCssPaths = addCss(themeBundle, "/rap/");
		swtCssPaths = addCss(themeBundle, "/swt/");
		addResources("*.png");
		addResources("*.gif");
		addResources("*.jpg");
		addResources("*.jpeg");
		addResources("*.svg");
		addResources("*.ico");

		// fonts
		URL fontsUrl = themeBundle.getEntry(basePath + "fonts.txt");
		if (fontsUrl != null) {
			loadFontsUrl(fontsUrl);
		}

		// common CSS header (plain CSS)
		URL headerCssUrl = themeBundle.getEntry(basePath + "header.css");
		if (headerCssUrl != null) {
			try (BufferedReader buffer = new BufferedReader(new InputStreamReader(headerCssUrl.openStream(), UTF_8))) {
				headerCss = buffer.lines().collect(Collectors.joining("\n"));
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot read " + headerCssUrl, e);
			}
		}
	}

	public String getHtmlHeaders() {
		StringBuilder sb = new StringBuilder();
		if (headerCss != null) {
			sb.append("<style type='text/css'>\n");
			sb.append(headerCss);
			sb.append("\n</style>\n");
		}
		for (String link : fonts) {
			sb.append("<link rel='stylesheet' href='");
			sb.append(link);
			sb.append("'/>\n");
		}
		if (sb.length() == 0)
			return null;
		else
			return sb.toString();
	}

	Set<String> addCss(Bundle themeBundle, String path) {
		Set<String> paths = new TreeSet<>();
		Enumeration<URL> themeResources = themeBundle.findEntries(path, "*.css", true);
		if (themeResources == null)
			return paths;
		while (themeResources.hasMoreElements()) {
			String resource = themeResources.nextElement().getPath();
			// remove first '/' so that RWT registers it
			resource = resource.substring(1);
			if (!resource.endsWith("/")) {
				paths.add(resource);
			}
		}

		// common CSS
		Enumeration<URL> commonResources = themeBundle.findEntries(styleCssPath, "*.css", true);
		if (commonResources == null)
			return paths;
		while (commonResources.hasMoreElements()) {
			String resource = commonResources.nextElement().getPath();
			// remove first '/' so that RWT registers it
			resource = resource.substring(1);
			if (!resource.endsWith("/")) {
				paths.add(resource);
			}
		}
		return paths;
	}

	void loadFontsUrl(URL url) {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), UTF_8))) {
			String line = null;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (!line.equals("") && !line.startsWith("#")) {
					fonts.add(line);
				}
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("Cannot load URL " + url, e);
		}
	}

	void addResources(String pattern) {
		Enumeration<URL> themeResources = themeBundle.findEntries(basePath, pattern, true);
		if (themeResources == null)
			return;
		while (themeResources.hasMoreElements()) {
			String resource = themeResources.nextElement().getPath();
			// remove first '/' so that RWT registers it
			resource = resource.substring(1);
			if (!resource.endsWith("/")) {
//				if (resources.containsKey(resource))
//					log.warn("Overriding " + resource + " from " + themeBundle.getSymbolicName());
//				resources.put(resource, themeBRL);
				imagesPaths.add(resource);
			}

		}

	}

	@Override
	public InputStream getResourceAsStream(String resourceName) throws IOException {
		URL res = themeBundle.getEntry(resourceName);
		if (res == null) {
			res = themeBundle.getResource(resourceName);
			if (res == null)
				return null;
//				throw new IllegalArgumentException(
//						"Resource " + resourceName + " not found in bundle " + themeBundle.getSymbolicName());
		}
		return res.openStream();
	}

	public String getThemeId() {
		return themeId;
	}

//	public void setThemeId(String themeId) {
//		this.themeId = themeId;
//	}
//
//	public String getBasePath() {
//		return basePath;
//	}
//
//	public void setBasePath(String basePath) {
//		this.basePath = basePath;
//	}
//
//	public String getRapCssPath() {
//		return rapCssPath;
//	}
//
//	public void setRapCssPath(String cssPath) {
//		this.rapCssPath = cssPath;
//	}

	@Override
	public Set<String> getWebCssPaths() {
		return webCssPaths;
	}

	@Override
	public Set<String> getRapCssPaths() {
		return rapCssPaths;
	}

	@Override
	public Set<String> getSwtCssPaths() {
		return swtCssPaths;
	}

	@Override
	public Set<String> getImagesPaths() {
		return imagesPaths;
	}

	@Override
	public InputStream loadPath(String path) throws IOException {
		URL url = themeBundle.getResource(path);
		if (url == null)
			throw new IllegalArgumentException(
					"Path " + path + " not found in bundle " + themeBundle.getSymbolicName());
		return url.openStream();
	}

	private static Bundle findThemeBundle(BundleContext bundleContext, String themeId) {
		if (themeId == null)
			return null;
		// TODO optimize
		// TODO deal with multiple versions
		Bundle themeBundle = null;
		if (themeId != null) {
			for (Bundle bundle : bundleContext.getBundles())
				if (themeId.equals(bundle.getSymbolicName())) {
					themeBundle = bundle;
					break;
				}
		}
		return themeBundle;
	}

}
