package org.argeo.cms.swt;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.argeo.api.cms.CmsApp;
import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsEventBus;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsSession;
import org.argeo.api.cms.ux.CmsImageManager;
import org.argeo.api.cms.ux.CmsUi;
import org.argeo.api.cms.ux.CmsView;
import org.argeo.api.cms.ux.UxContext;
import org.argeo.cms.CurrentUser;
import org.argeo.cms.auth.RemoteAuthCallbackHandler;
import org.argeo.cms.util.CurrentSubject;
import org.argeo.eclipse.ui.specific.UiContext;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public abstract class AbstractSwtCmsView implements CmsView {
	private final static CmsLog log = CmsLog.getLog(AbstractSwtCmsView.class);

	/** A timer to be used to perform background UX tasks. */
	private final static Timer uxTimer = new Timer(true);

	static {
		// purge every day
		uxTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				uxTimer.purge();
			}
		}, 0, 24 * 60 * 60 * 1000);
	}

	private final String uiName;

	private LoginContext loginContext;
	private String state;
//	protected Throwable exception;
	private UxContext uxContext;
	private CmsImageManager<?, ?> imageManager;

	private Display display;
	private CmsUi ui;

	private String uid;

	public AbstractSwtCmsView(String uiName, String uid, CmsImageManager<?, ?> imageManager) {
		this.uiName = uiName;
		this.uid = uid;
		this.imageManager = imageManager;
	}

	public abstract CmsEventBus getCmsEventBus();

	public abstract CmsApp getCmsApp();

	@Override
	public void sendEvent(String topic, Map<String, Object> properties) {
		if (properties == null)
			properties = new HashMap<>();
		if (properties.containsKey(CMS_VIEW_UID_PROPERTY) && !properties.get(CMS_VIEW_UID_PROPERTY).equals(uid))
			throw new IllegalArgumentException("Property " + CMS_VIEW_UID_PROPERTY + " is set to another CMS view uid ("
					+ properties.get(CMS_VIEW_UID_PROPERTY) + ") then " + uid);
		properties.put(CMS_VIEW_UID_PROPERTY, uid);

		log.trace(() -> uid + ": send event to " + topic);

		getCmsEventBus().sendEvent(topic, properties);
		// getCmsApp().onEvent(topic, properties);
	}

//	public void runAs(Runnable runnable) {
//		display.asyncExec(() -> doAs(Executors.callable(runnable)));
//	}

	public <T> T doAs(Callable<T> action) {
		try {
			CompletableFuture<T> result = new CompletableFuture<>();
			Runnable toDo = () -> {
				log.trace(() -> uid + ": process doAs");
				Subject subject = CurrentSubject.current();
				T res;
				if (subject != null) {
					assert subject == getSubject();
					try {
						res = action.call();
					} catch (Exception e) {
						throw new CompletionException("Failed to execute action for " + subject, e);
					}
				} else {
					res = CurrentSubject.callAs(getSubject(), action);
				}
				result.complete(res);
			};
			if (Thread.currentThread() == display.getThread())
				toDo.run();
			else {
				display.asyncExec(toDo);
				display.wake();
			}
//				throw new IllegalStateException("Must be called from UI thread");
			return result.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new IllegalStateException("Cannot execute action ins CMS view " + uid, e);
		}
	}

	@Override
	public UxContext getUxContext() {
		return uxContext;
	}

	@Override
	public String getUid() {
		return uid;
	}

	@SuppressWarnings("unchecked")
	@Override
	public CmsImageManager<?, ?> getImageManager() {
		return imageManager;
	}

	public CmsSession getCmsSession() {
		CmsSession cmsSession = getCmsApp().getCmsContext().getCmsSession(getSubject());
		if (cmsSession == null)
			throw new IllegalStateException("No CMS session available for " + getSubject());
		return cmsSession;
	}

	protected void initUi(Composite parent) {
		parent.setData(CmsApp.UI_NAME_PROPERTY, uiName);
		this.display = parent.getDisplay();
		this.uxContext = new SimpleSwtUxContext(display);
		this.ui = getCmsApp().initUi(parent);
		if (ui instanceof Composite)
			((Composite) ui).setLayoutData(CmsSwtUtils.fillAll());
		// we need ui to be set before refresh so that CmsView can store UI context data
		// in it.
		getCmsApp().refreshUi(ui, null);
	}

//	protected void setDisplay(Display display) {
//		this.display = display;
//	}

	@Override
	public boolean isAnonymous() {
		return CurrentUser.isAnonymous(getSubject());
	}

	protected Subject getSubject() {
		if (loginContext == null)
			throw new IllegalStateException("Login context is not set");
		return loginContext.getSubject();
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
	public synchronized void logout() {
		if (loginContext == null)
			throw new IllegalArgumentException("Login context should not be null");
		try {
			CurrentUser.logoutCmsSession(loginContext.getSubject());
			loginContext.logout();
			LoginContext anonymousLc = CmsAuth.ANONYMOUS.newLoginContext(
					new RemoteAuthCallbackHandler(UiContext.getRemoteAuthRequest(), UiContext.getRemoteAuthResponse()));
			anonymousLc.login();
			authChange(anonymousLc);
		} catch (LoginException e) {
			log.error("Cannot logout", e);
		}
	}

	protected synchronized void doRefresh() {
		if (ui != null)
			Subject.callAs(getSubject(), () -> {
				getCmsApp().refreshUi(ui, state);
				return null;
			});
	}

	/** Sets the state of the entry point and retrieve the related content. */
	protected String setState(String newState) {
		getCmsApp().setState(ui, newState);
		state = newState;
		return null;
	}

	protected Display getDisplay() {
		return display;
	}

//	protected void setLoginContext(LoginContext loginContext) {
//		this.loginContext = loginContext;
//	}

//	protected void setUi(CmsUi ui) {
//		this.ui = ui;
//	}

	@Override
	public Object getData(String key) {
		if (ui != null) {
			return ui.getData(key);
		} else {
			throw new IllegalStateException("UI is not initialized");
		}
	}

	@Override
	public void setData(String key, Object value) {
		if (ui != null) {
			ui.setData(key, value);
		} else {
			throw new IllegalStateException("UI is not initialized");
		}
	}

	@Override
	public TimerTask schedule(Runnable task, long delay) {
		TimerTask timerTask = newSwtUxTimerTask(task);
		uxTimer.schedule(timerTask, delay);
		return timerTask;
	}

	@Override
	public TimerTask schedule(Runnable task, long delay, long period) {
		TimerTask timerTask = newSwtUxTimerTask(task);
		uxTimer.schedule(timerTask, delay, period);
		return timerTask;
	}

	protected TimerTask newSwtUxTimerTask(Runnable todo) {
		return new TimerTask() {

			@Override
			public void run() {
				synchronized (display) {
					try {
						if (!display.isDisposed()) {
							display.syncExec(() -> {
								todo.run();
							});
						}
					} catch (Exception e) {
						log.error("Cannot run UX timer task", e);
					}
				}
			}
		};
	}
}
