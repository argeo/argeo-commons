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

	private HomeRepository homeRepository;

	public CmsInstance() {
		initTrackers();
	}

	private void initTrackers() {
		// node repository
		new ServiceTracker<Repository, Repository>(bc, Repository.class, null) {
			@Override
			public Repository addingService(ServiceReference<Repository> reference) {
				Object cn = reference.getProperty(NodeConstants.CN);
				if (cn != null && cn.equals(NodeConstants.HOME)) {
					homeRepository = (HomeRepository) bc.getService(reference);
					if (log.isDebugEnabled())
						log.debug("Home repository is available");
				}
				return super.addingService(reference);
			}

			@Override
			public void removedService(ServiceReference<Repository> reference, Repository service) {
				super.removedService(reference, service);
				homeRepository = null;
			}

		}.open();
	}

	public void shutdown() {

	}

	@Override
	public void createWorkgroup(LdapName dn) {
		if (homeRepository == null)
			throw new CmsException("Home repository is not available");
		// TODO add check that the group exists
		homeRepository.createWorkgroup(dn);
	}

}
