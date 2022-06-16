package org.argeo.cms.internal.http;

import java.util.Locale;
import java.util.UUID;

import javax.security.auth.Subject;

import org.argeo.cms.auth.RemoteAuthRequest;
import org.argeo.cms.auth.RemoteAuthSession;
import org.argeo.cms.internal.auth.CmsSessionImpl;
import org.osgi.service.useradmin.Authorization;

/** CMS session implementation in a web context. */
public class WebCmsSessionImpl extends CmsSessionImpl {
	private static final long serialVersionUID = -5178883380637048025L;
	private RemoteAuthSession httpSession;

	public WebCmsSessionImpl(UUID uuid, Subject initialSubject, Authorization authorization, Locale locale,
			RemoteAuthRequest request) {
		super(uuid, initialSubject, authorization, locale, request.getSession().getId());
		httpSession = request.getSession();
	}

	@Override
	public boolean isValid() {
		if (isClosed())
			return false;
		return httpSession.isValid();
	}
}
