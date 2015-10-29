package org.argeo.cms.internal.kernel;

import static bitronix.tm.TransactionManagerServices.getTransactionManager;
import static bitronix.tm.TransactionManagerServices.getTransactionSynchronizationRegistry;
import static java.util.Locale.ENGLISH;
import static org.argeo.cms.internal.kernel.KernelUtils.getFrameworkProp;
import static org.argeo.cms.internal.kernel.KernelUtils.getOsgiInstanceDir;
import static org.argeo.jcr.ArgeoJcrConstants.ALIAS_NODE;
import static org.argeo.jcr.ArgeoJcrConstants.JCR_REPOSITORY_ALIAS;
import static org.argeo.util.LocaleChoice.asLocaleList;
import static org.osgi.framework.Constants.FRAMEWORK_UUID;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryFactory;
import javax.security.auth.Subject;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.util.TransientFileFactory;
import org.argeo.ArgeoException;
import org.argeo.ArgeoLogger;
import org.argeo.cms.CmsException;
import org.argeo.jackrabbit.OsgiJackrabbitRepositoryFactory;
import org.argeo.jcr.ArgeoJcrConstants;
import org.eclipse.equinox.http.jetty.JettyConfigurator;
import org.eclipse.equinox.http.jetty.JettyConstants;
import org.eclipse.equinox.http.servlet.ExtendedHttpService;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.util.tracker.ServiceTracker;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.BitronixTransactionSynchronizationRegistry;
import bitronix.tm.TransactionManagerServices;

/**
 * Argeo CMS Kernel. Responsible for :
 * <ul>
 * <li>security</li>
 * <li>provisioning</li>
 * <li>transaction</li>
 * <li>logging</li>
 * <li>local and remote file systems access</li>
 * <li>OS access</li>
 * </ul>
 */
final class Kernel implements KernelHeader, KernelConstants, ServiceListener {
	/*
	 * SERVICE REFERENCES
	 */
	private ServiceReference<ConfigurationAdmin> configurationAdmin;
	/*
	 * REGISTERED SERVICES
	 */
	private ServiceRegistration<ArgeoLogger> loggerReg;
	private ServiceRegistration<TransactionManager> tmReg;
	private ServiceRegistration<UserTransaction> utReg;
	private ServiceRegistration<TransactionSynchronizationRegistry> tsrReg;
	private ServiceRegistration<? extends Repository> repositoryReg;
	private ServiceRegistration<RepositoryFactory> repositoryFactoryReg;
	private ServiceRegistration<UserAdmin> userAdminReg;

	/*
	 * SERVICES IMPLEMENTATIONS
	 */
	private NodeLogger logger;
	private BitronixTransactionManager transactionManager;
	private BitronixTransactionSynchronizationRegistry transactionSynchronizationRegistry;
	private OsgiJackrabbitRepositoryFactory repositoryFactory;
	NodeRepository repository;
	private NodeUserAdmin userAdmin;

	// Members
	private final static Log log = LogFactory.getLog(Kernel.class);
	ThreadGroup threadGroup = new ThreadGroup(Kernel.class.getSimpleName());
	private final BundleContext bc = Activator.getBundleContext();
	private final NodeSecurity nodeSecurity;
	private DataHttp dataHttp;
	private KernelThread kernelThread;

	private Locale defaultLocale = null;
	private List<Locale> locales = null;

	public Kernel() {
		nodeSecurity = new NodeSecurity();
	}

	final void init() {
		Subject.doAs(nodeSecurity.getKernelSubject(),
				new PrivilegedAction<Void>() {
					@Override
					public Void run() {
						doInit();
						return null;
					}
				});
	}

