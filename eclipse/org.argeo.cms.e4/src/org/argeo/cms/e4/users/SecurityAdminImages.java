package org.argeo.cms.e4.users;

import org.argeo.cms.ui.theme.CmsImages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

/** Shared icons that must be declared programmatically . */
public class SecurityAdminImages extends CmsImages {
	private final static String PREFIX = "icons/";

	public final static ImageDescriptor ICON_REMOVE_DESC = createDesc(PREFIX + "delete.png");
	public final static ImageDescriptor ICON_USER_DESC = createDesc(PREFIX + "person.png");

	public final static Image ICON_USER = ICON_USER_DESC.createImage();
	public final static Image ICON_GROUP = createImg(PREFIX + "group.png");
	public final static Image ICON_WORKGROUP = createImg(PREFIX + "workgroup.png");
	public final static Image ICON_ROLE = createImg(PREFIX + "role.gif");

}
