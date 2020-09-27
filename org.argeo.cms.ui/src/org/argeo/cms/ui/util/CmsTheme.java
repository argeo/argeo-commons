package org.argeo.cms.ui.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.Application;
import org.eclipse.rap.rwt.service.ResourceLoader;
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
public class CmsTheme {
	public final static String DEFAULT_CMS_THEME_BUNDLE = "org.argeo.theme.argeo2";

	public final static String CMS_THEME_BUNDLE_PROPERTY = "argeo.cms.theme.bundle";

	private final static Log log = LogFactory.getLog(CmsTheme.class);

	private String themeId;
	private Map<String, ResourceLoader> css = new HashMap<>();
	private Map<String, ResourceLoader> resources = new HashMap<>();

	private String headerCss;
	private List<String> fonts = new ArrayList<>();

	private String basePath;
	private String cssPath;
	private final Bundle themeBundle;

	public CmsTheme(BundleContext bundleContext) {
		this(bundleContext, null);
	}

	public CmsTheme(BundleContext bundleContext, String symbolicName) {
		if (symbolicName == null) {
			themeBundle = bundleContext.getBundle();
//			basePath = "/theme/";
//			cssPath = basePath;
		} else {
			themeBundle = ThemeUtils.findThemeBundle(bundleContext, symbolicName);
		}
		basePath = "/";
		cssPath = "/rap/";
		this.themeId = RWT.DEFAULT_THEME_ID;
		addStyleSheets(themeBundle, new BundleResourceLoader(themeBundle));
		BundleResourceLoader themeBRL = new BundleResourceLoader(themeBundle);
		addResources(themeBRL, "*.png");
		addResources(themeBRL, "*.gif");
		addResources(themeBRL, "*.jpg");
		addResources(themeBRL, "*.jpeg");
		addResources(themeBRL, "*.svg");
		addResources(themeBRL, "*.ico");

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
				throw new CmsException("Cannot read " + headerCssUrl, e);
			}
		}

	}

	public void apply(Application application) {
		resources: for (String name : resources.keySet()) {
			if (name.startsWith("target/"))
				continue resources; // skip maven output
			application.addResource(name, resources.get(name));
			if (log.isTraceEnabled())
				log.trace("Theme " + themeBundle + ": added resource " + name);
		}
		for (String name : css.keySet()) {
			application.addStyleSheet(themeId, name, css.get(name));
			if (log.isDebugEnabled())
				log.debug("Theme " + themeBundle + ": added RAP CSS " + name);
		}
	}

	public String getAdditionalHeaders() {
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

	void addStyleSheets(Bundle themeBundle, ResourceLoader ssRL) {
		Enumeration<URL> themeResources = themeBundle.findEntries(cssPath, "*.css", true);
		if (themeResources == null)
			return;
		while (themeResources.hasMoreElements()) {
			String resource = themeResources.nextElement().getPath();
			// remove first '/' so that RWT registers it
			resource = resource.substring(1);
			if (!resource.endsWith("/")) {
				if (css.containsKey(resource))
					log.warn("Overriding " + resource + " from " + themeBundle.getSymbolicName());
				css.put(resource, ssRL);
			}

		}

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
			throw new CmsException("Cannot load URL " + url, e);
		}
	}

	void addResources(BundleResourceLoader themeBRL, String pattern) {
		Bundle themeBundle = themeBRL.getBundle();
		Enumeration<URL> themeResources = themeBundle.findEntries(basePath, pattern, true);
		if (themeResources == null)
			return;
		while (themeResources.hasMoreElements()) {
			String resource = themeResources.nextElement().getPath();
			// remove first '/' so that RWT registers it
			resource = resource.substring(1);
			if (!resource.endsWith("/")) {
				if (resources.containsKey(resource))
					log.warn("Overriding " + resource + " from " + themeBundle.getSymbolicName());
				resources.put(resource, themeBRL);
			}

		}

	}

	public String getThemeId() {
		return themeId;
	}

	public void setThemeId(String themeId) {
		this.themeId = themeId;
	}

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public String getCssPath() {
		return cssPath;
	}

	public void setCssPath(String cssPath) {
		this.cssPath = cssPath;
	}

}
