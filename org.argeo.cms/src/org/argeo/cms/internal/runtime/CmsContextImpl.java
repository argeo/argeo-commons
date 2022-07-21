package org.argeo.cms.internal.runtime;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.SubmissionPublisher;

import javax.security.auth.Subject;

import org.argeo.api.cms.CmsContext;
import org.argeo.api.cms.CmsDeployment;
import org.argeo.api.cms.CmsEventSubscriber;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsSession;
import org.argeo.api.cms.CmsSessionId;
import org.argeo.api.cms.CmsState;
import org.argeo.api.uuid.UuidFactory;
import org.argeo.cms.CmsDeployProperty;
import org.argeo.cms.internal.auth.CmsSessionImpl;
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
	private UuidFactory uuidFactory;
//	private ProvidedRepository contentRepository;

	// i18n
	private Locale defaultLocale;
	private List<Locale> locales = null;

	private Long availableSince;

	// CMS sessions
	private Map<UUID, CmsSessionImpl> cmsSessionsByUuid = new HashMap<>();
	private Map<String, CmsSessionImpl> cmsSessionsByLocalId = new HashMap<>();

	// CMS events
	private Map<String, SubmissionPublisher<Map<String, Object>>> topics = new TreeMap<>();
//	private IdentityHashMap<CmsEventSubscriber, List<CmsEventFlowSubscriber>> subscriptions = new IdentityHashMap<>();

//	public CmsContextImpl() {
//		initTrackers();
//	}

	public void start() {
		List<String> codes = CmsStateImpl.getDeployProperties(cmsState, CmsDeployProperty.LOCALE);
		locales = getLocaleList(codes);
		if (locales.size() == 0)
			throw new IllegalStateException("At least one locale must be set");
		defaultLocale = locales.get(0);
//		Object defaultLocaleValue = KernelUtils.getFrameworkProp(CmsConstants.I18N_DEFAULT_LOCALE);
//		defaultLocale = defaultLocaleValue != null ? new Locale(defaultLocaleValue.toString())
//				: new Locale(ENGLISH.getLanguage());
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

		new Thread(() -> {
			while (!checkReadiness()) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
		}, "Check readiness").start();
		
		// checkReadiness();

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

//	public ProvidedRepository getContentRepository() {
//		return contentRepository;
//	}
//
//	public void setContentRepository(ProvidedRepository contentRepository) {
//		this.contentRepository = contentRepository;
//	}

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

	@Override
	public CmsState getCmsState() {
		return cmsState;
	}

	/*
	 * STATIC
	 */

	public synchronized static CmsContextImpl getCmsContext() {
		return getInstance();
	}

//	/** Required by USER login module. */
//	public synchronized static UserAdmin getUserAdmin() {
//		return getInstance().userAdmin;
//	}

	/** Required by SPNEGO login module. */
	@Deprecated
	public synchronized static GSSCredential getAcceptorCredentials() {
		// FIXME find a cleaner way
		return ((CmsUserAdmin) getInstance().userAdmin).getAcceptorCredentials();
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

	public UserAdmin getUserAdmin() {
		return userAdmin;
	}

	/*
	 * CMS Sessions
	 */

	@Override
	public synchronized CmsSession getCmsSession(Subject subject) {
		if (subject.getPrivateCredentials(CmsSessionId.class).isEmpty())
			return null;
		CmsSessionId cmsSessionId = subject.getPrivateCredentials(CmsSessionId.class).iterator().next();
		return getCmsSessionByUuid(cmsSessionId.getUuid());
	}

	public synchronized void registerCmsSession(CmsSessionImpl cmsSession) {
		if (cmsSessionsByUuid.containsKey(cmsSession.getUuid())
				|| cmsSessionsByLocalId.containsKey(cmsSession.getLocalId()))
			throw new IllegalStateException("CMS session " + cmsSession + " is already registered.");
		cmsSessionsByUuid.put(cmsSession.getUuid(), cmsSession);
		cmsSessionsByLocalId.put(cmsSession.getLocalId(), cmsSession);
	}

	public synchronized void unregisterCmsSession(CmsSessionImpl cmsSession) {
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
	public synchronized CmsSessionImpl getCmsSessionByUuid(UUID uuid) {
		return cmsSessionsByUuid.get(uuid);
	}

	/**
	 * The {@link CmsSession} related to this local id, or <code>null</null> if not
	 * registered.
	 */
	public synchronized CmsSessionImpl getCmsSessionByLocalId(String localId) {
		return cmsSessionsByLocalId.get(localId);
	}

	/*
	 * CMS Events
	 */
	public void sendEvent(String topic, Map<String, Object> event) {
		SubmissionPublisher<Map<String, Object>> publisher = topics.get(topic);
		if (publisher == null)
			return; // no one is interested
		publisher.submit(event);
	}

	public void addEventSubscriber(String topic, CmsEventSubscriber subscriber) {
		synchronized (topics) {
			if (!topics.containsKey(topic))
				topics.put(topic, new SubmissionPublisher<>());
		}
		SubmissionPublisher<Map<String, Object>> publisher = topics.get(topic);
		CmsEventFlowSubscriber flowSubscriber = new CmsEventFlowSubscriber(topic, subscriber);
		publisher.subscribe(flowSubscriber);
	}

	public void removeEventSubscriber(String topic, CmsEventSubscriber subscriber) {
		SubmissionPublisher<Map<String, Object>> publisher = topics.get(topic);
		if (publisher == null) {
			log.error("There should be an event topic " + topic);
			return;
		}
		for (Flow.Subscriber<? super Map<String, Object>> flowSubscriber : publisher.getSubscribers()) {
			if (flowSubscriber instanceof CmsEventFlowSubscriber)
				((CmsEventFlowSubscriber) flowSubscriber).unsubscribe();
		}
		synchronized (topics) {
			if (!publisher.hasSubscribers()) {
				publisher.close();
				topics.remove(topic);
			}
		}
	}

	static class CmsEventFlowSubscriber implements Flow.Subscriber<Map<String, Object>> {
		private String topic;
		private CmsEventSubscriber eventSubscriber;

		private Subscription subscription;

		public CmsEventFlowSubscriber(String topic, CmsEventSubscriber eventSubscriber) {
			this.topic = topic;
			this.eventSubscriber = eventSubscriber;
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			this.subscription = subscription;
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(Map<String, Object> item) {
			eventSubscriber.onEvent(topic, item);

		}

		@Override
		public void onError(Throwable throwable) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onComplete() {
			// TODO Auto-generated method stub

		}

		void unsubscribe() {
			if (subscription != null)
				subscription.cancel();
			else
				throw new IllegalStateException("No subscription to cancel");
		}

	}

}
