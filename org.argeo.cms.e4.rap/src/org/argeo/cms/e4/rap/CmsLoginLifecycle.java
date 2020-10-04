package org.argeo.cms.e4.rap;

import java.security.AccessController;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.api.NodeConstants;
import org.argeo.cms.CmsException;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.cms.ui.CmsImageManager;
import org.argeo.cms.ui.CmsView;
import org.argeo.cms.ui.UxContext;
import org.argeo.cms.ui.dialogs.CmsFeedback;
import org.argeo.cms.ui.util.SimpleImageManager;
import org.argeo.cms.ui.util.SimpleUxContext;
import org.argeo.cms.ui.widgets.auth.CmsLoginShell;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.eclipse.e4.ui.workbench.lifecycle.PreSave;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.BrowserNavigation;
import org.eclipse.rap.rwt.client.service.BrowserNavigationEvent;
import org.eclipse.rap.rwt.client.service.BrowserNavigationListener;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

@SuppressWarnings("restriction")
public class CmsLoginLifecycle implements CmsView {
	private final static Log log = LogFactory.getLog(CmsLoginLifecycle.class);

	private UxContext uxContext;
	private CmsImageManager imageManager;

	private LoginContext loginContext;
	private BrowserNavigation browserNavigation;

	private String state = null;

	@PostContextCreate
	boolean login(final IEventBroker eventBroker) {
		browserNavigation = RWT.getClient().getService(BrowserNavigation.class);
		if (browserNavigation != null)
			browserNavigation.addBrowserNavigationListener(new BrowserNavigationListener() {
				private static final long serialVersionUID = -3668136623771902865L;

				@Override
				public void navigated(BrowserNavigationEvent event) {
					state = event.getState();
					if (uxContext != null)// is logged in
						stateChanged();
				}
			});

		Subject subject = Subject.getSubject(AccessController.getContext());
		Display display = Display.getCurrent();
//		UiContext.setData(CmsView.KEY, this);
		CmsLoginShell loginShell = new CmsLoginShell(this);
		CmsView.registerCmsView(loginShell.getShell(), this);
		loginShell.setSubject(subject);
		try {
			// try pre-auth
			loginContext = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER, subject, loginShell);
			loginContext.login();
		} catch (LoginException e) {
			loginShell.createUi();
			loginShell.open();

			while (!loginShell.getShell().isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		}
		if (CurrentUser.getUsername(getSubject()) == null)
			return false;
		uxContext = new SimpleUxContext();
		imageManager = new SimpleImageManager();

		eventBroker.subscribe(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE, new EventHandler() {
			@Override
			public void handleEvent(Event event) {
				startupComplete();
				eventBroker.unsubscribe(this);
			}
		});

		// lcs.changeApplicationLocale(Locale.FRENCH);
		return true;
	}

	@PreSave
	void destroy() {
		// logout();
	}

	@Override
	public UxContext getUxContext() {
		return uxContext;
	}

	@Override
	public void navigateTo(String state) {
		browserNavigation.pushState(state, state);
	}

	@Override
	public void authChange(LoginContext loginContext) {
		if (loginContext == null)
			throw new CmsException("Login context cannot be null");
		// logout previous login context
		// if (this.loginContext != null)
		// try {
		// this.loginContext.logout();
		// } catch (LoginException e1) {
		// System.err.println("Could not log out: " + e1);
		// }
		this.loginContext = loginContext;
	}

	@Override
	public void logout() {
		if (loginContext == null)
			throw new CmsException("Login context should not be null");
		try {
			CurrentUser.logoutCmsSession(loginContext.getSubject());
			loginContext.logout();
		} catch (LoginException e) {
			throw new CmsException("Cannot log out", e);
		}
	}

	@Override
	public void exception(Throwable e) {
		String msg = "Unexpected exception in Eclipse 4 RAP";
		log.error(msg, e);
		CmsFeedback.show(msg, e);
	}

	@Override
	public CmsImageManager getImageManager() {
		return imageManager;
	}

	protected Subject getSubject() {
		return loginContext.getSubject();
	}

	@Override
	public boolean isAnonymous() {
		return CurrentUser.isAnonymous(getSubject());
	}

	// CALLBACKS
	protected void startupComplete() {
	}

	protected void stateChanged() {

	}

	// GETTERS
	protected BrowserNavigation getBrowserNavigation() {
		return browserNavigation;
	}

	protected String getState() {
		return state;
	}

}
