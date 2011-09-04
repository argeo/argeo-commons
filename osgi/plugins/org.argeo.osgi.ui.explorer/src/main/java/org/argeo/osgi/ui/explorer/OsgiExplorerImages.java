package org.argeo.osgi.ui.explorer;

import org.eclipse.swt.graphics.Image;

/** Shared icons. */
public class OsgiExplorerImages {
	public final static Image INSTALLED = OsgiExplorerPlugin
			.getImageDescriptor("icons/installed.gif").createImage();
	public final static Image RESOLVED = OsgiExplorerPlugin.getImageDescriptor(
			"icons/resolved.gif").createImage();
	public final static Image STARTING = OsgiExplorerPlugin.getImageDescriptor(
			"icons/starting.gif").createImage();
	public final static Image ACTIVE = OsgiExplorerPlugin.getImageDescriptor(
			"icons/active.gif").createImage();
}
