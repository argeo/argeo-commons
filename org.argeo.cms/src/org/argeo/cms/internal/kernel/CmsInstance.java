package org.argeo.cms.internal.kernel;

import javax.jcr.Repository;
import javax.naming.ldap.LdapName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.node.NodeConstants;
import org.argeo.node.NodeInstance;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class CmsInstance implements NodeInstance {
	private final Log log = LogFactory.getLog(getClass());
	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

	private EgoRepository egoRepository;

	public CmsInstance() {
		initTrackers();
	}

	private void initTrackers() {
		// node repository
		new ServiceTracker<Repository, Repository>(bc, Repository.class, null) {
			@Override
			public Repository addingService(ServiceReference<Repository> reference) {
				Object cn = reference.getProperty(NodeConstants.CN);
				if (cn != null && cn.equals(NodeConstants.EGO)) {
					egoRepository = (EgoRepository) bc.getService(reference);
					if (log.isTraceEnabled())
						log.trace("Home repository is available");
				}
				return super.addingService(reference);
			}

			@Override
			public void removedService(ServiceReference<Repository> reference, Repository service) {
				super.removedService(reference, service);
				egoRepository = null;
			}

		}.open();
	}

	public void shutdown() {

	}

	@Override
	public void createWorkgroup(LdapName dn) {
		if (egoRepository == null)
			throw new CmsException("Ego repository is not available");
		// TODO add check that the group exists
		egoRepository.createWorkgroup(dn);
	}

}
