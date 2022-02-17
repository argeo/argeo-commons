package org.argeo.cms.osgi;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.argeo.api.cms.CmsTheme;
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
public class BundleCmsTheme implements CmsTheme {
	public final static String DEFAULT_CMS_THEME_BUNDLE = "org.argeo.theme.argeo2";

	public final static String CMS_THEME_PROPERTY = "argeo.cms.theme";
	public final static String CMS_THEME_BUNDLE_PROPERTY = "argeo.cms.theme.bundle";

	private final static String HEADER_CSS = "header.css";
	private final static String FONTS_TXT = "fonts.txt";
	private final static String BODY_HTML = "body.html";

//	private final static Log log = LogFactory.getLog(BundleCmsTheme.class);

	private CmsTheme parentTheme;

	private String themeId;
	private Set<String> webCssPaths = new TreeSet<>();
	private Set<String> rapCssPaths = new TreeSet<>();
	private Set<String> swtCssPaths = new TreeSet<>();
	private Set<String> imagesPaths = new TreeSet<>();
	private Set<String> fontsPaths = new TreeSet<>();

	private String headerCss;
	private List<String> fonts = new ArrayList<>();

	private String bodyHtml = "<body></body>";

	private String basePath;
	private String styleCssPath;
//	private String webCssPath;
//	private String rapCssPath;
//	private String swtCssPath;
	private Bundle themeBundle;

	private Integer defaultIconSize = 16;

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
		addImages("*.png");
		addImages("*.gif");
		addImages("*.jpg");
		addImages("*.jpeg");
		addImages("*.svg");
		addImages("*.ico");

		addFonts("*.woff");
		addFonts("*.woff2");

		// fonts
		URL fontsUrl = themeBundle.getEntry(basePath + FONTS_TXT);
		if (fontsUrl != null) {
			loadFontsUrl(fontsUrl);
		}

		// common CSS header (plain CSS)
		URL headerCssUrl = themeBundle.getEntry(basePath + HEADER_CSS);
		if (headerCssUrl != null) {
			// added to plain Web CSS
			webCssPaths.add(basePath + HEADER_CSS);
			// and it will also be used by RAP:
			try (BufferedReader buffer = new BufferedReader(new InputStreamReader(headerCssUrl.openStream(), UTF_8))) {
				headerCss = buffer.lines().collect(Collectors.joining("\n"));
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot read " + headerCssUrl, e);
			}
		}

		// body
		URL bodyUrl = themeBundle.getEntry(basePath + BODY_HTML);
		if (bodyUrl != null) {
			loadBodyHtml(bodyUrl);
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

	@Override
	public String getBodyHtml() {
		return bodyHtml;
	}

	Set<String> addCss(Bundle themeBundle, String path) {
		Set<String> paths = new TreeSet<>();

		// common CSS
		Enumeration<URL> commonResources = themeBundle.findEntries(styleCssPath, "*.css", true);
		if (commonResources != null) {
			while (commonResources.hasMoreElements()) {
				String resource = commonResources.nextElement().getPath();
				// remove first '/' so that RWT registers it
				resource = resource.substring(1);
				if (!resource.endsWith("/")) {
					paths.add(resource);
				}
			}
		}

		// specific CSS
		Enumeration<URL> themeResources = themeBundle.findEntries(path, "*.css", true);
		if (themeResources != null) {
			while (themeResources.hasMoreElements()) {
				String resource = themeResources.nextElement().getPath();
				// remove first '/' so that RWT registers it
				resource = resource.substring(1);
				if (!resource.endsWith("/")) {
					paths.add(resource);
				}
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

	void loadBodyHtml(URL url) {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), UTF_8))) {
			bodyHtml = IOUtils.toString(url, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalArgumentException("Cannot load URL " + url, e);
		}
	}

	void addImages(String pattern) {
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

	void addFonts(String pattern) {
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
				fontsPaths.add(resource);
			}

		}

	}

	@Override
	public InputStream getResourceAsStream(String resourceName) throws IOException {
		URL res = themeBundle.getEntry(resourceName);
		if (res == null) {
			res = themeBundle.getResource(resourceName);
			if (res == null) {
				if (parentTheme == null)
					return null;
				return parentTheme.getResourceAsStream(resourceName);
			}
		}
		return res.openStream();
	}

	public String getThemeId() {
		return themeId;
	}

	@Override
	public Set<String> getWebCssPaths() {
		if (parentTheme != null) {
			Set<String> res = new HashSet<>(parentTheme.getWebCssPaths());
			res.addAll(webCssPaths);
			return res;
		}
		return webCssPaths;
	}

	@Override
	public Set<String> getRapCssPaths() {
		if (parentTheme != null) {
			Set<String> res = new HashSet<>(parentTheme.getRapCssPaths());
			res.addAll(rapCssPaths);
			return res;
		}
		return rapCssPaths;
	}

	@Override
	public Set<String> getSwtCssPaths() {
		if (parentTheme != null) {
			Set<String> res = new HashSet<>(parentTheme.getSwtCssPaths());
			res.addAll(swtCssPaths);
			return res;
		}
		return swtCssPaths;
	}

	@Override
	public Set<String> getImagesPaths() {
		if (parentTheme != null) {
			Set<String> res = new HashSet<>(parentTheme.getImagesPaths());
			res.addAll(imagesPaths);
			return res;
		}
		return imagesPaths;
	}

	@Override
	public Set<String> getFontsPaths() {
		return fontsPaths;
	}

	@Override
	public Integer getDefaultIconSize() {
		return defaultIconSize;
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

	@Override
	public int hashCode() {
		return themeId.hashCode();
	}

	@Override
	public String toString() {
		return "Bundle CMS Theme " + themeId;
	}

	public void setParentTheme(CmsTheme parentTheme) {
		this.parentTheme = parentTheme;
	}

}
