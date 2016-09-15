package org.argeo.node;

import javax.security.auth.Subject;

public interface NodeAuthenticated {
	String KEY = "org.argeo.node.authenticated";

	Subject getSubject();

}
