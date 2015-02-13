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
package org.argeo.security.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Maintains a list of authentication providers injected in to a provider
 * manager, in order to avoid issues with OSGi services and use packages.
 */
@Deprecated
public class AuthenticationProvidersRegister implements InitializingBean {
	private Log log = LogFactory.getLog(AuthenticationProvidersRegister.class);

	private List<Object> providers = new ArrayList<Object>();
	private List<Object> defaultProviders = new ArrayList<Object>();

	public void register(Object authenticationProvider,
			Map<String, String> parameters) {
		providers.add(authenticationProvider);
		if (log.isTraceEnabled())
			log.trace("Registered authentication provider " + parameters);
	}

	public void unregister(Object authenticationProvider,
			Map<String, String> parameters) {
		providers.remove(authenticationProvider);
		if (log.isTraceEnabled())
			log.trace("Unregistered authentication provider " + parameters);
	}

	public List<Object> getProviders() {
		return providers;
	}

	public void setDefaultProviders(
			List<Object> defaultProviders) {
		this.defaultProviders = defaultProviders;
	}

	public void afterPropertiesSet() throws Exception {
		providers.addAll(defaultProviders);
	}

}
