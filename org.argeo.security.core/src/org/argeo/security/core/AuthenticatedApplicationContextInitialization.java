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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.Subject;

import org.eclipse.gemini.blueprint.context.DependencyInitializationAwareBeanPostProcessor;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.SecurityContextProvider;
import org.springframework.beans.factory.support.SimpleSecurityContextProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Executes with a system authentication the instantiation and initialization
 * methods of the application context where it has been defined.
 */
public class AuthenticatedApplicationContextInitialization extends
		AbstractSystemExecution implements DependencyInitializationAwareBeanPostProcessor,
		ApplicationListener<ApplicationEvent>, ApplicationContextAware {
	// private Log log = LogFactory
	// .getLog(AuthenticatedApplicationContextInitialization.class);
	/** If non empty, restricts to these beans */
	private List<String> beanNames = new ArrayList<String>();

//	@SuppressWarnings("rawtypes")
//	public Object postProcessBeforeInstantiation(Class beanClass,
//			String beanName) throws BeansException {
//		// we authenticate when any bean is instantiated
//		// we will deauthenticate only when the application context has been
//		// refreshed in order to be able to deal with factory beans has well
//		// if (!isAuthenticatedBySelf()) {
//		// if (beanNames.size() == 0)
//		// authenticateAsSystem();
//		// else if (beanNames.contains(beanName))
//		// authenticateAsSystem();
//		// }
//		return null;
//	}
//
//	public boolean postProcessAfterInstantiation(Object bean, String beanName)
//			throws BeansException {
//		return true;
//	}
//
//	public PropertyValues postProcessPropertyValues(PropertyValues pvs,
//			PropertyDescriptor[] pds, Object bean, String beanName)
//			throws BeansException {
//		return pvs;
//	}

	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		if (beanNames.size() == 0 || beanNames.contains(beanName))
			authenticateAsSystem();
		// try {
		// if (beanNames.size() == 0 || beanNames.contains(beanName)) {
		// LoginContext lc = new LoginContext("INIT", subject);
		// lc.login();
		// }
		// } catch (LoginException e) {
		// throw new ArgeoException("Cannot login as initialization", e);
		// }
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		// NOTE: in case there was an exception in on the initialization method
		// we expect the underlying thread to die and thus the system
		// authentication to be lost. We have currently no way to catch the
		// exception and perform the deauthentication by ourselves.
		if (beanNames.size() == 0 || beanNames.contains(beanName))
			deauthenticateAsSystem();
		// try {
		// if (beanNames.size() == 0 || beanNames.contains(beanName)) {
		// LoginContext lc = new LoginContext("INIT", subject);
		// lc.logout();
		// }
		// } catch (LoginException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		return bean;
	}

	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContextRefreshedEvent) {
			// make sure that we have deauthenticated after the application
			// context was initialized/refreshed
			// deauthenticateAsSystem();
		}
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
