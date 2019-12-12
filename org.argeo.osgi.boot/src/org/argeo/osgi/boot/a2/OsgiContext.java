package org.argeo.osgi.boot.a2;

import org.argeo.osgi.boot.OsgiBootUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

/** A running OSGi bundle context seen as a {@link ProvisioningSource}. */
class OsgiContext extends ProvisioningSource {
	private final BundleContext bc;

	public OsgiContext(BundleContext bc) {
		super();
		this.bc = bc;
	}

	public OsgiContext() {
		Bundle bundle = FrameworkUtil.getBundle(OsgiContext.class);
		if (bundle == null)
			throw new IllegalArgumentException(
					"OSGi Boot bundle must be started or a bundle context must be specified");
		this.bc = bundle.getBundleContext();
	}

	void load() {
		A2Contribution runtimeContribution = new A2Contribution(this, A2Contribution.RUNTIME);
		for (Bundle bundle : bc.getBundles()) {
			// OsgiBootUtils.debug(bundle.getDataFile("/"));
			String componentId = bundle.getSymbolicName();
			Version version = bundle.getVersion();
			A2Component component = runtimeContribution.getOrAddComponent(componentId);
			A2Module module = component.getOrAddModule(version, bundle);
			if (OsgiBootUtils.isDebug())
				OsgiBootUtils.debug("Registered " + module + " (location id: " + bundle.getLocation() + ")");
		}

	}
}
