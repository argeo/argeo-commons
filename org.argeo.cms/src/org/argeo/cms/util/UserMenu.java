package org.argeo.cms.util;

import java.io.IOException;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.argeo.ArgeoException;
import org.argeo.cms.CmsMsg;
import org.argeo.cms.CmsSession;
import org.argeo.cms.CmsStyles;
import org.argeo.cms.KernelHeader;
import org.argeo.cms.auth.ArgeoLoginContext;
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
import org.springframework.security.core.context.SecurityContextHolder;

/** The site-related user menu */
public class UserMenu extends Shell implements CmsStyles, CallbackHandler {
	private static final long serialVersionUID = -5788157651532106301L;
	private Text username, password;

	public UserMenu(Control source) {
		super(source.getDisplay(), SWT.NO_TRIM | SWT.BORDER | SWT.ON_TOP);
		setData(RWT.CUSTOM_VARIANT, CMS_USER_MENU);

		String username = SecurityContextHolder.getContext()
				.getAuthentication().getName();
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

		String username = SecurityContextHolder.getContext()
				.getAuthentication().getName();

		Label l = new Label(this, SWT.NONE);
		l.setData(RWT.CUSTOM_VARIANT, CMS_USER_MENU_ITEM);
		l.setData(RWT.MARKUP_ENABLED, true);
		l.setLayoutData(CmsUtils.fillWidth());
		l.setText("<b>" + username + "</b>");

		l = new Label(this, SWT.NONE);
		l.setData(RWT.CUSTOM_VARIANT, CMS_USER_MENU_ITEM);
		l.setText(CmsMsg.logout.lead());
		GridData lData = CmsUtils.fillWidth();
		lData.widthHint = 120;
		l.setLayoutData(lData);

		l.addMouseListener(new MouseAdapter() {
			private static final long serialVersionUID = 6444395812777413116L;

			public void mouseDown(MouseEvent e) {
				logout();
			}
		});
	}

	protected void anonymousUi() {
		Integer textWidth = 150;
		setData(RWT.CUSTOM_VARIANT, CMS_USER_MENU);
		setLayout(new GridLayout(2, false));

		new Label(this, SWT.NONE).setText(CmsMsg.username.lead());
		username = new Text(this, SWT.BORDER);
		username.setData(RWT.CUSTOM_VARIANT, CMS_LOGIN_DIALOG_USERNAME);
		GridData gd = CmsUtils.fillWidth();
		gd.widthHint = textWidth;
		username.setLayoutData(gd);

		new Label(this, SWT.NONE).setText(CmsMsg.password.lead());
		password = new Text(this, SWT.BORDER | SWT.PASSWORD);
		password.setData(RWT.CUSTOM_VARIANT, CMS_LOGIN_DIALOG_PASSWORD);
		gd = CmsUtils.fillWidth();
		gd.widthHint = textWidth;
		password.setLayoutData(gd);

		TraverseListener tl = new TraverseListener() {
			private static final long serialVersionUID = -1158892811534971856L;

			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_RETURN)
					login();
			}
		};
		username.addTraverseListener(tl);
		password.addTraverseListener(tl);
	}

	protected void login() {
		CmsSession cmsSession = (CmsSession) getDisplay().getData(
				CmsSession.KEY);
		Subject subject = new Subject();
		try {
			//
			// LOGIN
			//
			new ArgeoLoginContext(KernelHeader.LOGIN_CONTEXT_ANONYMOUS, subject)
					.logout();
			LoginContext loginContext = new ArgeoLoginContext(
					KernelHeader.LOGIN_CONTEXT_USER, subject, this);
			loginContext.login();
		} catch (LoginException e1) {
			throw new ArgeoException("Cannot authenticate anonymous", e1);
		}
		close();
		dispose();
		cmsSession.authChange();
	}

	protected void logout() {
		final CmsSession cmsSession = (CmsSession) getDisplay().getData(
				CmsSession.KEY);
		Subject subject = new Subject();
		try {
			//
			// LOGOUT
			//
			new ArgeoLoginContext(KernelHeader.LOGIN_CONTEXT_USER, subject)
					.logout();
			new ArgeoLoginContext(KernelHeader.LOGIN_CONTEXT_ANONYMOUS, subject)
					.login();
		} catch (LoginException e1) {
			throw new ArgeoException("Cannot authenticate anonymous", e1);
		}
		close();
		dispose();
		cmsSession.authChange();
	}

	@Override
	public void handle(Callback[] callbacks) throws IOException,
			UnsupportedCallbackException {
		((NameCallback) callbacks[0]).setName(username.getText());
		((PasswordCallback) callbacks[1]).setPassword(password.getTextChars());
	}

}
