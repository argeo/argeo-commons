package org.argeo.cms;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

/** Provides interaction with the CMS system. UNSTABLE API at this stage. */
public interface CmsView {
	public final static String KEY = "org.argeo.cms.view";

	UxContext getUxContext();

	// NAVIGATION
	public void navigateTo(String state);

	// SECURITY
	public void authChange(LoginContext loginContext);

	public Subject getSubject();

	public void logout();

	// SERVICES
	public void exception(Throwable e);

	public CmsImageManager getImageManager();
}
