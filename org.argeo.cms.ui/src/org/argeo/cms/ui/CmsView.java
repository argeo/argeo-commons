package org.argeo.cms.ui;

import javax.security.auth.login.LoginContext;

/** Provides interaction with the CMS system. */
public interface CmsView {
	String KEY = "org.argeo.cms.ui.view";

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
}
