package org.argeo.cms.swt;

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import org.argeo.api.cms.CmsEventBus;
import org.argeo.api.cms.ux.CmsImageManager;
import org.argeo.api.cms.ux.CmsUi;
import org.argeo.api.cms.ux.CmsView;
import org.argeo.api.cms.ux.UxContext;
import org.argeo.cms.auth.CurrentUser;
import org.eclipse.swt.widgets.Display;

public abstract class AbstractSwtCmsView implements CmsView {
	protected final String uiName;

	protected LoginContext loginContext;
	protected String state;
	protected Throwable exception;
	protected UxContext uxContext;
	protected CmsImageManager imageManager;

	protected Display display;
	protected CmsUi ui;

	protected String uid;

	public AbstractSwtCmsView(String uiName) {
		this.uiName = uiName;
	}

	public abstract CmsEventBus getCmsEventBus();

	@Override
	public void sendEvent(String topic, Map<String, Object> properties) {
		if (properties == null)
			properties = new HashMap<>();
		if (properties.containsKey(CMS_VIEW_UID_PROPERTY) && !properties.get(CMS_VIEW_UID_PROPERTY).equals(uid))
			throw new IllegalArgumentException("Property " + CMS_VIEW_UID_PROPERTY + " is set to another CMS view uid ("
					+ properties.get(CMS_VIEW_UID_PROPERTY) + ") then " + uid);
		properties.put(CMS_VIEW_UID_PROPERTY, uid);
		getCmsEventBus().sendEvent(topic, properties);
	}

	public <T> T doAs(PrivilegedAction<T> action) {
		try {
			CompletableFuture<T> result = new CompletableFuture<>();
			Runnable toDo = () -> {
				T res = Subject.doAs(getSubject(), action);
				result.complete(res);
			};
			if (Thread.currentThread() == display.getThread())
				toDo.run();
			else
				display.syncExec(toDo);
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
