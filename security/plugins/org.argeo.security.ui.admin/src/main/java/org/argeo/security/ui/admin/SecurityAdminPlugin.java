package org.argeo.security.ui.admin;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class SecurityAdminPlugin extends AbstractUIPlugin {
	public static final String PLUGIN_ID = "org.argeo.security.ui.admin"; //$NON-NLS-1$
	private static SecurityAdminPlugin plugin;

	public SecurityAdminPlugin() {
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static SecurityAdminPlugin getDefault() {
		return plugin;
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

}
