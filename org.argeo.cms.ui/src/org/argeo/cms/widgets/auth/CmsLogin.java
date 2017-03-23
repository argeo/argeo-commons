package org.argeo.cms.widgets.auth;

import static org.argeo.cms.CmsMsg.password;
import static org.argeo.cms.CmsMsg.username;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsMsg;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.auth.HttpRequestCallback;
import org.argeo.cms.i18n.LocaleUtils;
import org.argeo.cms.ui.CmsStyles;
import org.argeo.cms.ui.CmsView;
import org.argeo.cms.ui.internal.Activator;
import org.argeo.cms.util.CmsUtils;
import org.argeo.eclipse.ui.specific.UiContext;
import org.argeo.node.NodeConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class CmsLogin implements CmsStyles, CallbackHandler {
	private final static Log log = LogFactory.getLog(CmsLogin.class);

	private Composite parent;
	private Text usernameT, passwordT;
	private Composite credentialsBlock;
	private final SelectionListener loginSelectionListener;

	private final Locale defaultLocale;
	private LocaleChoice localeChoice = null;

	private final CmsView cmsView;

	public CmsLogin(CmsView cmsView) {
		this.cmsView = cmsView;
		defaultLocale = Activator.getNodeState().getDefaultLocale();
		List<Locale> locales = Activator.getNodeState().getLocales();
		if (locales != null)
			localeChoice = new LocaleChoice(locales, defaultLocale);
		loginSelectionListener = new SelectionListener() {
			private static final long serialVersionUID = -8832133363830973578L;

			@Override
			public void widgetSelected(SelectionEvent e) {
				login();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
	}

	protected boolean isAnonymous() {
		return CurrentUser.isAnonymous(cmsView.getLoginContext().getSubject());
	}

	public final void createUi(Composite parent) {
		this.parent = parent;
		createContents(parent);
	}

	protected void createContents(Composite parent) {
		defaultCreateContents(parent);
	}

	public final void defaultCreateContents(Composite parent) {
		parent.setLayout(CmsUtils.noSpaceGridLayout());
		Composite credentialsBlock = createCredentialsBlock(parent);
		if (parent instanceof Shell) {
			credentialsBlock.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		}
	}

	public final Composite createCredentialsBlock(Composite parent) {
		if (isAnonymous()) {
			return anonymousUi(parent);
		} else {
			return userUi(parent);
		}
	}

	protected Composite getCredentialsBlock() {
		return credentialsBlock;
	}

	protected Composite userUi(Composite parent) {
		Locale locale = localeChoice == null ? this.defaultLocale : localeChoice.getSelectedLocale();
		credentialsBlock = new Composite(parent, SWT.NONE);
		credentialsBlock.setLayout(new GridLayout());
		credentialsBlock.setLayoutData(CmsUtils.fillAll());

		specificUserUi(credentialsBlock);

		Label l = new Label(credentialsBlock, SWT.NONE);
		CmsUtils.style(l, CMS_USER_MENU_ITEM);
		l.setText(CmsMsg.logout.lead(locale));
		GridData lData = CmsUtils.fillWidth();
		lData.widthHint = 120;
		l.setLayoutData(lData);

		l.addMouseListener(new MouseAdapter() {
			private static final long serialVersionUID = 6444395812777413116L;

			public void mouseDown(MouseEvent e) {
				logout();
			}
		});
		return credentialsBlock;
	}

	/** To be overridden */
	protected void specificUserUi(Composite parent) {

	}

	protected Composite anonymousUi(Composite parent) {
		Locale locale = localeChoice == null ? this.defaultLocale : localeChoice.getSelectedLocale();
		// We need a composite for the traversal
		credentialsBlock = new Composite(parent, SWT.NONE);
		credentialsBlock.setLayout(new GridLayout());
		credentialsBlock.setLayoutData(CmsUtils.fillAll());

		Integer textWidth = 120;
		CmsUtils.style(parent, CMS_USER_MENU);

		// new Label(this, SWT.NONE).setText(CmsMsg.username.lead());
		usernameT = new Text(credentialsBlock, SWT.BORDER);
		usernameT.setMessage(username.lead(locale));
		CmsUtils.style(usernameT, CMS_LOGIN_DIALOG_USERNAME);
		GridData gd = CmsUtils.fillWidth();
		gd.widthHint = textWidth;
		usernameT.setLayoutData(gd);

		// new Label(this, SWT.NONE).setText(CmsMsg.password.lead());
		passwordT = new Text(credentialsBlock, SWT.BORDER | SWT.PASSWORD);
		passwordT.setMessage(password.lead(locale));
		CmsUtils.style(passwordT, CMS_LOGIN_DIALOG_PASSWORD);
		gd = CmsUtils.fillWidth();
		gd.widthHint = textWidth;
		passwordT.setLayoutData(gd);

		TraverseListener tl = new TraverseListener() {
			private static final long serialVersionUID = -1158892811534971856L;

			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_RETURN)
					login();
			}
		};
		credentialsBlock.addTraverseListener(tl);
		usernameT.addTraverseListener(tl);
		passwordT.addTraverseListener(tl);
		parent.setTabList(new Control[] { credentialsBlock });
		credentialsBlock.setTabList(new Control[] { usernameT, passwordT });
		// credentialsBlock.setFocus();

		extendsCredentialsBlock(credentialsBlock, locale, loginSelectionListener);
		if (localeChoice != null)
			createLocalesBlock(credentialsBlock);
		return credentialsBlock;
	}

	/**
	 * To be overridden in order to provide custome login button and other
	 * links.
	 */
	protected void extendsCredentialsBlock(Composite credentialsBlock, Locale selectedLocale,
			SelectionListener loginSelectionListener) {

	}

	protected void updateLocale(Locale selectedLocale) {
		// save already entered values
		String usernameStr = usernameT.getText();
		char[] pwd = passwordT.getTextChars();

		for (Control child : parent.getChildren())
			child.dispose();
		createContents(parent);
		if (parent.getParent() != null)
			parent.getParent().layout();
		else
			parent.layout();
		usernameT.setText(usernameStr);
		passwordT.setTextChars(pwd);
	}

	protected Composite createLocalesBlock(final Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(CmsUtils.noSpaceGridLayout());
		c.setLayoutData(CmsUtils.fillAll());

		SelectionListener selectionListener = new SelectionAdapter() {
			private static final long serialVersionUID = 4891637813567806762L;

			public void widgetSelected(SelectionEvent event) {
				Button button = (Button) event.widget;
				if (button.getSelection()) {
					localeChoice.setSelectedIndex((Integer) event.widget.getData());
					updateLocale(localeChoice.getSelectedLocale());
				}
			};
		};

		List<Locale> locales = localeChoice.getLocales();
		for (Integer i = 0; i < locales.size(); i++) {
			Locale locale = locales.get(i);
			Button button = new Button(c, SWT.RADIO);
			button.setData(i);
			button.setText(LocaleUtils.lead(locale.getDisplayName(locale), locale) + " (" + locale + ")");
			// button.addListener(SWT.Selection, listener);
			button.addSelectionListener(selectionListener);
			if (i == localeChoice.getSelectedIndex())
				button.setSelection(true);
		}
		return c;
	}

	protected boolean login() {
		// Subject subject = cmsView.getLoginContext().getSubject();
		LoginContext loginContext = cmsView.getLoginContext();
		try {
			//
			// LOGIN
			//
			loginContext.logout();
			loginContext = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER, this);
			loginContext.login();
		} catch (LoginException e) {
			if (log.isTraceEnabled())
				log.warn("Login failed: " + e.getMessage(), e);
			else
				log.warn("Login failed: " + e.getMessage());

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e2) {
				// silent
			}
			// ErrorFeedback.show("Login failed", e);
			return false;
		}
		// catch (LoginException e) {
		// log.error("Cannot login", e);
		// return false;
		// }
		cmsView.authChange(loginContext);
		return true;
	}

	protected void logout() {
		cmsView.logout();
		cmsView.navigateTo("~");
	}

	@Override
	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		for (Callback callback : callbacks) {
			if (callback instanceof NameCallback && usernameT != null)
				((NameCallback) callback).setName(usernameT.getText());
			else if (callback instanceof PasswordCallback && passwordT != null)
				((PasswordCallback) callback).setPassword(passwordT.getTextChars());
			else if (callback instanceof HttpRequestCallback) {
				((HttpRequestCallback) callback).setRequest(UiContext.getHttpRequest());
				((HttpRequestCallback) callback).setResponse(UiContext.getHttpResponse());
			} else if (callback instanceof LanguageCallback && localeChoice != null)
				((LanguageCallback) callback).setLocale(localeChoice.getSelectedLocale());
		}
	}

}
