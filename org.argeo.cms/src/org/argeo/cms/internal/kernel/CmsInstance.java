package org.argeo.cms.internal.kernel;

import javax.jcr.Repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.node.NodeConstants;
import org.argeo.node.NodeInstance;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class CmsInstance implements NodeInstance {
	private final Log log = LogFactory.getLog(getClass());
	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();


	public CmsInstance() {
		initTrackers();
	}

	private void initTrackers() {
		// node repository
		new ServiceTracker<Repository, Repository>(bc, Repository.class, null) {
			@Override
			public Repository addingService(ServiceReference<Repository> reference) {
				Object cn = reference.getProperty(NodeConstants.CN);
				if (cn != null && cn.equals(NodeConstants.ALIAS_NODE)) {
					if (log.isDebugEnabled())
						log.debug("Node repository is available");
				}
				return super.addingService(reference);
			}
		}.open();
	}

	public void shutdown() {

	}

}
