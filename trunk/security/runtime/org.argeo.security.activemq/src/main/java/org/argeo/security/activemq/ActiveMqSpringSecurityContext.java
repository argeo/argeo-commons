/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.security.activemq;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContext;

/** An ActiveMQ security context compatible with Spring Security. */
public class ActiveMqSpringSecurityContext extends
		org.apache.activemq.security.SecurityContext {

	private final SecurityContext springSecurityContext;

	public ActiveMqSpringSecurityContext(SecurityContext springSecurityContext) {
		super(springSecurityContext.getAuthentication().getName());
		this.springSecurityContext = springSecurityContext;
	}

	@Override
	public Set<?> getPrincipals() {
		return new HashSet<GrantedAuthority>(
				Arrays.asList(springSecurityContext.getAuthentication()
						.getAuthorities()));
	}

	public SecurityContext getSpringSecurityContext() {
		return springSecurityContext;
	}

}
