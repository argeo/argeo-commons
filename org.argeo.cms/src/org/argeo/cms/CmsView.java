package org.argeo.cms;

import javax.security.auth.Subject;

/** Provides interaction with the CMS system. UNSTABLE API at this stage. */
public interface CmsView {
	public final static String KEY = "org.argeo.cms.view";

	// NAVIGATION
	public void navigateTo(String state);

	// SECURITY
	public void authChange();

	public Subject getSubject();

	// SERVICES
	public void exception(Throwable e);

	public CmsImageManager getImageManager();
}
