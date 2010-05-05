package org.argeo.security.activemq;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContext;

public class ActiveMqSpringSecurityContext extends
		org.apache.activemq.security.SecurityContext {

	private final SecurityContext springSecurityContext;

	public ActiveMqSpringSecurityContext(SecurityContext springSecurityContext) {
		super(springSecurityContext.getAuthentication().getName());
		this.springSecurityContext = springSecurityContext;
	}

	@Override
	public Set<?> getPrincipals() {
		return new HashSet<GrantedAuthority>(Arrays
				.asList(springSecurityContext.getAuthentication()
						.getAuthorities()));
	}

	public SecurityContext getSpringSecurityContext() {
		return springSecurityContext;
	}

}
