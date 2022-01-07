package org.argeo.cms.internal.kernel;

import org.argeo.api.cms.CmsContext;
import org.argeo.api.cms.CmsLog;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class CmsContextImpl implements CmsContext {
	private final CmsLog log = CmsLog.getLog(getClass());
	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

//	private EgoRepository egoRepository;

	public CmsContextImpl() {
		initTrackers();
	}

	private void initTrackers() {
		// node repository
//		new ServiceTracker<Repository, Repository>(bc, Repository.class, null) {
//			@Override
//			public Repository addingService(ServiceReference<Repository> reference) {
//				Object cn = reference.getProperty(NodeConstants.CN);
//				if (cn != null && cn.equals(NodeConstants.EGO_REPOSITORY)) {
////					egoRepository = (EgoRepository) bc.getService(reference);
//					if (log.isTraceEnabled())
//						log.trace("Home repository is available");
//				}
//				return super.addingService(reference);
//			}
//
//			@Override
//			public void removedService(ServiceReference<Repository> reference, Repository service) {
//				super.removedService(reference, service);
////				egoRepository = null;
//			}
//
//		}.open();
	}

	public void shutdown() {

	}

	@Override
	public void createWorkgroup(String dn) {
//		if (egoRepository == null)
//			throw new CmsException("Ego repository is not available");
//		// TODO add check that the group exists
//		egoRepository.createWorkgroup(dn);
		throw new UnsupportedOperationException();
	}

}
