package org.argeo.cms.ui.workbench.osgi;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/** Label provider showing the sate of bundles */
class StateLabelProvider extends ColumnLabelProvider {
	private static final long serialVersionUID = -7885583135316000733L;

	@Override
	public Image getImage(Object element) {
		int state;
		if (element instanceof Bundle)
			state = ((Bundle) element).getState();
		else if (element instanceof BundleNode)
			state = ((BundleNode) element).getBundle().getState();
		else if (element instanceof ServiceReferenceNode)
			if (((ServiceReferenceNode) element).isPublished())
				return OsgiExplorerImages.SERVICE_PUBLISHED;
			else
				return OsgiExplorerImages.SERVICE_REFERENCED;
		else
			return null;

		switch (state) {
		case Bundle.UNINSTALLED:
			return OsgiExplorerImages.INSTALLED;
		case Bundle.INSTALLED:
			return OsgiExplorerImages.INSTALLED;
		case Bundle.RESOLVED:
			return OsgiExplorerImages.RESOLVED;
		case Bundle.STARTING:
			return OsgiExplorerImages.STARTING;
		case Bundle.STOPPING:
			return OsgiExplorerImages.STARTING;
		case Bundle.ACTIVE:
			return OsgiExplorerImages.ACTIVE;
		default:
			return null;
		}
	}

	@Override
	public String getText(Object element) {
		return null;
	}

	@Override
	public String getToolTipText(Object element) {
		Bundle bundle = (Bundle) element;
		Integer state = bundle.getState();
		switch (state) {
		case Bundle.UNINSTALLED:
			return "UNINSTALLED";
		case Bundle.INSTALLED:
			return "INSTALLED";
		case Bundle.RESOLVED:
			return "RESOLVED";
		case Bundle.STARTING:
			String activationPolicy = bundle.getHeaders()
					.get(Constants.BUNDLE_ACTIVATIONPOLICY).toString();

			// .get("Bundle-ActivationPolicy").toString();
			// FIXME constant triggers the compilation failure
			if (activationPolicy != null
					&& activationPolicy.equals(Constants.ACTIVATION_LAZY))
				// && activationPolicy.equals("lazy"))
				// FIXME constant triggers the compilation failure
				// && activationPolicy.equals(Constants.ACTIVATION_LAZY))
				return "<<LAZY>>";
			return "STARTING";
		case Bundle.STOPPING:
			return "STOPPING";
		case Bundle.ACTIVE:
			return "ACTIVE";
		default:
			return null;
		}
	}
}