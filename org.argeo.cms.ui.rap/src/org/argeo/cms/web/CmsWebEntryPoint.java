package org.argeo.cms.web;

import static org.eclipse.rap.rwt.internal.service.ContextProvider.getApplicationContext;

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.api.NodeConstants;
import org.argeo.cms.auth.CmsSession;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.auth.HttpRequestCallbackHandler;
import org.argeo.cms.ui.CmsApp;
import org.argeo.cms.ui.CmsImageManager;
import org.argeo.cms.ui.CmsView;
import org.argeo.cms.ui.UxContext;
import org.argeo.cms.ui.dialogs.CmsFeedback;
import org.argeo.cms.ui.util.CmsUiUtils;
import org.argeo.cms.ui.util.DefaultImageManager;
import org.argeo.cms.ui.util.SimpleUxContext;
import org.argeo.eclipse.ui.specific.UiContext;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.EntryPoint;
import org.eclipse.rap.rwt.client.service.BrowserNavigation;
import org.eclipse.rap.rwt.client.service.BrowserNavigationEvent;
import org.eclipse.rap.rwt.client.service.BrowserNavigationListener;
import org.eclipse.rap.rwt.internal.lifecycle.RWTLifeCycle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/** The {@link CmsView} for a {@link CmsWebApp}. */
@SuppressWarnings("restriction")
public class CmsWebEntryPoint implements EntryPoint, CmsView, BrowserNavigationListener {
	private static final long serialVersionUID = 7733510691684570402L;
	private final static Log log = LogFactory.getLog(CmsWebEntryPoint.class);

	private EventAdmin eventAdmin;

	private final CmsWebApp cmsWebApp;
	private final String uiName;

	private LoginContext loginContext;
	private String state;
	private Throwable exception;
	private UxContext uxContext;
	private CmsImageManager imageManager;

	private Composite ui;

	private String uid;

	// Client services
	// private final JavaScriptExecutor jsExecutor;
	private final BrowserNavigation browserNavigation;

	/** Experimental OS-like multi windows. */
	private boolean multipleShells = false;

	public CmsWebEntryPoint(CmsWebApp cmsWebApp, String uiName) {
		assert cmsWebApp != null;
		assert uiName != null;
		this.cmsWebApp = cmsWebApp;
		this.uiName = uiName;
		uid = UUID.randomUUID().toString();

		// Initial login
		LoginContext lc;
		try {
			lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER,
					new HttpRequestCallbackHandler(UiContext.getHttpRequest(), UiContext.getHttpResponse()));
			lc.login();
		} catch (LoginException e) {
			try {
				lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_ANONYMOUS);
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
					uxContext = new SimpleUxContext();
					imageManager = new DefaultImageManager();
					ui = cmsWebApp.getCmsApp().initUi(parent);
					ui.setData(CmsApp.UI_NAME_PROPERTY, uiName);
					ui.setLayoutData(CmsUiUtils.fillAll());
				} catch (Exception e) {
					throw new IllegalStateException("Cannot create entrypoint contents", e);
				}
				return null;
			}
		});
	}

	protected Subject getSubject() {
		return loginContext.getSubject();
	}

	public <T> T doAs(PrivilegedAction<T> action) {
		return Subject.doAs(getSubject(), action);
	}

	@Override
	public boolean isAnonymous() {
		return CurrentUser.isAnonymous(getSubject());
	}

	@Override
	public synchronized void logout() {
		if (loginContext == null)
			throw new IllegalArgumentException("Login context should not be null");
		try {
			CurrentUser.logoutCmsSession(loginContext.getSubject());
			loginContext.logout();
			LoginContext anonymousLc = new LoginContext(NodeConstants.LOGIN_CONTEXT_ANONYMOUS);
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
		ui.getDisplay().syncExec(() -> {
			CmsFeedback.show("Unexpected exception in CMS", e);
			exception = e;
//		log.error("Unexpected exception in CMS", e);
			doRefresh();
		});
	}

	protected synchronized void doRefresh() {
		if (ui != null)
			Subject.doAs(getSubject(), new PrivilegedAction<Void>() {
				@Override
				public Void run() {
					if (exception != null) {
						// TODO internationalise
						CmsFeedback.show("Unexpected exception", exception);
						exception = null;
						// TODO report
					}
					cmsWebApp.getCmsApp().refreshUi(ui, state);
					return null;
				}
			});
	}

	/** Sets the state of the entry point and retrieve the related JCR node. */
	protected String setState(String newState) {
		cmsWebApp.getCmsApp().setState(ui, newState);
		state = newState;
		return null;
	}

	@Override
	public UxContext getUxContext() {
		return uxContext;
	}

	@Override
	public String getUid() {
		return uid;
	}

	@Override
	public void navigateTo(String state) {
		exception = null;
		String title = setState(state);
		doRefresh();
		if (browserNavigation != null)
			browserNavigation.pushState(state, title);
	}

	@Override
	public CmsImageManager getImageManager() {
		return imageManager;
	}

	@Override
	public void navigated(BrowserNavigationEvent event) {
		setState(event.getState());
		// doRefresh();
	}

	@Override
	public void sendEvent(String topic, Map<String, Object> properties) {
		if (properties == null)
			properties = new HashMap<>();
		if (properties.containsKey(CMS_VIEW_UID_PROPERTY) && !properties.get(CMS_VIEW_UID_PROPERTY).equals(uid))
			throw new IllegalArgumentException("Property " + CMS_VIEW_UID_PROPERTY + " is set to another CMS view uid ("
					+ properties.get(CMS_VIEW_UID_PROPERTY) + ") then " + uid);
		properties.put(CMS_VIEW_UID_PROPERTY, uid);
		eventAdmin.sendEvent(new Event(topic, properties));
	}

	@Override
	public void stateChanged(String state, String title) {
		browserNavigation.pushState(state, title);
	}

	@Override
	public CmsSession getCmsSession() {
		CmsSession cmsSession = CmsSession.getCmsSession(cmsWebApp.getBundleContext(), getSubject());
		return cmsSession;
	}

	/*
	 * EntryPoint IMPLEMENTATION
	 */

	@Override
	public int createUI() {
		Display display = new Display();
		Shell shell = createShell(display);
		shell.setLayout(CmsUiUtils.noSpaceGridLayout());
		CmsView.registerCmsView(shell, this);
		createContents(shell);
		shell.layout();
//		if (shell.getMaximized()) {
//			shell.layout();
//		} else {
////			shell.pack();
//		}
		shell.open();
		if (getApplicationContext().getLifeCycleFactory().getLifeCycle() instanceof RWTLifeCycle) {
			while (!shell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
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

	public void setEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

}
