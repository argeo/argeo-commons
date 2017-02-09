package org.argeo.cms.internal.http;

import javax.security.auth.login.LoginContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class PrivateHttpContext extends DataHttpContext {

	@Override
	protected LoginContext processUnauthorized(HttpServletRequest request, HttpServletResponse response) {
		askForWwwAuth(request, response);
		return null;
	}

}
