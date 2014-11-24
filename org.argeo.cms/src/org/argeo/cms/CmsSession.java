package org.argeo.cms;

/** Provides interaction with the CMS system. UNSTABLE API at this stage. */
public interface CmsSession {
	public final static String KEY = "org.argeo.connect.web.cmsSession";

	final ThreadLocal<CmsSession> current = new ThreadLocal<CmsSession>();

	public void navigateTo(String state);

	public void authChange();

	public void exception(Throwable e);

	public Object local(Msg msg);

	public String getState();

	public CmsImageManager getImageManager();
}
