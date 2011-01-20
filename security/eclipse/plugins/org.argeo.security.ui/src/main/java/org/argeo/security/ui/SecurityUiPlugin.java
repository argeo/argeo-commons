package org.argeo.security.ui;

import org.argeo.ArgeoException;
import org.argeo.security.ArgeoUser;
import org.argeo.security.UserNature;
import org.argeo.security.nature.SimpleUserNature;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class SecurityUiPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.argeo.security.ui"; //$NON-NLS-1$

	// The shared instance
	private static SecurityUiPlugin plugin;

	/**
	 * The constructor
	 */
	public SecurityUiPlugin() {
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
	public static SecurityUiPlugin getDefault() {
		return plugin;
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	/*
	 * SECURITY UTILITIES
	 */
	public final static SimpleUserNature findSimpleUserNature(ArgeoUser user,
			String simpleNatureType) {
		SimpleUserNature simpleNature = null;
		if (simpleNatureType != null)
			simpleNature = (SimpleUserNature) user.getUserNatures().get(
					simpleNatureType);
		else
			for (UserNature userNature : user.getUserNatures().values())
				if (userNature instanceof SimpleUserNature)
					simpleNature = (SimpleUserNature) userNature;

		if (simpleNature == null)
			throw new ArgeoException("No simple user nature in user " + user);
		return simpleNature;
	}
}
