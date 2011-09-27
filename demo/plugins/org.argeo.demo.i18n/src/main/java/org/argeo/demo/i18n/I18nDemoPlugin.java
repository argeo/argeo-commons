package org.argeo.demo.i18n;

import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class I18nDemoPlugin extends AbstractUIPlugin {
	private final static Log log = LogFactory.getLog(I18nDemoPlugin.class);
	private ResourceBundle messages;

	// The plug-in ID
	public static final String ID = "org.argeo.demo.i18n"; //$NON-NLS-1$

	// The shared instance
	private static I18nDemoPlugin plugin;

	/**
	 * The constructor
	 */
	public I18nDemoPlugin() {
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
		messages = ResourceBundle.getBundle("org.argeo.demo.i18n.messages");
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
	public static I18nDemoPlugin getDefault() {
		return plugin;
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(ID, path);
	}

	/** Returns the internationalized label for the given key */
	public static String getMessage(String key) {
		try {
			return getDefault().messages.getString(key);
		} catch (NullPointerException npe) {
			log.warn(key + " not found.");
			return key;
		}
	}

	/**
	 * Gives access to the internationalization message bundle. Returns null in
	 * case the ClientUiPlugin is not started (for JUnit tests, by instance)
	 */
	public static ResourceBundle getMessagesBundle() {
		if (getDefault() != null)
			// To avoid NPE
			return getDefault().messages;
		else
			return null;
	}
}