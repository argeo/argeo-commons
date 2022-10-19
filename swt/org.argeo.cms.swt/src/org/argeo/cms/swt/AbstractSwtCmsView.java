package org.argeo.cms.swt;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import org.argeo.api.cms.CmsApp;
import org.argeo.api.cms.CmsEventBus;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.ux.CmsImageManager;
import org.argeo.api.cms.ux.CmsUi;
import org.argeo.api.cms.ux.CmsView;
import org.argeo.api.cms.ux.UxContext;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.util.CurrentSubject;
import org.eclipse.swt.widgets.Display;

public abstract class AbstractSwtCmsView implements CmsView {
	private final static CmsLog log = CmsLog.getLog(AbstractSwtCmsView.class);

	protected final String uiName;

	protected LoginContext loginContext;
	protected String state;
//	protected Throwable exception;
	protected UxContext uxContext;
	protected CmsImageManager imageManager;

	protected Display display;
	protected CmsUi ui;

	protected String uid;

	public AbstractSwtCmsView(String uiName) {
		this.uiName = uiName;
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

	@Override
	public CmsImageManager<?, ?> getImageManager() {
		return imageManager;
	}

	@Override
	public boolean isAnonymous() {
		return CurrentUser.isAnonymous(getSubject());
	}

	protected Subject getSubject() {
		return loginContext.getSubject();
	}

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

}
