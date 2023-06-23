package org.argeo.api.cms.ux;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import javax.security.auth.login.LoginContext;

import org.argeo.api.cms.CmsSession;

/** Provides interaction with the CMS system. */
public interface CmsView {
	final static String CMS_VIEW_UID_PROPERTY = "argeo.cms.view.uid";
	// String KEY = "org.argeo.cms.ui.view";

	String getUid();

	UxContext getUxContext();

	// NAVIGATION
	void navigateTo(String state);

	// SECURITY
	void authChange(LoginContext loginContext);

	void logout();

	// void registerCallbackHandler(CallbackHandler callbackHandler);

	// SERVICES
	void exception(Throwable e);

	<V,M> CmsImageManager<V, M> getImageManager();

	boolean isAnonymous();

	/**
	 * Send an event to this topic. Does nothing by default., but if implemented it
	 * MUST set the {@link #CMS_VIEW_UID_PROPERTY} in the properties.
	 */
	default void sendEvent(String topic, Map<String, Object> properties) {

	}

	/**
	 * Convenience methods for when {@link #sendEvent(String, Map)} only requires
	 * one single parameter.
	 */
	default void sendEvent(String topic, String param, Object value) {
		Map<String, Object> properties = new HashMap<>();
		properties.put(param, value);
		sendEvent(topic, properties);
	}

	default void applyStyles(Object widget) {

	}

	/**
	 * Make sure that this action is executed with the proper subject and in a
	 * proper thread.
	 */
	<T> T doAs(Callable<T> action);

	default void runAs(Runnable runnable) {
		doAs(Executors.callable(runnable));
	}

	default void stateChanged(String state, String title) {
	}

	default CmsSession getCmsSession() {
		throw new UnsupportedOperationException();
	}

	default Object getData(String key) {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unchecked")
	default <T> T getUiContext(Class<T> clss) {
		return (T) getData(clss.getName());
	}

	default <T> void setUiContext(Class<T> clss, T instance) {
		setData(clss.getName(), instance);
	}

	default void setData(String key, Object value) {
		throw new UnsupportedOperationException();
	}

}
