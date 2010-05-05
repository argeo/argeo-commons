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
