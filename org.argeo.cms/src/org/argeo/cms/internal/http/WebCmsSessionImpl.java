package org.argeo.cms.internal.http;

import java.util.Locale;

import javax.security.auth.Subject;

import org.argeo.cms.auth.HttpRequest;
import org.argeo.cms.auth.HttpSession;
import org.argeo.cms.internal.auth.CmsSessionImpl;
import org.osgi.service.useradmin.Authorization;

/** CMS session implementation in a web context. */
public class WebCmsSessionImpl extends CmsSessionImpl {
	private static final long serialVersionUID = -5178883380637048025L;
	private HttpSession httpSession;

	public WebCmsSessionImpl(Subject initialSubject, Authorization authorization, Locale locale,
			HttpRequest request) {
		super(initialSubject, authorization, locale, request.getSession().getId());
		httpSession = request.getSession();
	}

	@Override
	public boolean isValid() {
		if (isClosed())
			return false;
		return httpSession.isValid();
	}

	public static CmsSessionImpl getCmsSession(HttpRequest request) {
		return CmsSessionImpl.getByLocalId(request.getSession().getId());
	}
}
