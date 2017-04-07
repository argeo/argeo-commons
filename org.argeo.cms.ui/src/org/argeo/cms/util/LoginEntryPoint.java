package org.argeo.cms.util;

import java.util.Locale;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import org.argeo.cms.CmsException;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.ui.CmsImageManager;
import org.argeo.cms.ui.CmsView;
import org.argeo.cms.ui.UxContext;
import org.argeo.cms.widgets.auth.CmsLogin;
import org.argeo.cms.widgets.auth.CmsLoginShell;
import org.argeo.eclipse.ui.specific.UiContext;
import org.argeo.node.NodeConstants;
import org.argeo.node.security.NodeAuthenticated;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.EntryPoint;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class LoginEntryPoint implements EntryPoint, CmsView {
	protected final static String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";
	protected final static String HEADER_AUTHORIZATION = "Authorization";
	// private final static Log log = LogFactory.getLog(WorkbenchLogin.class);
	// private final Subject subject = new Subject();
	private LoginContext loginContext;
	private UxContext uxContext = null;

	@Override
	public int createUI() {
		final Display display = createDisplay();
		UiContext.setData(NodeAuthenticated.KEY, this);
		CmsLoginShell loginShell = createCmsLoginShell();
		try {
			// try pre-auth
			loginContext = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER, loginShell);
			loginContext.login();
		} catch (LoginException e) {
			loginShell.createUi();
			loginShell.open();

//			HttpServletRequest request = RWT.getRequest();
//			String authorization = request.getHeader(HEADER_AUTHORIZATION);
//			if (authorization == null || !authorization.startsWith("Negotiate")) {
//				HttpServletResponse response = RWT.getResponse();
//				response.setStatus(401);
//				response.setHeader(HEADER_WWW_AUTHENTICATE, "Negotiate");
//				response.setDateHeader("Date", System.currentTimeMillis());
//				response.setDateHeader("Expires", System.currentTimeMillis() + (24 * 60 * 60 * 1000));
//				response.setHeader("Accept-Ranges", "bytes");
//				response.setHeader("Connection", "Keep-Alive");
//				response.setHeader("Keep-Alive", "timeout=5, max=97");
//				// response.setContentType("text/html; charset=UTF-8");
//			}

			while (!loginShell.getShell().isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		}

		if (CurrentUser.getUsername() == null)
			return -1;
		uxContext = new SimpleUxContext();
		return postLogin();
	}

	protected Display createDisplay() {
		return new Display();
	}

	protected int postLogin() {
		return 0;
	}

	protected HttpServletRequest getRequest() {
		return RWT.getRequest();
	}

	protected CmsLoginShell createCmsLoginShell() {
		return new CmsLoginShell(this) {

			@Override
			public void createContents(Composite parent) {
				LoginEntryPoint.this.createLoginPage(parent, this);
			}

			@Override
			protected void extendsCredentialsBlock(Composite credentialsBlock, Locale selectedLocale,
					SelectionListener loginSelectionListener) {
				LoginEntryPoint.this.extendsCredentialsBlock(credentialsBlock, selectedLocale, loginSelectionListener);
			}

		};
	}

	/**
	 * To be overridden. CmsLogin#createCredentialsBlock() should be called at
	 * some point in order to create the credentials composite. In order to use
	 * the default layout, call CmsLogin#defaultCreateContents() but <b>not</b>
	 * CmsLogin#createContent(), since it would lead to a stack overflow.
	 */
	protected void createLoginPage(Composite parent, CmsLogin login) {
		login.defaultCreateContents(parent);
	}

	protected void extendsCredentialsBlock(Composite credentialsBlock, Locale selectedLocale,
			SelectionListener loginSelectionListener) {

	}

	@Override
	public void navigateTo(String state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void authChange(LoginContext loginContext) {
		this.loginContext = loginContext;
	}

	@Override
	public void logout() {
		if (loginContext == null)
			throw new CmsException("Login context should not bet null");
		try {
			CurrentUser.logoutCmsSession(loginContext.getSubject());
			loginContext.logout();
		} catch (LoginException e) {
			throw new CmsException("Cannot log out", e);
		}
	}

	@Override
	public void exception(Throwable e) {
		// TODO Auto-generated method stub

	}

	@Override
	public LoginContext getLoginContext() {
		return loginContext;
	}

	@Override
	public CmsImageManager getImageManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UxContext getUxContext() {
		return uxContext;
	}
}