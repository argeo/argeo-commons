package org.argeo.cms.internal.kernel;

import static bitronix.tm.TransactionManagerServices.getTransactionManager;
import static bitronix.tm.TransactionManagerServices.getTransactionSynchronizationRegistry;
import static java.util.Locale.ENGLISH;
import static org.argeo.cms.internal.auth.LocaleChoice.asLocaleList;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.jcr.RepositoryFactory;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.maintenance.MaintenanceUi;
import org.argeo.node.NodeConstants;
import org.argeo.node.NodeState;
import org.argeo.util.LangUtils;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.useradmin.UserAdmin;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.BitronixTransactionSynchronizationRegistry;
import bitronix.tm.TransactionManagerServices;

public class CmsState implements NodeState {
	private final Log log = LogFactory.getLog(CmsState.class);
	private final BundleContext bc = FrameworkUtil.getBundle(CmsState.class).getBundleContext();

	// REFERENCES
	private Long availableSince;

	// i18n
	private Locale defaultLocale;
	private List<Locale> locales = null;

	private ThreadGroup threadGroup = new ThreadGroup("CMS");
	private KernelThread kernelThread;
	private List<Runnable> shutdownHooks = new ArrayList<>();

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
			log.error("Cannot set hostname", e);
		}

		availableSince = System.currentTimeMillis();
		if (log.isDebugEnabled())
			log.debug("## CMS STARTED " + this.stateUuid + (cleanState ? " (clean state) " : " "));

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
		locales = asLocaleList(KernelUtils.getFrameworkProp(NodeConstants.I18N_LOCALES));
	}

	private void initServices() {
		initTransactionManager();

		// JCR
		RepositoryServiceFactory repositoryServiceFactory = new RepositoryServiceFactory();
		shutdownHooks.add(() -> repositoryServiceFactory.shutdown());
		bc.registerService(ManagedServiceFactory.class, repositoryServiceFactory,
				LangUtils.init(Constants.SERVICE_PID, NodeConstants.NODE_REPOS_FACTORY_PID));

		NodeRepositoryFactory repositoryFactory = new NodeRepositoryFactory();
		bc.registerService(RepositoryFactory.class, repositoryFactory, null);

		// Security
		NodeUserAdmin userAdmin = new NodeUserAdmin();
		shutdownHooks.add(() -> userAdmin.destroy());
		Dictionary<String, Object> props = userAdmin.currentState();
		props.put(Constants.SERVICE_PID, NodeConstants.NODE_USER_ADMIN_PID);
		bc.registerService(UserAdmin.class, userAdmin, props);

		// UI
		bc.registerService(ApplicationConfiguration.class, new MaintenanceUi(),
				LangUtils.init(KernelConstants.CONTEXT_NAME_PROP, "system"));
		bc.registerService(ApplicationConfiguration.class, new UserUi(), LangUtils.init(KernelConstants.CONTEXT_NAME_PROP, "user"));
	}

	private void initTransactionManager() {
		// TODO manage it in a managed service, as startup could be long
		ServiceReference<TransactionManager> existingTm = bc.getServiceReference(TransactionManager.class);
		if (existingTm != null) {
			if (log.isDebugEnabled())
				log.debug("Using provided transaction manager " + existingTm);
		}
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

		BitronixTransactionManager transactionManager = getTransactionManager();
		shutdownHooks.add(() -> transactionManager.shutdown());
		BitronixTransactionSynchronizationRegistry transactionSynchronizationRegistry = getTransactionSynchronizationRegistry();
		// register
		bc.registerService(TransactionManager.class, transactionManager, null);
		bc.registerService(UserTransaction.class, transactionManager, null);
		bc.registerService(TransactionSynchronizationRegistry.class, transactionSynchronizationRegistry, null);
		if (log.isDebugEnabled())
			log.debug("Initialised default Bitronix transaction manager");
	}

	void shutdown() {
		applyShutdownHooks();

		if (kernelThread != null)
			kernelThread.destroyAndJoin();

		if (log.isDebugEnabled())
			log.debug("## CMS STOPPED");
	}

	/** Apply shutdown hoos in reverse order. */
	private void applyShutdownHooks() {
		for (int i = shutdownHooks.size() - 1; i >= 0; i--) {
			try {
				shutdownHooks.get(i).run();
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

	/** Workaround for blocking Gogo shell by system shutdown. */
	private class GogoShellKiller extends Thread {

		public GogoShellKiller() {
			super("Gogo Shell Killer");
			setDaemon(true);
		}

		@Override
		public void run() {
			ThreadGroup rootTg = getRootThreadGroup(null);
			Thread gogoShellThread = findGogoShellThread(rootTg);
			if (gogoShellThread == null)
				return;
			while (getNonDaemonCount(rootTg) > 2) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// silent
				}
			}
			gogoShellThread = findGogoShellThread(rootTg);
			if (gogoShellThread == null)
				return;
			System.exit(0);
		}
	}

	private static ThreadGroup getRootThreadGroup(ThreadGroup tg) {
		if (tg == null)
			tg = Thread.currentThread().getThreadGroup();
		if (tg.getParent() == null)
			return tg;
		else
			return getRootThreadGroup(tg.getParent());
	}

	private static int getNonDaemonCount(ThreadGroup rootThreadGroup) {
		Thread[] threads = new Thread[rootThreadGroup.activeCount()];
		rootThreadGroup.enumerate(threads);
		int nonDameonCount = 0;
		for (Thread t : threads)
			if (t != null && !t.isDaemon())
				nonDameonCount++;
		return nonDameonCount;
	}

	private static Thread findGogoShellThread(ThreadGroup rootThreadGroup) {
		Thread[] threads = new Thread[rootThreadGroup.activeCount()];
		rootThreadGroup.enumerate(threads, true);
		for (Thread thread : threads) {
			if (thread.getName().equals("Gogo shell"))
				return thread;
		}
		return null;
	}

}
