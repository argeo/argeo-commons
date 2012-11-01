package org.argeo.security.jcr;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 * Manages data expected by the Argeo security model, such as user home and
 * profile.
 */
public interface JcrSecurityModel {
	/**
	 * To be called before user details are loaded. Make sure than any logged in
	 * user has a home directory with full access and a profile with information
	 * about him (read access)
	 * 
	 * @return the user profile (whose parent is the user home), never null
	 */
	public Node sync(Session session, String username, List<String> roles);
}