	private void doInit() {
		long begin = System.currentTimeMillis();
		ConfigurationAdmin conf = findConfigurationAdmin();
		// Use CMS bundle classloader
		ClassLoader currentContextCl = Thread.currentThread()
				.getContextClassLoader();
		Thread.currentThread().setContextClassLoader(
				Kernel.class.getClassLoader());
		try {
			if (nodeSecurity.isFirstInit())
				firstInit();

			defaultLocale = new Locale(getFrameworkProp(I18N_DEFAULT_LOCALE,
					ENGLISH.getLanguage()));
			locales = asLocaleList(getFrameworkProp(I18N_LOCALES));

			ServiceTracker<LogReaderService, LogReaderService> logReaderService = new ServiceTracker<LogReaderService, LogReaderService>(
					bc, LogReaderService.class, null);
			logReaderService.open();
			logger = new NodeLogger(logReaderService.getService());
			logReaderService.close();

			// KernelUtils.logFrameworkProperties(log);

			// Initialise services
			initTransactionManager();
			repository = new NodeRepository();
			repositoryFactory = new OsgiJackrabbitRepositoryFactory();
			userAdmin = new NodeUserAdmin(transactionManager, repository);

			// HTTP
			initWebServer(conf);
			ServiceReference<ExtendedHttpService> sr = bc
					.getServiceReference(ExtendedHttpService.class);
			if (sr != null)
				addHttpService(sr);

			UserUi userUi = new UserUi();
			Hashtable<String, String> props = new Hashtable<String, String>();
			props.put("contextName", "user");
			bc.registerService(ApplicationConfiguration.class, userUi, props);

			// Kernel thread
			kernelThread = new KernelThread(this);
			kernelThread.setContextClassLoader(Kernel.class.getClassLoader());
			kernelThread.start();

			// Publish services to OSGi
			publish();
		} catch (Exception e) {
			log.error("Cannot initialize Argeo CMS", e);
			throw new ArgeoException("Cannot initialize", e);
		} finally {
			Thread.currentThread().setContextClassLoader(currentContextCl);
		}

		long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
		log.info("## ARGEO CMS UP in " + (jvmUptime / 1000) + "."
				+ (jvmUptime % 1000) + "s ##");
		long initDuration = System.currentTimeMillis() - begin;
		if (log.isTraceEnabled())
			log.trace("Kernel initialization took " + initDuration + "ms");
		directorsCut(initDuration);
	}

	private void firstInit() {
		log.info("## FIRST INIT ##");
		String nodeInit = getFrameworkProp(NODE_INIT);
		if (nodeInit == null)
			nodeInit = "../../init";
		File initDir;
		if (nodeInit.startsWith("."))
			initDir = KernelUtils.getExecutionDir(nodeInit);
		else
			initDir = new File(nodeInit);
		// TODO also uncompress archives
		if (initDir.exists())
			try {
				FileUtils.copyDirectory(initDir, getOsgiInstanceDir(),
						new FileFilter() {

							@Override
							public boolean accept(File pathname) {
								if (pathname.getName().equals(".svn")
										|| pathname.getName().equals(".git"))
									return false;
								return true;
							}
						});
				log.info("CMS initialized from " + initDir.getCanonicalPath());
			} catch (IOException e) {
				throw new CmsException("Cannot initialize from " + initDir, e);
			}
	}

	/** Can be null */
	private ConfigurationAdmin findConfigurationAdmin() {
		configurationAdmin = bc.getServiceReference(ConfigurationAdmin.class);
		if (configurationAdmin == null) {
			return null;
		}
		return bc.getService(configurationAdmin);
	}

	private void initTransactionManager() {
		bitronix.tm.Configuration tmConf = TransactionManagerServices
				.getConfiguration();
		tmConf.setServerId(getFrameworkProp(FRAMEWORK_UUID));

		// File tmBaseDir = new File(getFrameworkProp(TRANSACTIONS_HOME,
		// getOsgiInstancePath(DIR_TRANSACTIONS)));
		File tmBaseDir = bc.getDataFile(DIR_TRANSACTIONS);
		File tmDir1 = new File(tmBaseDir, "btm1");
		tmDir1.mkdirs();
		tmConf.setLogPart1Filename(new File(tmDir1, tmDir1.getName() + ".tlog")
				.getAbsolutePath());
		File tmDir2 = new File(tmBaseDir, "btm2");
		tmDir2.mkdirs();
		tmConf.setLogPart2Filename(new File(tmDir2, tmDir2.getName() + ".tlog")
				.getAbsolutePath());
		transactionManager = getTransactionManager();
		transactionSynchronizationRegistry = getTransactionSynchronizationRegistry();
	}

