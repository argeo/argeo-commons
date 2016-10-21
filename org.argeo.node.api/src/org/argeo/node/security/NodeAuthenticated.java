package org.argeo.node.security;

import javax.security.auth.login.LoginContext;

public interface NodeAuthenticated {
	String KEY = "org.argeo.node.authenticated";

//	Subject getSubject();
	LoginContext getLoginContext();

}
