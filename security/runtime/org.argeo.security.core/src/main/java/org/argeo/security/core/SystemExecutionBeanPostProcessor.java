package org.argeo.security.core;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Executes with a system authentication the initialization methods of the
 * application context where it has been defined.
 */
public class SystemExecutionBeanPostProcessor extends
		AbstractSystemExecution implements BeanPostProcessor {

	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		authenticateAsSystem();
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		// NOTE: in case there was an exception in on the initialization method
		// we expect the underlying thread to die and thus the system
		// authentication to be lost. We have currently no way to catch the
		// exception and perform the deauthentication by ourselves.
		deauthenticateAsSystem();
		return bean;
	}

}