	private void initWebServer(ConfigurationAdmin conf) {
		String httpPort = getFrameworkProp("org.osgi.service.http.port");
		String httpsPort = getFrameworkProp("org.osgi.service.http.port.secure");
		try {
			if (httpPort != null || httpsPort != null) {
				Hashtable<String, Object> jettyProps = new Hashtable<String, Object>();
				if (httpPort != null) {
					jettyProps.put(JettyConstants.HTTP_PORT, httpPort);
					jettyProps.put(JettyConstants.HTTP_ENABLED, true);
				}
				if (httpsPort != null) {
					jettyProps.put(JettyConstants.HTTPS_PORT, httpsPort);
					jettyProps.put(JettyConstants.HTTPS_ENABLED, true);
					jettyProps.put(JettyConstants.SSL_KEYSTORETYPE, "PKCS12");
					jettyProps.put(JettyConstants.SSL_KEYSTORE, nodeSecurity
							.getHttpServerKeyStore().getCanonicalPath());
					jettyProps.put(JettyConstants.SSL_PASSWORD, "changeit");
					jettyProps.put(JettyConstants.SSL_WANTCLIENTAUTH, true);
				}
				if (conf != null) {
					// TODO make filter more generic
					String filter = "(" + JettyConstants.HTTP_PORT + "="
							+ httpPort + ")";
					if (conf.listConfigurations(filter) != null)
						return;
					Configuration jettyConf = conf.createFactoryConfiguration(
							JETTY_FACTORY_PID, null);
					jettyConf.update(jettyProps);
				} else {
					JettyConfigurator.startServer("default", jettyProps);
				}
			}
		} catch (Exception e) {
			throw new CmsException("Cannot initialize web server on "
					+ httpPortsMsg(httpPort, httpsPort), e);
		}
	}

	@SuppressWarnings("unchecked")
	private void publish() {
		// Listen to service publication (also ours)
		bc.addServiceListener(Kernel.this);

		// Logging
		loggerReg = bc.registerService(ArgeoLogger.class, logger, null);
		// Transaction
		tmReg = bc.registerService(TransactionManager.class,
				transactionManager, null);
		utReg = bc.registerService(UserTransaction.class, transactionManager,
				null);
		tsrReg = bc.registerService(TransactionSynchronizationRegistry.class,
				transactionSynchronizationRegistry, null);
		// User admin
		userAdminReg = bc.registerService(UserAdmin.class, userAdmin,
				userAdmin.currentState());
		// JCR
		Hashtable<String, String> regProps = new Hashtable<String, String>();
		regProps.put(JCR_REPOSITORY_ALIAS, ALIAS_NODE);
		repositoryReg = (ServiceRegistration<? extends Repository>) bc
				.registerService(new String[] { Repository.class.getName(),
						JackrabbitRepository.class.getName() }, repository,
						regProps);
		repositoryFactoryReg = bc.registerService(RepositoryFactory.class,
				repositoryFactory, null);
	}

	void destroy() {
		long begin = System.currentTimeMillis();
		unpublish();

		kernelThread.destroyAndJoin();

		if (dataHttp != null)
			dataHttp.destroy();
		if (userAdmin != null)
			userAdmin.destroy();
		if (repository != null)
			repository.destroy();
		if (transactionManager != null)
			transactionManager.shutdown();

		bc.removeServiceListener(this);

		// Clean hanging threads from Jackrabbit
		TransientFileFactory.shutdown();

		// Clean hanging Gogo shell thread
		new GogoShellKiller().start();

		nodeSecurity.destroy();
		long duration = System.currentTimeMillis() - begin;
		log.info("## ARGEO CMS DOWN in " + (duration / 1000) + "."
				+ (duration % 1000) + "s ##");
	}

