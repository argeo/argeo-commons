package org.argeo.cms.internal.runtime;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.login.Configuration;

import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsState;
import org.argeo.cms.auth.ident.IdentClient;
import org.argeo.cms.internal.osgi.CmsShutdown;
import org.osgi.framework.Constants;

/**
 * Implementation of a {@link CmsState}, initialising the required services.
 */
public class CmsStateImpl implements CmsState {
	private final static CmsLog log = CmsLog.getLog(CmsStateImpl.class);
//	private final BundleContext bc = FrameworkUtil.getBundle(CmsState.class).getBundleContext();

//	private static CmsStateImpl instance;

//	private ExecutorService internalExecutorService;

	// REFERENCES
	private Long availableSince;


//	private ThreadGroup threadGroup = new ThreadGroup("CMS");
	private List<Runnable> stopHooks = new ArrayList<>();

	private String stateUuid;
//	private final boolean cleanState;
	private String hostname;

	public void init() {
//		instance = this;

		Runtime.getRuntime().addShutdownHook(new CmsShutdown());
//		this.internalExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		try {
			initSecurity();
//			initArgeoLogger();
//			initNode();

			if (log.isTraceEnabled())
				log.trace("CMS State started");
		} catch (Throwable e) {
			log.error("## FATAL: CMS activator failed", e);
		}

		this.stateUuid = KernelUtils.getFrameworkProp(Constants.FRAMEWORK_UUID);
//		this.cleanState = stateUuid.equals(frameworkUuid);
		try {
			this.hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			log.error("Cannot set hostname: " + e);
		}

		availableSince = System.currentTimeMillis();
		if (log.isDebugEnabled())
			// log.debug("## CMS starting... stateUuid=" + this.stateUuid + (cleanState ? "
			// (clean state) " : " "));
			log.debug("## CMS starting... (" + stateUuid + ")");

//		initI18n();
//		initServices();

	}

	private void initSecurity() {
		if (System.getProperty(KernelConstants.JAAS_CONFIG_PROP) == null) {
			String jaasConfig = KernelConstants.JAAS_CONFIG;
			URL url = getClass().getResource(jaasConfig);
			// System.setProperty(KernelConstants.JAAS_CONFIG_PROP,
			// url.toExternalForm());
			KernelUtils.setJaasConfiguration(url);
		}
		// explicitly load JAAS configuration
		Configuration.getConfiguration();
	}

//	private void initI18n() {
//		Object defaultLocaleValue = KernelUtils.getFrameworkProp(CmsConstants.I18N_DEFAULT_LOCALE);
//		defaultLocale = defaultLocaleValue != null ? new Locale(defaultLocaleValue.toString())
//				: new Locale(ENGLISH.getLanguage());
//		locales = LocaleUtils.asLocaleList(KernelUtils.getFrameworkProp(CmsConstants.I18N_LOCALES));
//	}

	private void initServices() {
		// JTA
//		String tmType = KernelUtils.getFrameworkProp(CmsConstants.TRANSACTION_MANAGER,
//				CmsConstants.TRANSACTION_MANAGER_SIMPLE);
//		if (CmsConstants.TRANSACTION_MANAGER_SIMPLE.equals(tmType)) {
//			initSimpleTransactionManager();
//		} else if (CmsConstants.TRANSACTION_MANAGER_BITRONIX.equals(tmType)) {
////			initBitronixTransactionManager();
//			throw new UnsupportedOperationException(
//					"Bitronix is not supported anymore, but could be again if there is enough interest.");
//		} else {
//			throw new IllegalArgumentException("Usupported transaction manager type " + tmType);
//		}

		// POI
//		POIXMLTypeLoader.setClassLoader(CTConnection.class.getClassLoader());

		// Tika
//		OpenDocumentParser odfParser = new OpenDocumentParser();
//		bc.registerService(Parser.class, odfParser, new Hashtable());
//		PDFParser pdfParser = new PDFParser();
//		bc.registerService(Parser.class, pdfParser, new Hashtable());
//		OOXMLParser ooxmlParser = new OOXMLParser();
//		bc.registerService(Parser.class, ooxmlParser, new Hashtable());
//		TesseractOCRParser ocrParser = new TesseractOCRParser();
//		ocrParser.setLanguage("ara");
//		bc.registerService(Parser.class, ocrParser, new Hashtable());

//		// JCR
//		RepositoryServiceFactory repositoryServiceFactory = new RepositoryServiceFactory();
//		stopHooks.add(() -> repositoryServiceFactory.shutdown());
//		Activator.registerService(ManagedServiceFactory.class, repositoryServiceFactory,
//				LangUtils.dict(Constants.SERVICE_PID, NodeConstants.NODE_REPOS_FACTORY_PID));
//
//		NodeRepositoryFactory repositoryFactory = new NodeRepositoryFactory();
//		Activator.registerService(RepositoryFactory.class, repositoryFactory, null);

		// Security
//		NodeUserAdmin userAdmin = new NodeUserAdmin(CmsConstants.ROLES_BASEDN, CmsConstants.TOKENS_BASEDN);
//		stopHooks.add(() -> userAdmin.destroy());
//		Activator.registerService(ManagedServiceFactory.class, userAdmin,
//				LangUtils.dict(Constants.SERVICE_PID, CmsConstants.NODE_USER_ADMIN_PID));

	}

