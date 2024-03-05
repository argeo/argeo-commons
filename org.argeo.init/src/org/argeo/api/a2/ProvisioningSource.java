package org.argeo.api.a2;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

/** Where components are retrieved from. */
public interface ProvisioningSource {
	/** List all contributions of this source. */
	Iterable<A2Contribution> listContributions(Object filter);

	/** Install a module in the OSGi runtime. */
	Bundle install(BundleContext bc, A2Module module);

	/** Update a module in the OSGi runtime. */
	void update(Bundle bundle, A2Module module);

	/** Finds the {@link A2Branch} related to this component and version. */
	A2Branch findBranch(String componentId, Version version);

}
