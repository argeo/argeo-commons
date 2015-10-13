package org.argeo.cms.util;

import static org.argeo.cms.auth.AuthConstants.LOGIN_CONTEXT_ANONYMOUS;
import static org.argeo.cms.auth.AuthConstants.LOGIN_CONTEXT_USER;

import java.io.IOException;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.argeo.cms.CmsException;
import org.argeo.cms.CmsMsg;
import org.argeo.cms.CmsStyles;
import org.argeo.cms.CmsView;
import org.argeo.cms.auth.AuthConstants;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.auth.HttpRequestCallback;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** The site-related user menu */
public class UserMenu implements CmsStyles, CallbackHandler {
	private final CmsView cmsView;
	private final Shell shell;

	private Text username, password;

	public UserMenu(Control source, boolean autoclose) {
		shell = new Shell(source.getDisplay(), SWT.NO_TRIM | SWT.BORDER
				| SWT.ON_TOP);
		shell.setData(RWT.CUSTOM_VARIANT, CMS_USER_MENU);
		// cmsView = (CmsView) shell.getDisplay().getData(CmsView.KEY);
		cmsView = CmsUtils.getCmsView();

		if (cmsView != null) {
			String username = CurrentUser.getUsername(cmsView.getSubject());
			if (username == null
					|| username.equalsIgnoreCase(AuthConstants.ROLE_ANONYMOUS)) {
				username = null;
				anonymousUi(shell);
			} else {
				userUi(shell);
			}
		} else {
			anonymousUi(shell);
		}

		shell.pack();
		shell.layout();
		if (autoclose)// popup
			shell.setLocation(source.toDisplay(
					source.getSize().x - shell.getSize().x, source.getSize().y));
		else // centered
		{
			Rectangle shellBounds = Display.getCurrent().getBounds();// RAP
			Point dialogSize = shell.getSize();
			int x = shellBounds.x + (shellBounds.width - dialogSize.x) / 2;
			int y = shellBounds.y + (shellBounds.height - dialogSize.y) / 2;
			shell.setLocation(x, y);

		}
		if (autoclose)
			shell.addShellListener(new ShellAdapter() {
				private static final long serialVersionUID = 5178980294808435833L;

				@Override
				public void shellDeactivated(ShellEvent e) {
					closeShell();
				}
			});
		shell.open();

	}

	protected void closeShell() {
		shell.close();
		shell.dispose();
	}

	protected void userUi(Composite parent) {
		parent.setLayout(CmsUtils.noSpaceGridLayout());
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout());
		c.setLayoutData(CmsUtils.fillAll());

		specificUserUi(c);

		Label l = new Label(c, SWT.NONE);
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

	/** To be overridden */
	protected void specificUserUi(Composite parent) {

	}

	protected void anonymousUi(Composite parent) {
		parent.setLayout(CmsUtils.noSpaceGridLayout());

		// We need a composite for the traversal
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout());
		c.setLayoutData(CmsUtils.fillAll());

		Integer textWidth = 120;
		parent.setData(RWT.CUSTOM_VARIANT, CMS_USER_MENU);

		// new Label(this, SWT.NONE).setText(CmsMsg.username.lead());
		username = new Text(c, SWT.BORDER);
		username.setMessage(CmsMsg.username.lead());
		username.setData(RWT.CUSTOM_VARIANT, CMS_LOGIN_DIALOG_USERNAME);
		GridData gd = CmsUtils.fillWidth();
		gd.widthHint = textWidth;
		username.setLayoutData(gd);

		// new Label(this, SWT.NONE).setText(CmsMsg.password.lead());
		password = new Text(c, SWT.BORDER | SWT.PASSWORD);
		password.setMessage(CmsMsg.password.lead());
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
		c.addTraverseListener(tl);
		username.addTraverseListener(tl);
		password.addTraverseListener(tl);
		parent.setTabList(new Control[] { c });
		c.setTabList(new Control[] { username, password });
		c.setFocus();
	}

	protected void login() {
		Subject subject = cmsView.getSubject();
		LoginContext loginContext;
		try {
			//
			// LOGIN
			//
			new LoginContext(LOGIN_CONTEXT_ANONYMOUS, subject).logout();
			loginContext = new LoginContext(LOGIN_CONTEXT_USER, subject, this);
			loginContext.login();
		} catch (LoginException e1) {
			throw new CmsException("Cannot authenticate", e1);
		}
		closeShell();
		cmsView.authChange(loginContext);
	}

	protected void logout() {
		closeShell();
		cmsView.logout();
		cmsView.navigateTo("~");
	}

	@Override
	public void handle(Callback[] callbacks) throws IOException,
			UnsupportedCallbackException {
		for (Callback callback : callbacks) {
			if (callback instanceof NameCallback)
				((NameCallback) callback).setName(username.getText());
			else if (callback instanceof PasswordCallback)
				((PasswordCallback) callback).setPassword(password
						.getTextChars());
			else if (callback instanceof HttpRequestCallback)
				((HttpRequestCallback) callback).setRequest(RWT.getRequest());
		}
	}

	public Shell getShell() {
		return shell;
	}
}
