package org.argeo.cms.ui;

import org.argeo.api.cms.Cms2DSize;

/** Commons constants */
@Deprecated
public interface CmsConstants {
	// DATAKEYS
//	public final static String STYLE = EclipseUiConstants.CSS_CLASS;
//	public final static String MARKUP = EclipseUiConstants.MARKUP_SUPPORT;
	@Deprecated
	/* RWT.CUSTOM_ITEM_HEIGHT */
	public final static String ITEM_HEIGHT = "org.eclipse.rap.rwt.customItemHeight";

	// EVENT DETAILS
	@Deprecated
	/* RWT.HYPERLINK */
	public final static int HYPERLINK = 1 << 26;

	// STANDARD RESOURCES
	public final static String LOADING_IMAGE = "icons/loading.gif";

	public final static String NO_IMAGE = "icons/noPic-square-640px.png";
	public final static Cms2DSize NO_IMAGE_SIZE = new Cms2DSize(320, 320);
	public final static Float NO_IMAGE_RATIO = 1f;
	// MISCEALLENEOUS
	String DATE_TIME_FORMAT = "dd/MM/yyyy, HH:mm";
}
