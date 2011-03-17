/*
 * Copyright (C) 2010 Mathieu Baudier <mbaudier@argeo.org>
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

import org.apache.activemq.broker.BrokerPluginSupport;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.command.ConnectionInfo;
import org.argeo.ArgeoException;
import org.argeo.security.core.InternalAuthentication;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;

@SuppressWarnings("unchecked")
public class ActiveMqSecurityBrokerPlugin extends BrokerPluginSupport {
//	private final static Log log = LogFactory
//			.getLog(ActiveMqSecurityBrokerPlugin.class);

	private AuthenticationManager authenticationManager;
	private String systemUsername = InternalAuthentication.DEFAULT_SYSTEM_USERNAME;
	private String systemRole = InternalAuthentication.DEFAULT_SYSTEM_ROLE;

	@Override
	public void addConnection(ConnectionContext context, ConnectionInfo info)
			throws Exception {
		String username = info.getUserName();
		if (username == null)
			throw new ArgeoException("No user name provided");
		String password = info.getPassword();
		if (password == null) {
			password = context.getConnection().getRemoteAddress().substring(1);
			password = password.substring(0, password.lastIndexOf(':'));
		}

		SecurityContext securityContext = SecurityContextHolder.getContext();

		final Authentication authRequest;
		if (username.equals(systemUsername))
			authRequest = new InternalAuthentication(password, username,
					systemRole);
		else
			authRequest = new UsernamePasswordAuthenticationToken(username,
					password);

		final Authentication auth = authenticationManager
				.authenticate(authRequest);
		securityContext.setAuthentication(auth);
		context.setSecurityContext(new ActiveMqSpringSecurityContext(
				securityContext));

		super.addConnection(context, info);
	}

	public void setAuthenticationManager(
			AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	public void setSystemUsername(String systemUsername) {
		this.systemUsername = systemUsername;
	}

	public void setSystemRole(String systemRole) {
		this.systemRole = systemRole;
	}

}
