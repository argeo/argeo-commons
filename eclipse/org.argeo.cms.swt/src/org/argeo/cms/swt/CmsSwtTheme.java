package org.argeo.cms.swt;

import org.argeo.api.cms.CmsTheme;
import org.eclipse.swt.graphics.Image;

/** SWT specific {@link CmsTheme}. */
public interface CmsSwtTheme extends CmsTheme {
//	/** The image registered at this path, or <code>null</code> if not found. */
//	Image getImage(String path);

	/**
	 * And icon with this file name (without the extension), with a best effort to
	 * find the appropriate size, or <code>null</code> if not found.
	 * 
	 * @param name          An icon file name without path and extension.
	 * @param preferredSize the preferred size, if <code>null</code>,
	 *                      {@link #getDefaultIconSize()} will be tried.
	 */
	Image getIcon(String name, Integer preferredSize);

}
