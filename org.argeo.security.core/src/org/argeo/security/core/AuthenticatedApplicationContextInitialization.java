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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.Subject;

import org.eclipse.gemini.blueprint.context.DependencyInitializationAwareBeanPostProcessor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.SecurityContextProvider;
import org.springframework.beans.factory.support.SimpleSecurityContextProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Executes with a system authentication the instantiation and initialization
 * methods of the application context where it has been defined.
 */
public class AuthenticatedApplicationContextInitialization extends
		AbstractSystemExecution implements
		DependencyInitializationAwareBeanPostProcessor, ApplicationContextAware {
	/** If non empty, restricts to these beans */
	private List<String> beanNames = new ArrayList<String>();

	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		if (beanNames.size() == 0 || beanNames.contains(beanName))
			authenticateAsSystem();
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		if (beanNames.size() == 0 || beanNames.contains(beanName))
			deauthenticateAsSystem();
		return bean;
	}

	public void setBeanNames(List<String> beanNames) {
		this.beanNames = beanNames;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		if (applicationContext.getAutowireCapableBeanFactory() instanceof AbstractBeanFactory) {
			final AbstractBeanFactory beanFactory = ((AbstractBeanFactory) applicationContext
					.getAutowireCapableBeanFactory());
			// retrieve subject's access control context
			// and set it as the bean factory security context
			Subject.doAs(getSubject(), new PrivilegedAction<Void>() {
				@Override
				public Void run() {
					SecurityContextProvider scp = new SimpleSecurityContextProvider(
							AccessController.getContext());
					beanFactory.setSecurityContextProvider(scp);
					return null;
				}
			});
		}
	}
}
