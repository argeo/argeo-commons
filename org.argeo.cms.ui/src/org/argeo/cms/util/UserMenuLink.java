package org.argeo.cms.util;

import javax.jcr.Node;

import org.argeo.cms.CmsMsg;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.ui.CmsStyles;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/** Open the user menu when clicked */
public class UserMenuLink extends MenuLink {

	public UserMenuLink() {
		setCustom(CmsStyles.CMS_USER_MENU_LINK);
	}

	@Override
	public Control createUi(Composite parent, Node context) {
		if (CurrentUser.isAnonymous())
			setLabel(CmsMsg.login.lead());
		else {
			setLabel(CurrentUser.getDisplayName());
		}
		Label link = (Label) ((Composite) super.createUi(parent, context)).getChildren()[0];
		link.addMouseListener(new UserMenuLinkController());
		return link.getParent();
	}

	protected UserMenu createUserMenu(Control source) {
		return new UserMenu(source.getParent());
	}

	private class UserMenuLinkController implements MouseListener, DisposeListener {
		private static final long serialVersionUID = 3634864186295639792L;

		private UserMenu userMenu = null;
		private long lastDisposeTS = 0l;

		//
		// MOUSE LISTENER
		//
		@Override
		public void mouseDown(MouseEvent e) {
			if (e.button == 1) {
				Control source = (Control) e.getSource();
				if (userMenu == null) {
					long durationSinceLastDispose = System.currentTimeMillis() - lastDisposeTS;
					// avoid to reopen the menu, if one has clicked gain
					if (durationSinceLastDispose > 200) {
						userMenu = createUserMenu(source);
						userMenu.getShell().addDisposeListener(this);
					}
				}
			}
		}

		@Override
		public void mouseDoubleClick(MouseEvent e) {
		}

		@Override
		public void mouseUp(MouseEvent e) {
		}

		@Override
		public void widgetDisposed(DisposeEvent event) {
			userMenu = null;
			lastDisposeTS = System.currentTimeMillis();
		}
	}
}
