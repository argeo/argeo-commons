package org.argeo.api.cms;

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginContext;

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

	CmsImageManager<?, ?> getImageManager();

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

	default <T> T doAs(PrivilegedAction<T> action) {
		throw new UnsupportedOperationException();
	}

	default Void runAs(Runnable runnable) {
		return doAs(new PrivilegedAction<Void>() {

			@Override
			public Void run() {
				if (runnable != null)
					runnable.run();
				return null;
			}
		});
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
