package org.argeo.cms.auth;

import org.osgi.service.useradmin.Authorization;

public interface WebCmsSession {
	public final static String CMS_DN = "cms.dn";
	public final static String CMS_SESSION_ID = "cms.sessionId";

//	public String getId();

	public Authorization getAuthorization();

//	public void addHttpSession(HttpServletRequest request);

//	public void cleanUp();
}
