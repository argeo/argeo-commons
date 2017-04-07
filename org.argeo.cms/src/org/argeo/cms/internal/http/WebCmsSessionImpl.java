package org.argeo.cms.internal.http;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

import org.argeo.cms.auth.CmsSession;
import org.argeo.cms.internal.auth.CmsSessionImpl;
import org.osgi.service.useradmin.Authorization;

public class WebCmsSessionImpl extends CmsSessionImpl {

	public WebCmsSessionImpl(Subject initialSubject, Authorization authorization, String httpSessionId) {
		super(initialSubject, authorization, httpSessionId);
	}

	public static CmsSession getCmsSession(HttpServletRequest request) {
//		CmsSession cmsSession = (CmsSession) request.getAttribute(CmsSession.class.getName());
//		if (cmsSession != null)
//			return cmsSession;
		return CmsSessionImpl.getByLocalId(request.getSession().getId());
	}
}
