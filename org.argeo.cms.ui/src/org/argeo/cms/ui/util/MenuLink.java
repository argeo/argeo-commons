package org.argeo.cms.ui.util;

import org.argeo.cms.ui.CmsStyles;

/**
 * Convenience class setting the custom style {@link CmsStyles#CMS_MENU_LINK} on
 * a {@link CmsLink} when simple menus are used.
 */
public class MenuLink extends CmsLink {
	public MenuLink() {
		setCustom(CmsStyles.CMS_MENU_LINK);
	}

	public MenuLink(String label, String target, String custom) {
		super(label, target, custom);
	}

	public MenuLink(String label, String target) {
		super(label, target, CmsStyles.CMS_MENU_LINK);
	}

}
