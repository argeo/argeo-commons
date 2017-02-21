package org.argeo.cms.internal.http;

import javax.security.auth.login.LoginContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Requests authorisation */
class PrivateHttpContext extends DataHttpContext {

	public PrivateHttpContext(String httpAuthrealm, boolean forceBasic) {
		super(httpAuthrealm, forceBasic);
	}

	public PrivateHttpContext(String httpAuthrealm) {
		super(httpAuthrealm);
	}

	@Override
	protected LoginContext processUnauthorized(HttpServletRequest request, HttpServletResponse response) {
		askForWwwAuth(request, response);
		return null;
	}

}
