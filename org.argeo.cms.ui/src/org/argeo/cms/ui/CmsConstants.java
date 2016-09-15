package org.argeo.cms.ui;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.graphics.Point;

/** Commons constants */
public interface CmsConstants {
	// DATAKEYS
	public final static String STYLE = RWT.CUSTOM_VARIANT;
	public final static String MARKUP = RWT.MARKUP_ENABLED;
	public final static String ITEM_HEIGHT = RWT.CUSTOM_ITEM_HEIGHT;

	// EVENT DETAILS
	public final static int HYPERLINK = RWT.HYPERLINK;

	// STANDARD RESOURCES
	public final static String LOADING_IMAGE = "icons/loading.gif";

	public final static String NO_IMAGE = "icons/noPic-square-640px.png";
	public final static Point NO_IMAGE_SIZE = new Point(640, 640);
	public final static Float NO_IMAGE_RATIO = 1f;
}
