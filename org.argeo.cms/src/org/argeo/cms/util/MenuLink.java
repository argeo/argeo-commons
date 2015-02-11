package org.argeo.cms.util;

import org.argeo.cms.CmsStyles;

/**
 * Convenience class setting the custom style {@link CmsStyles#CMS_MENU_LINK} on
 * a {@link CmsLink} when simple menus are used.
 */
public class MenuLink extends CmsLink {
	public MenuLink() {
		setCustom(CmsStyles.CMS_MENU_LINK);
	}
}
