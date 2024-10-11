package org.argeo.cms.jakarta.websocket.server;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import org.osgi.service.useradmin.Role;

/**
 * Abstraction of a single Frontend view, that is a web browser page. There can
 * be multiple views within one single authenticated HTTP session.
 */
public class WebSocketView {
	private final String uid;
	private Subject subject;

	public WebSocketView(Subject subject) {
		this.uid = UUID.randomUUID().toString();
		this.subject = subject;
	}

	public String getUid() {
		return uid;
	}

	public Set<String> getRoles() {
		return roles(subject);
	}

	public boolean isInRole(String role) {
		return getRoles().contains(role);
	}

	public void checkRole(String role) {
		checkRole(subject, role);
	}

	public final static Set<String> roles(Subject subject) {
		Set<String> roles = new HashSet<String>();
		X500Principal principal = subject.getPrincipals(X500Principal.class).iterator().next();
		String username = principal.getName();
		roles.add(username);
		for (Principal group : subject.getPrincipals()) {
			if (group instanceof Role)
				roles.add(group.getName());
		}
		return roles;
	}

	public static void checkRole(Subject subject, String role) {
		Set<String> roles = roles(subject);
		if (!roles.contains(role))
			throw new IllegalStateException("User is not in role " + role);
	}

}
