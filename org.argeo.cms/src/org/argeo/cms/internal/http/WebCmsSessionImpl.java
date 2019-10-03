package org.argeo.cms.internal.http;

import java.util.Locale;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.argeo.cms.internal.auth.CmsSessionImpl;
import org.osgi.service.useradmin.Authorization;

public class WebCmsSessionImpl extends CmsSessionImpl {
	// private final static Log log =
	// LogFactory.getLog(WebCmsSessionImpl.class);

	private HttpSession httpSession;

	public WebCmsSessionImpl(Subject initialSubject, Authorization authorization, Locale locale, HttpServletRequest request) {
		super(initialSubject, authorization, locale,request.getSession(false).getId());
		httpSession = request.getSession(false);
	}

	@Override
	public boolean isValid() {
		if (isClosed())
			return false;
		try {// test http session
			httpSession.getCreationTime();
			return true;
		} catch (IllegalStateException ise) {
			return false;
		}
	}

	public static CmsSessionImpl getCmsSession(HttpServletRequest request) {
		return CmsSessionImpl.getByLocalId(request.getSession(false).getId());
	}
}
