package org.argeo.cms.internal.runtime;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.security.auth.Subject;

import org.argeo.api.cms.CmsContext;
import org.argeo.api.cms.CmsDeployment;
import org.argeo.api.cms.CmsEventBus;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsSession;
import org.argeo.api.cms.CmsSessionId;
import org.argeo.api.cms.CmsState;
import org.argeo.api.uuid.UuidFactory;
import org.argeo.cms.CmsDeployProperty;
import org.argeo.cms.internal.auth.CmsSessionImpl;
import org.ietf.jgss.GSSCredential;
import org.osgi.service.useradmin.UserAdmin;

/** Reference implementation of {@link CmsContext}. */
public class CmsContextImpl implements CmsContext {

	private final CmsLog log = CmsLog.getLog(getClass());

	private static CompletableFuture<CmsContextImpl> instance = new CompletableFuture<CmsContextImpl>();

	private CmsState cmsState;
	private CmsDeployment cmsDeployment;
	private UserAdmin userAdmin;
	private UuidFactory uuidFactory;
	private CmsEventBus cmsEventBus;

	// i18n
	private Locale defaultLocale;
	private List<Locale> locales = null;

	private Long availableSince;

	// time in ms to wait for CMS to be ready
	private final long readynessTimeout = 30 * 1000;

	// CMS sessions
	private Map<UUID, CmsSessionImpl> cmsSessionsByUuid = new HashMap<>();
	private Map<String, CmsSessionImpl> cmsSessionsByLocalId = new HashMap<>();

	public void start() {
		List<String> codes = CmsStateImpl.getDeployProperties(cmsState, CmsDeployProperty.LOCALE);
		locales = getLocaleList(codes);
		if (locales.size() == 0)
			throw new IllegalStateException("At least one locale must be set");
		defaultLocale = locales.get(0);

		new Thread(() -> {
			long begin = System.currentTimeMillis();
			long duration = 0;
			readyness: while (!checkReadiness()) {
				duration = System.currentTimeMillis() - begin;
				if (duration > readynessTimeout) {
					log.error("## CMS not ready after " + duration + " ms. Giving up checking.");
					break readyness;
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
		}, "Check readiness").start();
		setInstance(this);
	}

	public void stop() {
		setInstance(null);
	}

	/**
	 * Checks whether the deployment is available according to expectations, and
	 * mark it as available.
	 */
	private boolean checkReadiness() {
		if (isAvailable())
			return true;
		if (cmsDeployment == null)
			return false;

		if (((CmsDeploymentImpl) cmsDeployment).allExpectedServicesAvailable() && userAdmin != null) {
			String data = KernelUtils.getFrameworkProp(KernelUtils.OSGI_INSTANCE_AREA);
			String state = KernelUtils.getFrameworkProp(KernelUtils.OSGI_CONFIGURATION_AREA);
			availableSince = System.currentTimeMillis();
			long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
			String jvmUptimeStr = " in " + (jvmUptime / 1000) + "." + (jvmUptime % 1000) + "s";
			log.info("## ARGEO CMS " + cmsState.getUuid() + " AVAILABLE" + (log.isDebugEnabled() ? jvmUptimeStr : "")
					+ " ##");
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

			return true;
		} else {
			return false;
			// throw new IllegalStateException("Deployment is not available");
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

	/** Returns null if argument is null. */
	private static List<Locale> getLocaleList(List<String> codes) {
		if (codes == null)
			return null;
		ArrayList<Locale> availableLocales = new ArrayList<Locale>();
		for (String code : codes) {
			if (code == null)
				continue;
			// variant not supported
			int indexUnd = code.indexOf("_");
			Locale locale;
			if (indexUnd > 0) {
				String language = code.substring(0, indexUnd);
				String country = code.substring(indexUnd + 1);
				locale = new Locale(language, country);
			} else {
				locale = new Locale(code);
			}
			availableLocales.add(locale);
		}
		return availableLocales;
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

	public UuidFactory getUuidFactory() {
		return uuidFactory;
	}

	public void setUuidFactory(UuidFactory uuidFactory) {
		this.uuidFactory = uuidFactory;
	}

	@Override
	public Locale getDefaultLocale() {
		return defaultLocale;
	}

	@Override
	public UUID timeUUID() {
		return uuidFactory.timeUUID();
	}

	@Override
	public List<Locale> getLocales() {
		return locales;
	}

	@Override
	public Long getAvailableSince() {
		return availableSince;
	}

	public boolean isAvailable() {
		return availableSince != null;
	}

	public CmsState getCmsState() {
		return cmsState;
	}

	@Override
	public CmsEventBus getCmsEventBus() {
		return cmsEventBus;
	}

	public void setCmsEventBus(CmsEventBus cmsEventBus) {
		this.cmsEventBus = cmsEventBus;
	}

	/*
	 * STATIC
	 */

	public static CmsContextImpl getCmsContext() {
		return getInstance();
	}

	/** Required by SPNEGO login module. */
	public GSSCredential getAcceptorCredentials() {
		// TODO find a cleaner way
		return ((CmsUserAdmin) userAdmin).getAcceptorCredentials();
	}

	private static void setInstance(CmsContextImpl cmsContextImpl) {
		if (cmsContextImpl != null) {
			if (instance.isDone())
				throw new IllegalStateException("CMS Context is already set");
			instance.complete(cmsContextImpl);
		} else {
			if (!instance.isDone())
				instance.cancel(true);
			instance = new CompletableFuture<CmsContextImpl>();
		}
	}

	private static CmsContextImpl getInstance() {
		try {
			return instance.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new IllegalStateException("Cannot retrieve CMS Context", e);
		}
	}

	public UserAdmin getUserAdmin() {
		return userAdmin;
	}

	/*
	 * CMS Sessions
	 */

	@Override
	public CmsSession getCmsSession(Subject subject) {
		if (subject.getPrivateCredentials(CmsSessionId.class).isEmpty())
			return null;
		CmsSessionId cmsSessionId = subject.getPrivateCredentials(CmsSessionId.class).iterator().next();
		return getCmsSessionByUuid(cmsSessionId.getUuid());
	}

	public void registerCmsSession(CmsSessionImpl cmsSession) {
		if (cmsSessionsByUuid.containsKey(cmsSession.getUuid())
				|| cmsSessionsByLocalId.containsKey(cmsSession.getLocalId()))
			throw new IllegalStateException("CMS session " + cmsSession + " is already registered.");
		cmsSessionsByUuid.put(cmsSession.getUuid(), cmsSession);
		cmsSessionsByLocalId.put(cmsSession.getLocalId(), cmsSession);
	}

	public void unregisterCmsSession(CmsSessionImpl cmsSession) {
		if (!cmsSessionsByUuid.containsKey(cmsSession.getUuid())
				|| !cmsSessionsByLocalId.containsKey(cmsSession.getLocalId()))
			throw new IllegalStateException("CMS session " + cmsSession + " is not registered.");
		CmsSession removed = cmsSessionsByUuid.remove(cmsSession.getUuid());
		assert removed == cmsSession;
		cmsSessionsByLocalId.remove(cmsSession.getLocalId());
	}

	/**
	 * The {@link CmsSession} related to this UUID, or <code>null</null> if not
	 * registered.
	 */
	public CmsSessionImpl getCmsSessionByUuid(UUID uuid) {
		return cmsSessionsByUuid.get(uuid);
	}

	/**
	 * The {@link CmsSession} related to this local id, or <code>null</null> if not
	 * registered.
	 */
	public CmsSessionImpl getCmsSessionByLocalId(String localId) {
		return cmsSessionsByLocalId.get(localId);
	}

}
