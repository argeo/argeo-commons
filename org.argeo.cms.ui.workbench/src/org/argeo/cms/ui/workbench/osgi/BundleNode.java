package org.argeo.cms.ui.workbench.osgi;

import org.argeo.eclipse.ui.TreeParent;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/** A tree element representing a {@link Bundle} */
class BundleNode extends TreeParent {
	private final Bundle bundle;

	public BundleNode(Bundle bundle) {
		this(bundle, false);
	}

	@SuppressWarnings("rawtypes")
	public BundleNode(Bundle bundle, boolean hasChildren) {
		super(bundle.getSymbolicName());
		this.bundle = bundle;

		if (hasChildren) {
			// REFERENCES
			ServiceReference[] usedServices = bundle.getServicesInUse();
			if (usedServices != null) {
				for (ServiceReference sr : usedServices) {
					if (sr != null)
						addChild(new ServiceReferenceNode(sr, false));
				}
			}

			// SERVICES
			ServiceReference[] registeredServices = bundle
					.getRegisteredServices();
			if (registeredServices != null) {
				for (ServiceReference sr : registeredServices) {
					if (sr != null)
						addChild(new ServiceReferenceNode(sr, true));
				}
			}
		}

	}

	Bundle getBundle() {
		return bundle;
	}
}
