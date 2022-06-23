package org.argeo.cms.e4.monitoring;

import org.argeo.cms.ux.widgets.TreeParent;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/** A tree element representing a {@link ServiceReference} */
@SuppressWarnings({ "rawtypes" })
class ServiceReferenceNode extends TreeParent {
	private final ServiceReference serviceReference;
	private final boolean published;

	public ServiceReferenceNode(ServiceReference serviceReference,
			boolean published) {
		super(serviceReference.toString());
		this.serviceReference = serviceReference;
		this.published = published;

		if (isPublished()) {
			Bundle[] usedBundles = serviceReference.getUsingBundles();
			if (usedBundles != null) {
				for (Bundle b : usedBundles) {
					if (b != null)
						addChild(new BundleNode(b));
				}
			}
		} else {
			Bundle provider = serviceReference.getBundle();
			addChild(new BundleNode(provider));
		}

		for (String key : serviceReference.getPropertyKeys()) {
			addChild(new TreeParent(key + "="
					+ serviceReference.getProperty(key)));
		}

	}

	public ServiceReference getServiceReference() {
		return serviceReference;
	}

	public boolean isPublished() {
		return published;
	}
}
