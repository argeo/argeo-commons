package org.argeo.cms;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.springframework.security.context.SecurityContextHolder;

/** The site-related user menu */
public class UserMenu extends Shell implements CmsStyles {
	private static final long serialVersionUID = -5788157651532106301L;

	private CmsLogin cmsLogin;
	private String username = null;

	public UserMenu(CmsLogin cmsLogin, Control source) {
		super(source.getDisplay(), SWT.NO_TRIM | SWT.BORDER | SWT.ON_TOP);
		this.cmsLogin = cmsLogin;

		setData(RWT.CUSTOM_VARIANT, CMS_USER_MENU);

		username = SecurityContextHolder.getContext().getAuthentication()
				.getName();
		if (username.equals("anonymous")) {
			username = null;
			anonymousUi();
		} else {
			userUi();
		}

		pack();
		layout();
		setLocation(source.toDisplay(source.getSize().x - getSize().x,
				source.getSize().y));

		addShellListener(new ShellAdapter() {
			private static final long serialVersionUID = 5178980294808435833L;

			@Override
			public void shellDeactivated(ShellEvent e) {
				close();
				dispose();
			}

		});

		open();

	}

	protected void userUi() {
		setLayout(new GridLayout());

		Label l = new Label(this, SWT.NONE);
		l.setData(RWT.CUSTOM_VARIANT, CMS_USER_MENU_ITEM);
		l.setData(RWT.MARKUP_ENABLED, true);
		l.setLayoutData(CmsUtils.fillWidth());
		l.setText("<b>" + username + "</b>");

		final CmsSession cmsSession = (CmsSession) getDisplay().getData(
				CmsSession.KEY);
		l = new Label(this, SWT.NONE);
		l.setData(RWT.CUSTOM_VARIANT, CMS_USER_MENU_ITEM);
		l.setText(CmsMsg.logout.lead());
		GridData lData = CmsUtils.fillWidth();
		lData.widthHint = 120;
		l.setLayoutData(lData);

		l.addMouseListener(new MouseAdapter() {
			private static final long serialVersionUID = 6444395812777413116L;

			public void mouseDown(MouseEvent e) {
				SecurityContextHolder.getContext().setAuthentication(null);
				close();
				dispose();
				cmsSession.authChange();
			}
		});
	}

	protected void anonymousUi() {
		Integer textWidth = 150;
		setData(RWT.CUSTOM_VARIANT, CMS_USER_MENU);
		setLayout(new GridLayout(2, false));

		new Label(this, SWT.NONE).setText(CmsMsg.username.lead());
		final Text username = new Text(this, SWT.BORDER);
		username.setData(RWT.CUSTOM_VARIANT, CMS_LOGIN_DIALOG_USERNAME);
		GridData gd = CmsUtils.fillWidth();
		gd.widthHint = textWidth;
		username.setLayoutData(gd);

		new Label(this, SWT.NONE).setText(CmsMsg.password.lead());
		final Text password = new Text(this, SWT.BORDER | SWT.PASSWORD);
		password.setData(RWT.CUSTOM_VARIANT, CMS_LOGIN_DIALOG_PASSWORD);
		gd = CmsUtils.fillWidth();
		gd.widthHint = textWidth;
		password.setLayoutData(gd);

		// Listeners
		TraverseListener tl = new TraverseListener() {
			private static final long serialVersionUID = -1158892811534971856L;

			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_RETURN)
					login(username.getText(), password.getTextChars());
			}
		};
		username.addTraverseListener(tl);
		password.addTraverseListener(tl);
	}

	protected void login(String username, char[] password) {
		CmsSession cmsSession = (CmsSession) getDisplay().getData(
				CmsSession.KEY);
		cmsLogin.logInWithPassword(username, password);
		close();
		dispose();
		// refreshUi(source.getParent());
		cmsSession.authChange();
	}

}
