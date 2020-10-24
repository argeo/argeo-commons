package org.argeo.cms.ui;

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginContext;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

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

	CmsImageManager getImageManager();

	boolean isAnonymous();

	/**
	 * Send an event to this topic. Does noothing by default., but if implemented it
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

	static CmsView getCmsView(Control parent) {
		// find parent shell
		Shell topShell = parent.getShell();
		while (topShell.getParent() != null)
			topShell = (Shell) topShell.getParent();
		return (CmsView) topShell.getData(CmsView.class.getName());
	}

	static void registerCmsView(Shell shell, CmsView view) {
		// find parent shell
		Shell topShell = shell;
		while (topShell.getParent() != null)
			topShell = (Shell) topShell.getParent();
		// check if already set
		if (topShell.getData(CmsView.class.getName()) != null) {
			CmsView registeredView = (CmsView) topShell.getData(CmsView.class.getName());
			throw new IllegalArgumentException("Cms view " + registeredView + " already registered in this shell");
		}
		shell.setData(CmsView.class.getName(), view);
	}

}
