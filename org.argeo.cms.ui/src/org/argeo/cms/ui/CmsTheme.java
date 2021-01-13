package org.argeo.cms.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

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

	/** The image registered at this path, or <code>null</code> if not found. */
	Image getImage(String path);

	/** The default icon size (typically the smallest). */
	Integer getDefaultIconSize();

	/** Loads one of the relative path provided by the other methods. */
	InputStream loadPath(String path) throws IOException;

	/**
	 * And icon with this file name (without the extension), with a best effort to
	 * find the appropriate size, or <code>null</code> if not found.
	 * 
	 * @param name          An icon file name without path and extension.
	 * @param preferredSize the preferred size, if <code>null</code>,
	 *                      {@link #getDefaultIconSize()} will be tried.
	 */
	Image getIcon(String name, Integer preferredSize);

	static CmsTheme getCmsTheme(Composite parent) {
		CmsTheme theme = (CmsTheme) parent.getData(CmsTheme.class.getName());
		if (theme == null) {
			// find parent shell
			Shell topShell = parent.getShell();
			while (topShell.getParent() != null)
				topShell = (Shell) topShell.getParent();
			theme = (CmsTheme) topShell.getData(CmsTheme.class.getName());
			parent.setData(CmsTheme.class.getName(), theme);
		}
		return theme;
	}

	static void registerCmsTheme(Shell shell, CmsTheme theme) {
		// find parent shell
		Shell topShell = shell;
		while (topShell.getParent() != null)
			topShell = (Shell) topShell.getParent();
		// check if already set
		if (topShell.getData(CmsTheme.class.getName()) != null) {
			CmsTheme registeredTheme = (CmsTheme) topShell.getData(CmsTheme.class.getName());
			throw new IllegalArgumentException(
					"Theme " + registeredTheme.getThemeId() + " already registered in this shell");
		}
		topShell.setData(CmsTheme.class.getName(), theme);
	}

}
