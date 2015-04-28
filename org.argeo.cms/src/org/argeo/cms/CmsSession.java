package org.argeo.cms;

import javax.security.auth.Subject;

import org.argeo.cms.i18n.Msg;

/** Provides interaction with the CMS system. UNSTABLE API at this stage. */
public interface CmsSession {
	public final static String KEY = "org.argeo.connect.web.cmsSession";

	final ThreadLocal<CmsSession> current = new ThreadLocal<CmsSession>();

	// NAVIGATION
	public void navigateTo(String state);

	public String getState();

	// SECURITY
	public void authChange();
	
	public Subject getSubject();

	// SERVICES
	public void exception(Throwable e);

	public Object local(Msg msg);

	public CmsImageManager getImageManager();
}
