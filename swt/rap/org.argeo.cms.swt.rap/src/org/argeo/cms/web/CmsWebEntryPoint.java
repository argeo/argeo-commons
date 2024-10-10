package org.argeo.cms.web;

import static org.eclipse.rap.rwt.internal.service.ContextProvider.getApplicationContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivilegedAction;
import java.util.Locale;
import java.util.UUID;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.argeo.api.cms.CmsApp;
import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsEventBus;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsSession;
import org.argeo.api.cms.ux.CmsImageManager;
import org.argeo.api.cms.ux.CmsView;
import org.argeo.cms.CurrentUser;
import org.argeo.cms.LocaleUtils;
import org.argeo.cms.auth.RemoteAuthCallbackHandler;
import org.argeo.cms.servlet.ServletHttpRequest;
import org.argeo.cms.servlet.ServletHttpResponse;
import org.argeo.cms.swt.AbstractSwtCmsView;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.swt.SimpleSwtUxContext;
import org.argeo.cms.swt.acr.AcrSwtImageManager;
import org.argeo.cms.swt.dialogs.CmsFeedback;
import org.argeo.eclipse.ui.specific.UiContext;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.EntryPoint;
import org.eclipse.rap.rwt.client.service.BrowserNavigation;
import org.eclipse.rap.rwt.client.service.BrowserNavigationEvent;
import org.eclipse.rap.rwt.client.service.BrowserNavigationListener;
import org.eclipse.rap.rwt.internal.lifecycle.RWTLifeCycle;
import org.eclipse.rap.rwt.service.ServerPushSession;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/** The {@link CmsView} for a {@link CmsWebApp}. */
@SuppressWarnings("restriction")
class CmsWebEntryPoint extends AbstractSwtCmsView implements EntryPoint, CmsView, BrowserNavigationListener {
	private static final long serialVersionUID = 7733510691684570402L;
	private final static CmsLog log = CmsLog.getLog(CmsWebEntryPoint.class);

	private final CmsWebApp cmsWebApp;

	// Client services
	// private final JavaScriptExecutor jsExecutor;
	private final BrowserNavigation browserNavigation;

	/** Experimental OS-like multi windows. */
	private boolean multipleShells = false;

	private ServerPushSession serverPushSession;

	public CmsWebEntryPoint(CmsWebApp cmsWebApp, String uiName) {
		super(uiName);
		assert cmsWebApp != null;
		assert uiName != null;
		this.cmsWebApp = cmsWebApp;
		uid = UUID.randomUUID().toString();

		// Initial login
		LoginContext lc;
		try {
			lc = new LoginContext(CmsAuth.LOGIN_CONTEXT_USER,
					new RemoteAuthCallbackHandler(new ServletHttpRequest(UiContext.getHttpRequest()),
							new ServletHttpResponse(UiContext.getHttpResponse())));
			lc.login();
		} catch (LoginException e) {
			try {
				lc = new LoginContext(CmsAuth.LOGIN_CONTEXT_ANONYMOUS,
						new RemoteAuthCallbackHandler(new ServletHttpRequest(UiContext.getHttpRequest()),
								new ServletHttpResponse(UiContext.getHttpResponse())));
				lc.login();
			} catch (LoginException e1) {
				throw new IllegalStateException("Cannot log in as anonymous", e1);
			}
		}
		authChange(lc);

		// jsExecutor = RWT.getClient().getService(JavaScriptExecutor.class);
		browserNavigation = RWT.getClient().getService(BrowserNavigation.class);
		if (browserNavigation != null)
			browserNavigation.addBrowserNavigationListener(this);

	}

	protected void createContents(Composite parent) {
		Subject.doAs(loginContext.getSubject(), new PrivilegedAction<Void>() {
			@Override
			public Void run() {
				try {
					uxContext = new SimpleSwtUxContext();
					imageManager = (CmsImageManager) new AcrSwtImageManager();
					CmsSession cmsSession = getCmsSession();
					if (cmsSession != null) {
						UiContext.setLocale(cmsSession.getLocale());
						LocaleUtils.setThreadLocale(cmsSession.getLocale());
					} else {
						Locale rwtLocale = RWT.getUISession().getLocale();
						LocaleUtils.setThreadLocale(rwtLocale);
					}
					parent.setData(CmsApp.UI_NAME_PROPERTY, uiName);
					display = parent.getDisplay();
					ui = cmsWebApp.getCmsApp().initUi(parent);
					if (ui instanceof Composite)
						((Composite) ui).setLayoutData(CmsSwtUtils.fillAll());
					serverPushSession = new ServerPushSession();

					// required in order to doAs to work
					// TODO check whether it would be worth optimising
					serverPushSession.start();
					// we need ui to be set before refresh so that CmsView can store UI context data
					// in it.
					cmsWebApp.getCmsApp().refreshUi(ui, null);
				} catch (Exception e) {
					throw new IllegalStateException("Cannot create entrypoint contents", e);
				}
				return null;
			}
		});
	}