//	private void initSimpleTransactionManager() {
//		SimpleTransactionManager transactionManager = new SimpleTransactionManager();
//		Activator.registerService(WorkControl.class, transactionManager, null);
//		Activator.registerService(WorkTransaction.class, transactionManager, null);
////		Activator.registerService(TransactionManager.class, transactionManager, null);
////		Activator.registerService(UserTransaction.class, transactionManager, null);
//		// TODO TransactionSynchronizationRegistry
//	}

//	private void initBitronixTransactionManager() {
//		// TODO manage it in a managed service, as startup could be long
//		ServiceReference<TransactionManager> existingTm = bc.getServiceReference(TransactionManager.class);
//		if (existingTm != null) {
//			if (log.isDebugEnabled())
//				log.debug("Using provided transaction manager " + existingTm);
//			return;
//		}
//
//		if (!TransactionManagerServices.isTransactionManagerRunning()) {
//			bitronix.tm.Configuration tmConf = TransactionManagerServices.getConfiguration();
//			tmConf.setServerId(UUID.randomUUID().toString());
//
//			Bundle bitronixBundle = FrameworkUtil.getBundle(bitronix.tm.Configuration.class);
//			File tmBaseDir = bitronixBundle.getDataFile(KernelConstants.DIR_TRANSACTIONS);
//			File tmDir1 = new File(tmBaseDir, "btm1");
//			tmDir1.mkdirs();
//			tmConf.setLogPart1Filename(new File(tmDir1, tmDir1.getName() + ".tlog").getAbsolutePath());
//			File tmDir2 = new File(tmBaseDir, "btm2");
//			tmDir2.mkdirs();
//			tmConf.setLogPart2Filename(new File(tmDir2, tmDir2.getName() + ".tlog").getAbsolutePath());
//		}
//		BitronixTransactionManager transactionManager = getTransactionManager();
//		stopHooks.add(() -> transactionManager.shutdown());
//		BitronixTransactionSynchronizationRegistry transactionSynchronizationRegistry = getTransactionSynchronizationRegistry();
//		// register
//		bc.registerService(TransactionManager.class, transactionManager, null);
//		bc.registerService(UserTransaction.class, transactionManager, null);
//		bc.registerService(TransactionSynchronizationRegistry.class, transactionSynchronizationRegistry, null);
//		if (log.isDebugEnabled())
//			log.debug("Initialised default Bitronix transaction manager");
//	}

	public void destroy() {
		if (log.isDebugEnabled())
			log.debug("CMS stopping...  (" + this.stateUuid + ")");

		// In a different thread in order to avoid interruptions
		Thread stopHookThread = new Thread(() -> applyStopHooks(), "Apply Argeo Stop Hooks");
		stopHookThread.start();
		try {
			stopHookThread.join(10 * 60 * 1000);
		} catch (InterruptedException e) {
			// silent
		}

//		internalExecutorService.shutdown();

		long duration = ((System.currentTimeMillis() - availableSince) / 1000) / 60;
		log.info("## ARGEO CMS STOPPED after " + (duration / 60) + "h " + (duration % 60) + "min uptime ##");
	}

	/** Apply shutdown hoos in reverse order. */
	private void applyStopHooks() {
		for (int i = stopHooks.size() - 1; i >= 0; i--) {
			try {
				stopHooks.get(i).run();
			} catch (Exception e) {
				log.error("Could not run shutdown hook #" + i);
			}
		}
		// Clean hanging Gogo shell thread
		new GogoShellKiller().start();

//		instance = null;
	}

//	@Override
//	public boolean isClean() {
//		return cleanState;
//	}

	@Override
	public Long getAvailableSince() {
		return availableSince;
	}

	/*
	 * ACCESSORS
	 */
	public String getHostname() {
		return hostname;
	}

	/*
	 * STATIC
	 */
	public static IdentClient getIdentClient(String remoteAddr) {
		if (!IdentClient.isDefaultAuthdPassphraseFileAvailable())
			return null;
		// TODO make passphrase more configurable
		return new IdentClient(remoteAddr);
	}
}
