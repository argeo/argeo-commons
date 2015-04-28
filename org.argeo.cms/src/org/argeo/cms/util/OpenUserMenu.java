package org.argeo.cms.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsLogin;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Control;

/** Open the user menu when clicked */
@SuppressWarnings("deprecation")
public class OpenUserMenu extends MouseAdapter {
	private static final long serialVersionUID = 3634864186295639792L;

	private final static Log log = LogFactory.getLog(OpenUserMenu.class);

	// private CmsLogin cmsLogin;

	@Override
	public void mouseDown(MouseEvent e) {
		if (e.button == 1) {
			new UserMenu((Control) e.getSource());
		}
	}

	public void setCmsLogin(CmsLogin cmsLogin) {
		log.warn("org.argeo.cms.CmsLogin is deprecated and will be removed soon");
	}

}