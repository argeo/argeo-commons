package org.argeo.cms;

/**
 * Convenience class setting the custom style {@link CmsStyles#CMS_MENU_LINK} on
 * a {@link CmsLink} when simple menus are used.
 */
public class MenuLink extends CmsLink {
	public MenuLink() {
		setCustom(CmsStyles.CMS_MENU_LINK);
	}
}
