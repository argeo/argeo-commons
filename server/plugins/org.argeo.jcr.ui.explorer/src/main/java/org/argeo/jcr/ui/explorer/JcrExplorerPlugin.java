package org.argeo.jcr.ui.explorer;

import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class JcrExplorerPlugin extends AbstractUIPlugin {
	private final static Log log = LogFactory.getLog(JcrExplorerPlugin.class);
	private ResourceBundle messages;

	// The plug-in ID
	public static final String ID = "org.argeo.jcr.ui.explorer"; //$NON-NLS-1$

	// The shared instance
	private static JcrExplorerPlugin plugin;

	/**
	 * The constructor
	 */
	public JcrExplorerPlugin() {
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
		messages = ResourceBundle
				.getBundle("org.argeo.jcr.ui.explorer.messages");

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
	public static JcrExplorerPlugin getDefault() {
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
