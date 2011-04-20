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
public class SystemExecutionBeanPostProcessor extends AbstractSystemExecution
		implements InstantiationAwareBeanPostProcessor, ApplicationListener {

	@SuppressWarnings("rawtypes")
	public Object postProcessBeforeInstantiation(Class beanClass,
			String beanName) throws BeansException {
		authenticateAsSystem();
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

	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContextRefreshedEvent) {
			// make sure that we have deauthenticated after the application
			// context was initialized/refreshed
			deauthenticateAsSystem();
		}
	}

}
