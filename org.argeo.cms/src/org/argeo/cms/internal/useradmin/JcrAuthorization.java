package org.argeo.cms.internal.useradmin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.argeo.security.jcr.JcrUserDetails;
import org.osgi.service.useradmin.Authorization;
import org.springframework.security.core.GrantedAuthority;

class JcrAuthorization implements Authorization {
	private final String name;
	private final List<String> roles;

	public JcrAuthorization(JcrUserDetails userDetails) {
		this.name = userDetails.getUsername();
		List<String> t = new ArrayList<String>();
		for (GrantedAuthority ga : userDetails.getAuthorities()) {
			t.add(ga.getAuthority());
		}
		roles = Collections.unmodifiableList(t);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean hasRole(String name) {
		return roles.contains(name);
	}

	@Override
	public String[] getRoles() {
		return roles.toArray(new String[roles.size()]);
	}

}
