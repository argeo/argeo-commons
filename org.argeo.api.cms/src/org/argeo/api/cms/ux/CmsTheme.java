package org.argeo.api.cms.ux;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/** A CMS theme which can be applied to web apps as well as desktop apps. */
public interface CmsTheme {
	/** Unique ID of this theme. */
	String getThemeId();

	/**
	 * Load a resource as an input stream, base don its relative path, or
	 * <code>null</code> if not found
	 */
	InputStream getResourceAsStream(String resourceName) throws IOException;

	/** Relative paths to standard web CSS. */
	Set<String> getWebCssPaths();

	/** Relative paths to RAP specific CSS. */
	Set<String> getRapCssPaths();

	/** Relative paths to SWT specific CSS. */
	Set<String> getSwtCssPaths();

	/** Relative paths to images such as icons. */
	Set<String> getImagesPaths();

	/** Relative paths to fonts. */
	Set<String> getFontsPaths();

	/** Tags to be added to the header section of the HTML page. */
	String getHtmlHeaders();

	/** The HTML body to use. */
	String getBodyHtml();

	/** The default icon size (typically the smallest). */
	default int getDefaultIconSize() {
		return getSmallIconSize();
	}

	int getSmallIconSize();

	int getBigIconSize();

	/** Loads one of the relative path provided by the other methods. */
	InputStream loadPath(String path) throws IOException;

}
