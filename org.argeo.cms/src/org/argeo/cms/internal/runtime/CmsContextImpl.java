package org.argeo.cms.internal.runtime;

import static java.util.Locale.ENGLISH;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsContext;
import org.argeo.api.cms.CmsDeployment;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsState;
import org.argeo.cms.LocaleUtils;
import org.argeo.cms.internal.osgi.NodeUserAdmin;
import org.ietf.jgss.GSSCredential;
import org.osgi.service.useradmin.UserAdmin;

public class CmsContextImpl implements CmsContext {
	private final CmsLog log = CmsLog.getLog(getClass());
//	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

//	private EgoRepository egoRepository;
	private static CompletableFuture<CmsContextImpl> instance = new CompletableFuture<CmsContextImpl>();

	private CmsState cmsState;
	private CmsDeployment cmsDeployment;
	private UserAdmin userAdmin;

	// i18n
	private Locale defaultLocale;
	private List<Locale> locales = null;

	private Long availableSince;

//	public CmsContextImpl() {
//		initTrackers();
//	}

	public void init() {
		Object defaultLocaleValue = KernelUtils.getFrameworkProp(CmsConstants.I18N_DEFAULT_LOCALE);
		defaultLocale = defaultLocaleValue != null ? new Locale(defaultLocaleValue.toString())
				: new Locale(ENGLISH.getLanguage());
		locales = LocaleUtils.asLocaleList(KernelUtils.getFrameworkProp(CmsConstants.I18N_LOCALES));
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

		checkReadiness();

		setInstance(this);
	}

	public void destroy() {
		setInstance(null);
	}

	/**
	 * Checks whether the deployment is available according to expectations, and
	 * mark it as available.
	 */
	private void checkReadiness() {
		if (isAvailable())
			return;
		if (cmsDeployment != null && userAdmin != null) {
			String data = KernelUtils.getFrameworkProp(KernelUtils.OSGI_INSTANCE_AREA);
			String state = KernelUtils.getFrameworkProp(KernelUtils.OSGI_CONFIGURATION_AREA);
			availableSince = System.currentTimeMillis();
			long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
			String jvmUptimeStr = " in " + (jvmUptime / 1000) + "." + (jvmUptime % 1000) + "s";
			log.info("## ARGEO CMS AVAILABLE" + (log.isDebugEnabled() ? jvmUptimeStr : "") + " ##");
			if (log.isDebugEnabled()) {
				log.debug("## state: " + state);
				if (data != null)
					log.debug("## data: " + data);
			}
			long begin = cmsState.getAvailableSince();
			long initDuration = System.currentTimeMillis() - begin;
			if (log.isTraceEnabled())
				log.trace("Kernel initialization took " + initDuration + "ms");
			tributeToFreeSoftware(initDuration);
		} else {
			throw new IllegalStateException("Deployment is not available");
		}
	}

	final private void tributeToFreeSoftware(long initDuration) {
		if (log.isTraceEnabled()) {
			long ms = initDuration / 100;
			log.trace("Spend " + ms + "ms" + " reflecting on the progress brought to mankind" + " by Free Software...");
			long beginNano = System.nanoTime();
			try {
				Thread.sleep(ms, 0);
			} catch (InterruptedException e) {
				// silent
			}
			long durationNano = System.nanoTime() - beginNano;
			final double M = 1000d * 1000d;
			double sleepAccuracy = ((double) durationNano) / (ms * M);
			log.trace("Sleep accuracy: " + String.format("%.2f", 100 - (sleepAccuracy * 100 - 100)) + " %");
		}
	}

	@Override
	public void createWorkgroup(String dn) {
//		if (egoRepository == null)
//			throw new CmsException("Ego repository is not available");
//		// TODO add check that the group exists
//		egoRepository.createWorkgroup(dn);
		throw new UnsupportedOperationException();
	}

	public void setCmsDeployment(CmsDeployment cmsDeployment) {
		this.cmsDeployment = cmsDeployment;
	}

	public void setCmsState(CmsState cmsState) {
		this.cmsState = cmsState;
	}

	public void setUserAdmin(UserAdmin userAdmin) {
		this.userAdmin = userAdmin;
	}

	@Override
	public Locale getDefaultLocale() {
		return defaultLocale;
	}

	@Override
	public List<Locale> getLocales() {
		return locales;
	}

	@Override
	public synchronized Long getAvailableSince() {
		return availableSince;
	}

	public synchronized boolean isAvailable() {
		return availableSince != null;
	}

	/*
	 * STATIC
	 */

	public synchronized static CmsContext getCmsContext() {
		return getInstance();
	}

	/** Required by USER login module. */
	public synchronized static UserAdmin getUserAdmin() {
		return getInstance().userAdmin;
	}

	/** Required by SPNEGO login module. */
	@Deprecated
	public synchronized static GSSCredential getAcceptorCredentials() {
		// FIXME find a cleaner way
		return ((NodeUserAdmin) getInstance().userAdmin).getAcceptorCredentials();
	}

	private synchronized static void setInstance(CmsContextImpl cmsContextImpl) {
		if (cmsContextImpl != null) {
			if (instance.isDone())
				throw new IllegalStateException("CMS Context is already set");
			instance.complete(cmsContextImpl);
		} else {
			instance = new CompletableFuture<CmsContextImpl>();
		}
	}

	private synchronized static CmsContextImpl getInstance() {
		try {
			return instance.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new IllegalStateException("Cannot retrieve CMS Context", e);
		}
	}

}