	private void unpublish() {
		userAdminReg.unregister();
		repositoryFactoryReg.unregister();
		repositoryReg.unregister();
		tmReg.unregister();
		utReg.unregister();
		tsrReg.unregister();
		loggerReg.unregister();
	}

	@Override
	public void serviceChanged(ServiceEvent event) {
		ServiceReference<?> sr = event.getServiceReference();
		Object service = bc.getService(sr);
		if (service instanceof Repository) {
			Object jcrRepoAlias = sr
					.getProperty(ArgeoJcrConstants.JCR_REPOSITORY_ALIAS);
			if (jcrRepoAlias != null) {// JCR repository
				String alias = jcrRepoAlias.toString();
				Repository repository = (Repository) bc.getService(sr);
				Map<String, Object> props = new HashMap<String, Object>();
				for (String key : sr.getPropertyKeys())
					props.put(key, sr.getProperty(key));
				if (ServiceEvent.REGISTERED == event.getType()) {
					try {
						repositoryFactory.register(repository, props);
						dataHttp.registerRepositoryServlets(alias, repository);
					} catch (Exception e) {
						throw new CmsException(
								"Could not publish JCR repository " + alias, e);
					}
				} else if (ServiceEvent.UNREGISTERING == event.getType()) {
					repositoryFactory.unregister(repository, props);
					dataHttp.unregisterRepositoryServlets(alias);
				}
			}
		} else if (service instanceof ExtendedHttpService) {
			if (ServiceEvent.REGISTERED == event.getType()) {
				addHttpService(sr);
			} else if (ServiceEvent.UNREGISTERING == event.getType()) {
				dataHttp.destroy();
				dataHttp = null;
			}
		}
	}

	private void addHttpService(ServiceReference<?> sr) {
		// for (String key : sr.getPropertyKeys())
		// log.debug(key + "=" + sr.getProperty(key));
		ExtendedHttpService httpService = (ExtendedHttpService) bc
				.getService(sr);
		// TODO find constants
		Object httpPort = sr.getProperty("http.port");
		Object httpsPort = sr.getProperty("https.port");
		dataHttp = new DataHttp(httpService, repository);
		if (log.isDebugEnabled())
			log.debug(httpPortsMsg(httpPort, httpsPort));
	}

	private String httpPortsMsg(Object httpPort, Object httpsPort) {
		return "HTTP " + httpPort
				+ (httpsPort != null ? " - HTTPS " + httpsPort : "");
	}

	@Override
	public Locale getDefaultLocale() {
		return defaultLocale;
	}

	/** Can be null. */
	@Override
	public List<Locale> getLocales() {
		return locales;
	}

	final private static void directorsCut(long initDuration) {
		// final long ms = 128l + (long) (Math.random() * 128d);
		long ms = initDuration / 100;
		log.info("Spend " + ms + "ms"
				+ " reflecting on the progress brought to mankind"
				+ " by Free Software...");
		long beginNano = System.nanoTime();
		try {
			Thread.sleep(ms, 0);
		} catch (InterruptedException e) {
			// silent
		}
		long durationNano = System.nanoTime() - beginNano;
		final double M = 1000d * 1000d;
		double sleepAccuracy = ((double) durationNano) / (ms * M);
		if (log.isDebugEnabled())
			log.debug("Sleep accuracy: "
					+ String.format("%.2f", 100 - (sleepAccuracy * 100 - 100))
					+ " %");
	}

	/** Workaround for blocking Gogo shell by system shutdown. */
	private class GogoShellKiller extends Thread {

		public GogoShellKiller() {
			super("Gogo shell killer");
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
			if (!t.isDaemon())
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