package org.argeo.cms.web;

import static org.eclipse.rap.rwt.internal.service.ContextProvider.getApplicationContext;

import java.net.URI;
import java.net.URISyntaxException;
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
import org.argeo.cms.LocaleUtils;
import org.argeo.cms.auth.RemoteAuthCallbackHandler;
import org.argeo.cms.swt.AbstractSwtCmsView;
import org.argeo.cms.swt.CmsSwtUtils;
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
		super(uiName, UUID.randomUUID().toString(), (CmsImageManager<?, ?>) new AcrSwtImageManager());
		assert cmsWebApp != null;
		assert uiName != null;
		this.cmsWebApp = cmsWebApp;

		// Initial login
		LoginContext lc;
		try {
			lc = CmsAuth.USER.newLoginContext(
					new RemoteAuthCallbackHandler(UiContext.getRemoteAuthRequest(), UiContext.getRemoteAuthResponse()));
			lc.login();
		} catch (LoginException e) {
			try {
				lc = CmsAuth.ANONYMOUS.newLoginContext(new RemoteAuthCallbackHandler(UiContext.getRemoteAuthRequest(),
						UiContext.getRemoteAuthResponse()));
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
		Subject.callAs(getSubject(), () -> {
			try {
				CmsSession cmsSession = getCmsSession();
				if (cmsSession != null) {
					UiContext.setLocale(cmsSession.getLocale());
					LocaleUtils.setThreadLocale(cmsSession.getLocale());
				} else {
					Locale rwtLocale = RWT.getUISession().getLocale();
					LocaleUtils.setThreadLocale(rwtLocale);
				}
				serverPushSession = new ServerPushSession();
				// required in order for doAs() to work
				// TODO check whether it would be worth optimising
				serverPushSession.start();
				initUi(parent);
			} catch (Exception e) {
				throw new IllegalStateException("Cannot create entrypoint contents", e);
			}
			return null;
		});
	}

	@Override
	public void exception(final Throwable e) {
		if (e instanceof SWTError) {
			SWTError swtError = (SWTError) e;
			if (swtError.code == SWT.ERROR_FUNCTION_DISPOSED)
				return;
		}
		getDisplay().syncExec(() -> {
			// TODO internationalise
			CmsFeedback.error("Unexpected exception", e);
			// TODO report
//			doRefresh();
		});
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

//	public CmsImageManager getImageManager() {
//		return imageManager;
//	}

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

//	@Override
//	public CmsSession getCmsSession() {
//		CmsSession cmsSession = getCmsApp().getCmsContext().getCmsSession(getSubject());
//		if (cmsSession == null)
//			throw new IllegalStateException("No CMS session available for " + getSubject());
//		return cmsSession;
//	}

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

	@SuppressWarnings("removal")
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
					Subject.callAs(getSubject(), () -> {
						// TODO rather loop here, until there is an auth change
						if (!display.readAndDispatch()) {
							// TODO update UI last access here
							display.sleep();
						}
						return null;
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
					// ThreadDeath is expected when the RWT UI thread terminates
					// since org.eclipse.rap.rwt.internal.lifecycle.UIThread$UIThreadTerminatedError
					// extends it (but is package protected)
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
