package org.argeo.security.core;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.providers.AuthenticationProvider;
import org.springframework.security.providers.ProviderManager;

public class ArgeoAuthenticationManager extends ProviderManager {
	private Log log = LogFactory.getLog(ArgeoAuthenticationManager.class);

	@SuppressWarnings("unchecked")
	public void register(AuthenticationProvider authenticationProvider,
			Map<String, String> parameters) {
		getProviders().add(authenticationProvider);
		if (log.isDebugEnabled())
			log.debug("Registered authentication provider " + parameters);
	}

	public void unregister(AuthenticationProvider authenticationProvider,
			Map<String, String> parameters) {
		getProviders().remove(authenticationProvider);
		if (log.isDebugEnabled())
			log.debug("Unregistered authentication provider " + parameters);
	}

}
