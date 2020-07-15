package org.argeo.cms.ui.useradmin;

import org.argeo.cms.ui.theme.CmsImages;
import org.eclipse.swt.graphics.Image;

/** Specific users icons. */
public class UsersImages {
	private final static String PREFIX = "icons/";

	public final static Image ICON_USER = CmsImages.createImg(PREFIX + "person.png");
	public final static Image ICON_GROUP = CmsImages.createImg(PREFIX + "group.png");
	public final static Image ICON_ROLE = CmsImages.createImg(PREFIX + "role.gif");
	public final static Image ICON_CHANGE_PASSWORD = CmsImages.createImg(PREFIX + "security.gif");
}
