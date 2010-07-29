package org.argeo.eclipse.ui;

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ArgeoUiPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.argeo.eclipse.ui";

	private final static String SPRING_OSGI_EXTENDER = "org.springframework.osgi.extender";

	// The shared instance
	private static ArgeoUiPlugin plugin;

	private BundleContext bundleContext;

	/**
	 * The constructor
	 */
	public ArgeoUiPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		bundleContext = context;

		// Make sure that the Spring OSGi extender is started
		Bundle osgiExtBundle = Platform.getBundle(SPRING_OSGI_EXTENDER);
		if (osgiExtBundle != null)
			osgiExtBundle.start();
		else
			throw new Exception("Spring OSGi Extender not found");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static ArgeoUiPlugin getDefault() {
		return plugin;
	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}

}
