package org.argeo.cms.internal.kernel;

import static java.util.Locale.ENGLISH;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.api.NodeConstants;
import org.argeo.api.NodeState;
import org.argeo.cms.LocaleUtils;
import org.argeo.osgi.transaction.SimpleTransactionManager;
import org.argeo.osgi.transaction.WorkControl;
import org.argeo.osgi.transaction.WorkTransaction;
import org.argeo.util.LangUtils;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedServiceFactory;

/**
 * Implementation of a {@link NodeState}, initialising the required services.
 */
public class CmsState implements NodeState {
	private final static Log log = LogFactory.getLog(CmsState.class);
//	private final BundleContext bc = FrameworkUtil.getBundle(CmsState.class).getBundleContext();

	// REFERENCES
	private Long availableSince;

	// i18n
	private Locale defaultLocale;
	private List<Locale> locales = null;

	private ThreadGroup threadGroup = new ThreadGroup("CMS");
	private List<Runnable> stopHooks = new ArrayList<>();

	private final String stateUuid;
//	private final boolean cleanState;
	private String hostname;

	public CmsState() {
//		this.stateUuid = stateUuid;
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

		initI18n();
		initServices();

	}

	private void initI18n() {
		Object defaultLocaleValue = KernelUtils.getFrameworkProp(NodeConstants.I18N_DEFAULT_LOCALE);
		defaultLocale = defaultLocaleValue != null ? new Locale(defaultLocaleValue.toString())
				: new Locale(ENGLISH.getLanguage());
		locales = LocaleUtils.asLocaleList(KernelUtils.getFrameworkProp(NodeConstants.I18N_LOCALES));
	}

	private void initServices() {
		// JTA
		String tmType = KernelUtils.getFrameworkProp(NodeConstants.TRANSACTION_MANAGER,
				NodeConstants.TRANSACTION_MANAGER_SIMPLE);
		if (NodeConstants.TRANSACTION_MANAGER_SIMPLE.equals(tmType)) {
			initSimpleTransactionManager();
		} else if (NodeConstants.TRANSACTION_MANAGER_BITRONIX.equals(tmType)) {
//			initBitronixTransactionManager();
			throw new UnsupportedOperationException(
					"Bitronix is not supported anymore, but could be again if there is enough interest.");
		} else {
			throw new IllegalArgumentException("Usupported transaction manager type " + tmType);
		}

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
		NodeUserAdmin userAdmin = new NodeUserAdmin(NodeConstants.ROLES_BASEDN, NodeConstants.TOKENS_BASEDN);
		stopHooks.add(() -> userAdmin.destroy());
		Activator.registerService(ManagedServiceFactory.class, userAdmin,
				LangUtils.dict(Constants.SERVICE_PID, NodeConstants.NODE_USER_ADMIN_PID));

	}

	private void initSimpleTransactionManager() {
		SimpleTransactionManager transactionManager = new SimpleTransactionManager();
		Activator.registerService(WorkControl.class, transactionManager, null);
		Activator.registerService(WorkTransaction.class, transactionManager, null);
//		Activator.registerService(TransactionManager.class, transactionManager, null);
//		Activator.registerService(UserTransaction.class, transactionManager, null);
		// TODO TransactionSynchronizationRegistry
	}

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

	void shutdown() {
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
	public Locale getDefaultLocale() {
		return defaultLocale;
	}

	public List<Locale> getLocales() {
		return locales;
	}

	public String getHostname() {
		return hostname;
	}
}
