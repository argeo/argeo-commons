package org.argeo.cms;

import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Control;

/** Open the user menu when clicked */
public class OpenUserMenu extends MouseAdapter {
	private static final long serialVersionUID = 3634864186295639792L;
	private CmsLogin cmsLogin;

	@Override
	public void mouseDown(MouseEvent e) {
		if (e.button == 1) {
			new UserMenu(cmsLogin, (Control) e.getSource());
		}
	}

	public void setCmsLogin(CmsLogin cmsLogin) {
		this.cmsLogin = cmsLogin;
	}

}