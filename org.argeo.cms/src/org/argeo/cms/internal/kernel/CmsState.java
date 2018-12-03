package org.argeo.cms.internal.kernel;

import static bitronix.tm.TransactionManagerServices.getTransactionManager;
import static bitronix.tm.TransactionManagerServices.getTransactionSynchronizationRegistry;
import static java.util.Locale.ENGLISH;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.jcr.RepositoryFactory;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.cms.i18n.LocaleUtils;
import org.argeo.node.NodeConstants;
import org.argeo.node.NodeState;
import org.argeo.transaction.simple.SimpleTransactionManager;
import org.argeo.util.LangUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedServiceFactory;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.BitronixTransactionSynchronizationRegistry;
import bitronix.tm.TransactionManagerServices;

public class CmsState implements NodeState {
	private final static Log log = LogFactory.getLog(CmsState.class);
	private final BundleContext bc = FrameworkUtil.getBundle(CmsState.class).getBundleContext();

	// REFERENCES
	private Long availableSince;

	// i18n
	private Locale defaultLocale;
	private List<Locale> locales = null;

	private ThreadGroup threadGroup = new ThreadGroup("CMS");
	private KernelThread kernelThread;
	private List<Runnable> stopHooks = new ArrayList<>();

	private final String stateUuid;
	private final boolean cleanState;
	private String hostname;

	public CmsState(String stateUuid) {
		this.stateUuid = stateUuid;
		String frameworkUuid = KernelUtils.getFrameworkProp(Constants.FRAMEWORK_UUID);
		this.cleanState = stateUuid.equals(frameworkUuid);
		try {
			this.hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			log.error("Cannot set hostname: " + e);
		}

		availableSince = System.currentTimeMillis();
		if (log.isDebugEnabled())
			log.debug("## CMS starting... stateUuid=" + this.stateUuid + (cleanState ? " (clean state) " : " "));

		initI18n();
		initServices();

		// kernel thread
		kernelThread = new KernelThread(threadGroup, "Kernel Thread");
		kernelThread.setContextClassLoader(getClass().getClassLoader());
		kernelThread.start();
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
			initBitronixTransactionManager();
		} else {
			throw new CmsException("Usupported transaction manager type " + tmType);
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

		// JCR
		RepositoryServiceFactory repositoryServiceFactory = new RepositoryServiceFactory();
		stopHooks.add(() -> repositoryServiceFactory.shutdown());
		bc.registerService(ManagedServiceFactory.class, repositoryServiceFactory,
				LangUtils.dico(Constants.SERVICE_PID, NodeConstants.NODE_REPOS_FACTORY_PID));

		NodeRepositoryFactory repositoryFactory = new NodeRepositoryFactory();
		bc.registerService(RepositoryFactory.class, repositoryFactory, null);

		// Security
		NodeUserAdmin userAdmin = new NodeUserAdmin(NodeConstants.ROLES_BASEDN);
		stopHooks.add(() -> userAdmin.destroy());
		bc.registerService(ManagedServiceFactory.class, userAdmin,
				LangUtils.dico(Constants.SERVICE_PID, NodeConstants.NODE_USER_ADMIN_PID));

		// File System
		CmsFsProvider cmsFsProvider = new CmsFsProvider();
//		ServiceLoader<FileSystemProvider> fspSl = ServiceLoader.load(FileSystemProvider.class);
//		for (FileSystemProvider fsp : fspSl) {
//			log.debug("FileSystemProvider " + fsp);
//			if (fsp instanceof CmsFsProvider) {
//				cmsFsProvider = (CmsFsProvider) fsp;
//			}
//		}
//		for (FileSystemProvider fsp : FileSystemProvider.installedProviders()) {
//			log.debug("Installed FileSystemProvider " + fsp);
//		}
		bc.registerService(FileSystemProvider.class, cmsFsProvider,
				LangUtils.dico(Constants.SERVICE_PID, NodeConstants.NODE_FS_PROVIDER_PID));
	}

	private void initSimpleTransactionManager() {
		SimpleTransactionManager transactionManager = new SimpleTransactionManager();
		bc.registerService(TransactionManager.class, transactionManager, null);
		bc.registerService(UserTransaction.class, transactionManager, null);
		// TODO TransactionSynchronizationRegistry
	}

	private void initBitronixTransactionManager() {
		// TODO manage it in a managed service, as startup could be long
		ServiceReference<TransactionManager> existingTm = bc.getServiceReference(TransactionManager.class);
		if (existingTm != null) {
			if (log.isDebugEnabled())
				log.debug("Using provided transaction manager " + existingTm);
			return;
		}

		if (!TransactionManagerServices.isTransactionManagerRunning()) {
			bitronix.tm.Configuration tmConf = TransactionManagerServices.getConfiguration();
			tmConf.setServerId(UUID.randomUUID().toString());

			Bundle bitronixBundle = FrameworkUtil.getBundle(bitronix.tm.Configuration.class);
			File tmBaseDir = bitronixBundle.getDataFile(KernelConstants.DIR_TRANSACTIONS);
			File tmDir1 = new File(tmBaseDir, "btm1");
			tmDir1.mkdirs();
			tmConf.setLogPart1Filename(new File(tmDir1, tmDir1.getName() + ".tlog").getAbsolutePath());
			File tmDir2 = new File(tmBaseDir, "btm2");
			tmDir2.mkdirs();
			tmConf.setLogPart2Filename(new File(tmDir2, tmDir2.getName() + ".tlog").getAbsolutePath());
		}
		BitronixTransactionManager transactionManager = getTransactionManager();
		stopHooks.add(() -> transactionManager.shutdown());
		BitronixTransactionSynchronizationRegistry transactionSynchronizationRegistry = getTransactionSynchronizationRegistry();
		// register
		bc.registerService(TransactionManager.class, transactionManager, null);
		bc.registerService(UserTransaction.class, transactionManager, null);
		bc.registerService(TransactionSynchronizationRegistry.class, transactionSynchronizationRegistry, null);
		if (log.isDebugEnabled())
			log.debug("Initialised default Bitronix transaction manager");
	}

	void shutdown() {
		if (log.isDebugEnabled())
			log.debug("CMS stopping...  stateUuid=" + this.stateUuid + (cleanState ? " (clean state) " : " "));

		if (kernelThread != null)
			kernelThread.destroyAndJoin();
		// In a different state in order to avois interruptions
		new Thread(() -> applyStopHooks(), "Apply Argeo Stop Hooks").start();
		// applyStopHooks();

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

	@Override
	public boolean isClean() {
		return cleanState;
	}

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
