package org.argeo.cms;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.graphics.Point;

/** Commons constants */
public interface CmsConstants {
	// DATAKEYS
	public static final String STYLE = RWT.CUSTOM_VARIANT;
	public static final String MARKUP = RWT.MARKUP_ENABLED;

	// STANDARD RESOURCES
	public static final String LOADING_IMAGE = "icons/loading.gif";

	public static final String NO_IMAGE = "icons/noPic-square-640px.png";
	public static final Point NO_IMAGE_SIZE = new Point(640, 640);
	public static final Float NO_IMAGE_RATIO = 1f;
}