	@Override
	public synchronized void logout() {
		if (loginContext == null)
			throw new IllegalArgumentException("Login context should not be null");
		try {
			CurrentUser.logoutCmsSession(loginContext.getSubject());
			loginContext.logout();
			LoginContext anonymousLc = new LoginContext(CmsAuth.LOGIN_CONTEXT_ANONYMOUS,
					new RemoteAuthCallbackHandler(new ServletHttpRequest(UiContext.getHttpRequest()),
							new ServletHttpResponse(UiContext.getHttpResponse())));
			anonymousLc.login();
			authChange(anonymousLc);
		} catch (LoginException e) {
			log.error("Cannot logout", e);
		}
	}

	@Override
	public synchronized void authChange(LoginContext lc) {
		if (lc == null)
			throw new IllegalArgumentException("Login context cannot be null");
		// logout previous login context
		if (this.loginContext != null)
			try {
				this.loginContext.logout();
			} catch (LoginException e1) {
				log.warn("Could not log out: " + e1);
			}
		this.loginContext = lc;
		doRefresh();
	}

	@Override
	public void exception(final Throwable e) {
		if (e instanceof SWTError) {
			SWTError swtError = (SWTError) e;
			if (swtError.code == SWT.ERROR_FUNCTION_DISPOSED)
				return;
		}
		display.syncExec(() -> {
			// TODO internationalise
			CmsFeedback.error("Unexpected exception", e);
			// TODO report
//			doRefresh();
		});
	}

	protected synchronized void doRefresh() {
		if (ui != null)
			Subject.doAs(getSubject(), new PrivilegedAction<Void>() {
				@Override
				public Void run() {
//					if (exception != null) {
//						// TODO internationalise
//						CmsFeedback.error("Unexpected exception", exception);
//						exception = null;
//						// TODO report
//					}
					cmsWebApp.getCmsApp().refreshUi(ui, state);
					return null;
				}
			});
	}

	/** Sets the state of the entry point and retrieve the related content. */
	protected String setState(String newState) {
		cmsWebApp.getCmsApp().setState(ui, newState);
		state = newState;
		return null;
	}

	@Override
	public void navigateTo(String state) {
//		exception = null;
		String title = setState(state);
		if (title != null)
			doRefresh();
		if (browserNavigation != null)
			browserNavigation.pushState(state, title);
	}

	public CmsImageManager getImageManager() {
		return imageManager;
	}

	@Override
	public void navigated(BrowserNavigationEvent event) {
		setState(event.getState());
		// doRefresh();
	}

	@Override
	public CmsEventBus getCmsEventBus() {
		return cmsWebApp.getCmsEventBus();
	}

	@Override
	public CmsApp getCmsApp() {
		return cmsWebApp.getCmsApp();
	}

	@Override
	public void stateChanged(String state, String title) {
		browserNavigation.pushState(state, title);
	}

	@Override
	public CmsSession getCmsSession() {
		CmsSession cmsSession = cmsWebApp.getCmsApp().getCmsContext().getCmsSession(getSubject());
		if (cmsSession == null)
			throw new IllegalStateException("No CMS session available for " + getSubject());
		return cmsSession;
	}

	@Override
	public URI toBackendUri(String url) {
		try {
			return new URI(url);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Cannot convert " + url, e);
		}
	}

	/*
	 * EntryPoint IMPLEMENTATION
	 */

	@Override
	public int createUI() {
		Display display = new Display();
		Shell shell = createShell(display);
		shell.setLayout(CmsSwtUtils.noSpaceGridLayout());
		CmsSwtUtils.registerCmsView(shell, this);
		createContents(shell);
		shell.layout();
//		if (shell.getMaximized()) {
//			shell.layout();
//		} else {
////			shell.pack();
//		}
		shell.open();
		if (getApplicationContext().getLifeCycleFactory().getLifeCycle() instanceof RWTLifeCycle) {
			eventLoop: while (!shell.isDisposed()) {
				try {
					Subject.doAs(loginContext.getSubject(), new PrivilegedAction<Void>() {
						@Override
						public Void run() {
							// TODO rather loop here, until there is an auth change
							if (!display.readAndDispatch()) {
								// TODO update UI last access here
								display.sleep();
							}
							return null;
						}
					});
				} catch (SWTError e) {
					SWTError swtError = (SWTError) e;
					if (swtError.code == SWT.ERROR_FUNCTION_DISPOSED) {
						if (log.isTraceEnabled())
							log.error("Unexpected SWT error in event loop, ignoring it. " + e.getMessage());
						continue eventLoop;
					} else {
						log.error("Unexpected SWT error in event loop, shutting down...", e);
						break eventLoop;
					}
				} catch (ThreadDeath e) {
					// ThreadDeath is expected when the UI thread terminates
					throw (ThreadDeath) e;
				} catch (Error e) {
					log.error("Unexpected error in event loop, shutting down...", e);
					break eventLoop;
				} catch (Throwable e) {
					log.error("Unexpected exception in event loop, ignoring it. " + e.getMessage());
					continue eventLoop;
				}
			}
			if (serverPushSession != null)
				serverPushSession.stop();
			if (!display.isDisposed())
				display.dispose();
		}
		return 0;
	}

	protected Shell createShell(Display display) {
		Shell shell;
		if (!multipleShells) {
			shell = new Shell(display, SWT.NO_TRIM);
			shell.setMaximized(true);
		} else {
			shell = new Shell(display, SWT.SHELL_TRIM);
			shell.setSize(800, 600);
		}
		return shell;
	}
}
