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

import java.beans.PropertyDescriptor;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Executes with a system authentication the instantiation and initialization
 * methods of the application context where it has been defined.
 */
public class AuthenticatedApplicationContextInitialization extends
		AbstractSystemExecution implements InstantiationAwareBeanPostProcessor,
		ApplicationListener {
	// private Log log = LogFactory
	// .getLog(AuthenticatedApplicationContextInitialization.class);

	@SuppressWarnings("rawtypes")
	public Object postProcessBeforeInstantiation(Class beanClass,
			String beanName) throws BeansException {
		// we authenticate when any bean is instantiated
		// we will deauthenticate only when the application context has been
		// refreshed in order to be able to deal with factory beans has well
		if (!isAuthenticatedBySelf()) {
			authenticateAsSystem();
		}
		return null;
	}

	public boolean postProcessAfterInstantiation(Object bean, String beanName)
			throws BeansException {
		return true;
	}

	public PropertyValues postProcessPropertyValues(PropertyValues pvs,
			PropertyDescriptor[] pds, Object bean, String beanName)
			throws BeansException {
		return pvs;
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		// authenticateAsSystem();
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		// NOTE: in case there was an exception in on the initialization method
		// we expect the underlying thread to die and thus the system
		// authentication to be lost. We have currently no way to catch the
		// exception and perform the deauthentication by ourselves.
		// deauthenticateAsSystem();
		return bean;
	}

	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContextRefreshedEvent) {
			// make sure that we have deauthenticated after the application
			// context was initialized/refreshed
			// deauthenticateAsSystem();
		}
	}

}
