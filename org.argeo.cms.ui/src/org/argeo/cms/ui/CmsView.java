package org.argeo.cms.ui;

import javax.security.auth.login.LoginContext;

import org.argeo.node.security.NodeAuthenticated;

/** Provides interaction with the CMS system. UNSTABLE API at this stage. */
public interface CmsView extends NodeAuthenticated {
	UxContext getUxContext();

	// NAVIGATION
	void navigateTo(String state);

	// SECURITY
	void authChange(LoginContext loginContext);

	void logout();
	
//	void registerCallbackHandler(CallbackHandler callbackHandler);

	// SERVICES
	void exception(Throwable e);

	CmsImageManager getImageManager();
}
