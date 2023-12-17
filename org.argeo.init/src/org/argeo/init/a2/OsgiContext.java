package org.argeo.init.a2;

import org.argeo.init.osgi.OsgiBootUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

/**
 * A running OSGi bundle context seen as a {@link AbstractProvisioningSource}.
 */
class OsgiContext extends AbstractProvisioningSource {
	private final BundleContext bc;

	private A2Contribution runtimeContribution;

	public OsgiContext(BundleContext bc) {
		super(false);
		this.bc = bc;
		runtimeContribution = getOrAddContribution(A2Contribution.RUNTIME);
	}

	public OsgiContext() {
		super(false);
		Bundle bundle = FrameworkUtil.getBundle(OsgiContext.class);
		if (bundle == null)
			throw new IllegalArgumentException(
					"OSGi Boot bundle must be started or a bundle context must be specified");
		this.bc = bundle.getBundleContext();
	}

	void load() {
		for (Bundle bundle : bc.getBundles()) {
			registerBundle(bundle);
		}

	}

	void registerBundle(Bundle bundle) {
		String componentId = bundle.getSymbolicName();
		Version version = bundle.getVersion();
		A2Component component = runtimeContribution.getOrAddComponent(componentId);
		A2Module module = component.getOrAddModule(version, bundle);
		if (OsgiBootUtils.isDebug())
			OsgiBootUtils.debug("Registered bundle module " + module + " (location id: " + bundle.getLocation() + ")");

	}
}
