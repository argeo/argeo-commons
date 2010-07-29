package org.argeo.eclipse.spring;

import org.argeo.eclipse.ui.ArgeoUiPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.springframework.context.ApplicationContext;

/**
 * The Spring Extension Factory builds a bridge between the Eclipse Extension
 * Registry and the Spring Framework (especially Spring Dynamic Modules).
 * 
 * It allows you to define your extension as a spring bean within the spring
 * application context of your bundle. If you would like to use this bean as an
 * instance of an extension (an Eclipse RCP view, for example) you define the
 * extension with this spring extension factory as the class to be created.
 * 
 * To let the spring extension factory pick the right bean from your application
 * context you need to set the bean id to the same value as the id of the view
 * within the view definition, for example. This is important if your extension
 * definition contains more than one element, where each element has its own id.
 * 
 * If the extension definition elements themselves have no id attribute the
 * spring extension factory uses the id of the extension itself to identify the
 * bean.
 * 
 * @author Martin Lippert
 */
public class SpringExtensionFactory implements IExecutableExtensionFactory,
		IExecutableExtension {

	private Object bean;

	public Object create() throws CoreException {
		return bean;
	}

	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
		String beanName = getBeanName(data, config);
		ApplicationContext appContext = getApplicationContext(config);

		if (beanName != null && appContext != null) {
			this.bean = appContext.getBean(beanName);
			if (this.bean instanceof IExecutableExtension) {
				((IExecutableExtension) this.bean).setInitializationData(
						config, propertyName, data);
			}
		}
	}

	private String getBeanName(Object data, IConfigurationElement config) {
		
		// try the specific bean id the extension defines
		if (data != null && data.toString().length() > 0) {
			return data.toString();
		}

		// try the id of the config element
		if (config.getAttribute("id") != null) {
			return config.getAttribute("id");
		}

		// try the id of the extension element itself
		if (config.getParent() != null
				&& config.getParent() instanceof IExtension) {
			IExtension extensionDefinition = (IExtension) config.getParent();
			return extensionDefinition.getSimpleIdentifier();
		}

		return null;
	}

	private ApplicationContext getApplicationContext(
			IConfigurationElement config) {
		String contributorName = config.getContributor().getName();
		Bundle contributorBundle = Platform.getBundle(contributorName);

		if (contributorBundle.getState() != Bundle.ACTIVE && contributorBundle.getState() != Bundle.STARTING) {
			try {
				System.out.println("starting bundle: " + contributorBundle.getSymbolicName());
				contributorBundle.start();
			} catch (BundleException e) {
				e.printStackTrace();
			}
		}

		final ApplicationContextTracker applicationContextTracker = new ApplicationContextTracker(
				contributorBundle, ArgeoUiPlugin.getDefault().getBundleContext());
		ApplicationContext applicationContext = null;
		try {
			applicationContext = applicationContextTracker
					.getApplicationContext();
		} finally {
			applicationContextTracker.close();
		}
		return applicationContext;
	}

}
