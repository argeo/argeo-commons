package org.argeo.cms.ui.internal;

import org.argeo.api.NodeState;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

	// avoid dependency to RWT OSGi
	private final static String CONTEXT_NAME_PROP = "contextName";

	private static ServiceTracker<NodeState, NodeState> nodeState;

	// @Override
	public void start(BundleContext bc) throws Exception {
		// UI
//		bc.registerService(ApplicationConfiguration.class, new MaintenanceUi(),
//				LangUtils.dico(CONTEXT_NAME_PROP, "system"));
//		bc.registerService(ApplicationConfiguration.class, new UserUi(), LangUtils.dico(CONTEXT_NAME_PROP, "user"));

		nodeState = new ServiceTracker<>(bc, NodeState.class, null);
		nodeState.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (nodeState != null) {
			nodeState.close();
			nodeState = null;
		}
	}

	public static NodeState getNodeState() {
		return nodeState.getService();
	}
}
