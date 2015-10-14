package org.argeo.cms.widgets.auth;

import static org.argeo.cms.CmsMsg.password;
import static org.argeo.cms.CmsMsg.username;
import static org.argeo.cms.auth.AuthConstants.LOGIN_CONTEXT_ANONYMOUS;
import static org.argeo.cms.auth.AuthConstants.LOGIN_CONTEXT_USER;
import static org.argeo.cms.internal.kernel.Activator.getKernelHeader;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.argeo.cms.CmsException;
import org.argeo.cms.CmsMsg;
import org.argeo.cms.CmsStyles;
import org.argeo.cms.CmsView;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.auth.HttpRequestCallback;
import org.argeo.cms.i18n.Msg;
import org.argeo.cms.util.CmsUtils;
import org.argeo.util.LocaleChoice;
import org.eclipse.rap.rwt.RWT;
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
	private Text usernameT, passwordT;
	private Composite credentialsBlock;

	private final Locale defaultLocale;
	private LocaleChoice localeChoice = null;

	private final CmsView cmsView;

	public CmsLogin(CmsView cmsView) {
		this.cmsView = cmsView;
		defaultLocale = getKernelHeader().getDefaultLocale();
		List<Locale> locales = getKernelHeader().getLocales();
		if (locales != null)
			localeChoice = new LocaleChoice(locales, defaultLocale);
	}

	protected boolean isAnonymous() {
		return CurrentUser.isAnonymous(cmsView.getSubject());
	}

	public void createContents(Composite parent) {
		defaultCreateContents(parent);
	}

	public final void defaultCreateContents(Composite parent) {
		parent.setLayout(CmsUtils.noSpaceGridLayout());
		Composite credentialsBlock = createCredentialsBlock(parent);
		if (parent instanceof Shell) {
			credentialsBlock.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER,
					true, true));
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
		credentialsBlock = new Composite(parent, SWT.NONE);
		credentialsBlock.setLayout(new GridLayout());
		credentialsBlock.setLayoutData(CmsUtils.fillAll());

		specificUserUi(credentialsBlock);

		Label l = new Label(credentialsBlock, SWT.NONE);
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
		return credentialsBlock;
	}

	/** To be overridden */
	protected void specificUserUi(Composite parent) {

	}

	protected Composite anonymousUi(Composite parent) {
		// We need a composite for the traversal
		credentialsBlock = new Composite(parent, SWT.NONE);
		credentialsBlock.setLayout(new GridLayout());
		credentialsBlock.setLayoutData(CmsUtils.fillAll());

		Integer textWidth = 120;
		parent.setData(RWT.CUSTOM_VARIANT, CMS_USER_MENU);

		// new Label(this, SWT.NONE).setText(CmsMsg.username.lead());
		usernameT = new Text(credentialsBlock, SWT.BORDER);
		usernameT.setMessage(username.lead(defaultLocale));
		usernameT.setData(RWT.CUSTOM_VARIANT, CMS_LOGIN_DIALOG_USERNAME);
		GridData gd = CmsUtils.fillWidth();
		gd.widthHint = textWidth;
		usernameT.setLayoutData(gd);

		// new Label(this, SWT.NONE).setText(CmsMsg.password.lead());
		passwordT = new Text(credentialsBlock, SWT.BORDER | SWT.PASSWORD);
		passwordT.setMessage(password.lead(defaultLocale));
		passwordT.setData(RWT.CUSTOM_VARIANT, CMS_LOGIN_DIALOG_PASSWORD);
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
		credentialsBlock.setFocus();

		if (localeChoice != null)
			createLocalesBlock(credentialsBlock);
		return credentialsBlock;
	}

	protected Composite createLocalesBlock(final Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(CmsUtils.noSpaceGridLayout());
		c.setLayoutData(CmsUtils.fillAll());

		SelectionListener selectionListener = new SelectionAdapter() {
			private static final long serialVersionUID = 4891637813567806762L;

			public void widgetSelected(SelectionEvent event) {
				localeChoice.setSelectedIndex((Integer) event.widget.getData());
				Locale selectedLocale = localeChoice.getSelectedLocale();
				usernameT.setMessage(username.lead(selectedLocale));
				passwordT.setMessage(password.lead(selectedLocale));
			};
		};

		List<Locale> locales = localeChoice.getLocales();
		for (Integer i = 0; i < locales.size(); i++) {
			Locale locale = locales.get(i);
			Button button = new Button(c, SWT.RADIO);
			button.setData(i);
			button.setText(Msg.lead(locale.getDisplayName(locale), locale)
					+ " (" + locale + ")");
			// button.addListener(SWT.Selection, listener);
			button.addSelectionListener(selectionListener);
			if (i == localeChoice.getDefaultIndex())
				button.setSelection(true);
		}
		return c;
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
		cmsView.authChange(loginContext);
	}

	protected void logout() {
		cmsView.logout();
		cmsView.navigateTo("~");
	}

	@Override
	public void handle(Callback[] callbacks) throws IOException,
			UnsupportedCallbackException {
		for (Callback callback : callbacks) {
			if (callback instanceof NameCallback)
				((NameCallback) callback).setName(usernameT.getText());
			else if (callback instanceof PasswordCallback)
				((PasswordCallback) callback).setPassword(passwordT
						.getTextChars());
			else if (callback instanceof HttpRequestCallback)
				((HttpRequestCallback) callback).setRequest(RWT.getRequest());
			else if (callback instanceof LanguageCallback
					&& localeChoice != null)
				((LanguageCallback) callback).setLocale(localeChoice
						.getSelectedLocale());
		}
	}

}
