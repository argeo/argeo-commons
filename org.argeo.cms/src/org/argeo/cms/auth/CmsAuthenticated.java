package org.argeo.cms.auth;

import javax.security.auth.Subject;

public interface CmsAuthenticated {
	String KEY = "org.argeo.cms.authenticated";

	Subject getSubject();
//	LoginContext getLoginContext();

}
