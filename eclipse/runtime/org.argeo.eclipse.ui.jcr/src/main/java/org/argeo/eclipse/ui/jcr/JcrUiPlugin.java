package org.argeo.eclipse.ui.jcr;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class JcrUiPlugin extends AbstractUIPlugin {
	public final static String ID = "org.argeo.eclipse.ui.jcr";

	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(ID, path);
	}

}
