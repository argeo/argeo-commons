package org.argeo.cms.ui.internal;

import org.argeo.cms.CmsStyles;
import org.argeo.cms.maintenance.MaintenanceUi;
import org.argeo.cms.ui.internal.rwt.UserUi;
import org.argeo.node.NodeState;
import org.argeo.util.LangUtils;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
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
		bc.registerService(ApplicationConfiguration.class, new MaintenanceUi(),
				LangUtils.init(CONTEXT_NAME_PROP, "system"));
		bc.registerService(ApplicationConfiguration.class, new UserUi(), LangUtils.init(CONTEXT_NAME_PROP, "user"));

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
