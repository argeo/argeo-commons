package org.argeo.cms.ui.theme;

import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class CmsImages {
	private static BundleContext themeBc = FrameworkUtil.getBundle(CmsImages.class).getBundleContext();

	final public static String ICONS_BASE = "icons/";
	final public static String TYPES_BASE = ICONS_BASE + "types/";
	final public static String ACTIONS_BASE = ICONS_BASE + "actions/";

	public static Image createIcon(String name) {
		return createImg(CmsImages.ICONS_BASE + name);
	}

	public static Image createAction(String name) {
		return createImg(CmsImages.ACTIONS_BASE + name);
	}

	public static Image createType(String name) {
		return createImg(CmsImages.TYPES_BASE + name);
	}

	public static Image createImg(String name) {
		return CmsImages.createDesc(name).createImage(Display.getDefault());
	}

	public static ImageDescriptor createDesc(String name) {
		return createDesc(themeBc, name);
	}

	public static ImageDescriptor createDesc(BundleContext bc, String name) {
		URL url = bc.getBundle().getResource(name);
		if (url == null)
			return ImageDescriptor.getMissingImageDescriptor();
		return ImageDescriptor.createFromURL(url);
	}

	public static Image createImg(BundleContext bc, String name) {
		return createDesc(bc, name).createImage(Display.getDefault());
	}

}
