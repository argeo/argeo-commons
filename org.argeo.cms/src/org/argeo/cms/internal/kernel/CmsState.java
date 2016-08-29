package org.argeo.cms.internal.kernel;

import static bitronix.tm.TransactionManagerServices.getTransactionManager;
import static bitronix.tm.TransactionManagerServices.getTransactionSynchronizationRegistry;
import static java.util.Locale.ENGLISH;
import static org.argeo.cms.internal.auth.LocaleChoice.asLocaleList;
import static org.argeo.cms.internal.kernel.KernelUtils.getFrameworkProp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.UUID;

import javax.jcr.RepositoryFactory;
import javax.naming.InvalidNameException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.RepositoryContext;
import org.argeo.cms.CmsException;
import org.argeo.cms.maintenance.MaintenanceUi;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.node.NodeConstants;
import org.argeo.node.NodeDeployment;
import org.argeo.node.NodeState;
import org.argeo.node.RepoConf;
import org.argeo.util.LangUtils;
import org.argeo.util.naming.AttributesDictionary;
import org.argeo.util.naming.LdifParser;
import org.eclipse.equinox.http.jetty.JettyConfigurator;
import org.eclipse.equinox.http.jetty.JettyConstants;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.http.HttpService;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.BitronixTransactionSynchronizationRegistry;
import bitronix.tm.TransactionManagerServices;

public class CmsState implements NodeState, ManagedService {
	private final Log log = LogFactory.getLog(CmsState.class);
	private final BundleContext bc = FrameworkUtil.getBundle(CmsState.class).getBundleContext();

	// avoid dependency to RWT OSGi
	private final static String PROPERTY_CONTEXT_NAME = "contextName";

	// REFERENCES
	private ConfigurationAdmin configurationAdmin;

	// i18n
	private Locale defaultLocale;
	private List<Locale> locales = null;

	// private BitronixTransactionManager transactionManager;
	// private BitronixTransactionSynchronizationRegistry
	// transactionSynchronizationRegistry;
	// private NodeRepositoryFactory repositoryFactory;
	// private NodeUserAdmin userAdmin;
	// private RepositoryServiceFactory repositoryServiceFactory;
	// private RepositoryService repositoryService;

	// Deployment
	// private final CmsDeployment nodeDeployment = new CmsDeployment();

	private boolean cleanState = false;
	private URI nodeRepoUri = null;

	private ThreadGroup threadGroup = new ThreadGroup("CMS");
	private KernelThread kernelThread;
	private List<Runnable> shutdownHooks = new ArrayList<>();

	private String hostname;

	public CmsState() {
		try {
			this.hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			log.error("Cannot set hostname", e);
		}
	}

	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		if (properties == null) {
			// // TODO this should not happen anymore
			// this.cleanState = true;
			// if (log.isTraceEnabled())
			// log.trace("Clean state");
			return;
		}
		String stateUuid = properties.get(NodeConstants.CN).toString();
		String frameworkUuid = KernelUtils.getFrameworkProp(Constants.FRAMEWORK_UUID);
		this.cleanState = stateUuid.equals(frameworkUuid);

		try {
			if (log.isDebugEnabled())
				log.debug("## CMS STARTED " + stateUuid + (cleanState ? " (clean state) " : " ")
						+ LangUtils.toJson(properties, true));
			configurationAdmin = bc.getService(bc.getServiceReference(ConfigurationAdmin.class));

			nodeRepoUri = KernelUtils.getOsgiInstanceUri("repos/node");

			initI18n();
			initServices();
//			initDeployConfigs();
			// initWebServer();
			// initNodeDeployment();

			// kernel thread
			kernelThread = new KernelThread(threadGroup, "Kernel Thread");
			kernelThread.setContextClassLoader(getClass().getClassLoader());
			kernelThread.start();
		} catch (Exception e) {
			throw new CmsException("Cannot get configuration", e);
		}
	}

	private void initI18n() {
		Object defaultLocaleValue = KernelUtils.getFrameworkProp(NodeConstants.I18N_DEFAULT_LOCALE);
		defaultLocale = defaultLocaleValue != null ? new Locale(defaultLocaleValue.toString())
				: new Locale(ENGLISH.getLanguage());
		locales = asLocaleList(KernelUtils.getFrameworkProp(NodeConstants.I18N_LOCALES));
	}

	private void initServices() {
		// trackers
		new ServiceTracker<HttpService, HttpService>(bc, HttpService.class, new PrepareHttpStc()).open();

		initTransactionManager();

		// JCR
		RepositoryServiceFactory repositoryServiceFactory = new RepositoryServiceFactory();
		shutdownHooks.add(() -> repositoryServiceFactory.shutdown());
		bc.registerService(ManagedServiceFactory.class, repositoryServiceFactory,
				LangUtils.init(Constants.SERVICE_PID, NodeConstants.NODE_REPOS_FACTORY_PID));

		NodeRepositoryFactory repositoryFactory = new NodeRepositoryFactory();
		bc.registerService(RepositoryFactory.class, repositoryFactory, null);

		// RepositoryService repositoryService = new RepositoryService();
		// shutdownHooks.add(() -> repositoryService.shutdown());
		// bc.registerService(LangUtils.names(ManagedService.class,
		// MetaTypeProvider.class), repositoryService,
		// LangUtils.init(Constants.SERVICE_PID, NodeConstants.NODE_REPO_PID));

		// Security
		NodeUserAdmin userAdmin = new NodeUserAdmin();
		shutdownHooks.add(() -> userAdmin.destroy());
		Dictionary<String, Object> props = userAdmin.currentState();
		props.put(Constants.SERVICE_PID, NodeConstants.NODE_USER_ADMIN_PID);
		bc.registerService(UserAdmin.class, userAdmin, props);

		// UI
		bc.registerService(ApplicationConfiguration.class, new MaintenanceUi(),
				LangUtils.init(PROPERTY_CONTEXT_NAME, "system"));
		bc.registerService(ApplicationConfiguration.class, new UserUi(), LangUtils.init(PROPERTY_CONTEXT_NAME, "user"));
	}
	// private void initUserAdmin() {
	// userAdmin = new NodeUserAdmin();
	// // register
	// Dictionary<String, Object> props = userAdmin.currentState();
	// props.put(Constants.SERVICE_PID, NodeConstants.NODE_USER_ADMIN_PID);
	// // TODO use ManagedService
	// bc.registerService(UserAdmin.class, userAdmin, props);
	// }

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

	// private void initRepositoryFactory() {
	// // TODO rationalise RepositoryFactory
	// repositoryFactory = new NodeRepositoryFactory();
	// // register
	// bc.registerService(RepositoryFactory.class, repositoryFactory, null);
	// }

	// private void initUi() {
	// bc.registerService(ApplicationConfiguration.class, new MaintenanceUi(),
	// LangUtils.init(PROPERTY_CONTEXT_NAME, "system"));
	// bc.registerService(ApplicationConfiguration.class, new UserUi(),
	// LangUtils.init(PROPERTY_CONTEXT_NAME, "user"));
	// }

	// private void initRepositories(Dictionary<String, ?> stateProps) throws
	// IOException {
	// // register
	// repositoryServiceFactory = new RepositoryServiceFactory();
	// bc.registerService(ManagedServiceFactory.class, repositoryServiceFactory,
	// LangUtils.init(Constants.SERVICE_PID,
	// NodeConstants.JACKRABBIT_FACTORY_PID));
	//
	// repositoryService = new RepositoryService();
	// Dictionary<String, Object> regProps =
	// LangUtils.init(Constants.SERVICE_PID, NodeConstants.NODE_REPO_PID);
	// bc.registerService(LangUtils.names(ManagedService.class,
	// MetaTypeProvider.class), repositoryService, regProps);
	// }

	// private void initNodeDeployment() throws IOException {
	// Configuration nodeDeploymentConf =
	// configurationAdmin.getConfiguration(NodeConstants.NODE_DEPLOYMENT_PID);
	// nodeDeploymentConf.update(new Hashtable<>());
	// }

	void shutdown() {
		// if (transactionManager != null)
		// transactionManager.shutdown();
		// if (userAdmin != null)
		// userAdmin.destroy();
		// if (repositoryServiceFactory != null)
		// repositoryServiceFactory.shutdown();

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
				// new Thread(shutdownHooks.get(i), "CMS Shutdown Hook #" +
				// i).start();
				shutdownHooks.get(i).run();
			} catch (Exception e) {
				log.error("Could not run shutdown hook #" + i);
			}
		}
		// Clean hanging Gogo shell thread
		new GogoShellKiller().start();
	}

	private class PrepareHttpStc implements ServiceTrackerCustomizer<HttpService, HttpService> {
		private DataHttp dataHttp;
		private NodeHttp nodeHttp;

		@Override
		public HttpService addingService(ServiceReference<HttpService> reference) {
			HttpService httpService = addHttpService(reference);
			return httpService;
		}

		@Override
		public void modifiedService(ServiceReference<HttpService> reference, HttpService service) {
		}

		@Override
		public void removedService(ServiceReference<HttpService> reference, HttpService service) {
			if (dataHttp != null)
				dataHttp.destroy();
			dataHttp = null;
			if (nodeHttp != null)
				nodeHttp.destroy();
			nodeHttp = null;
		}

		private HttpService addHttpService(ServiceReference<HttpService> sr) {
			HttpService httpService = bc.getService(sr);
			// TODO find constants
			Object httpPort = sr.getProperty("http.port");
			Object httpsPort = sr.getProperty("https.port");
			dataHttp = new DataHttp(httpService);
			nodeHttp = new NodeHttp(httpService, bc);
			if (log.isDebugEnabled())
				log.debug(httpPortsMsg(httpPort, httpsPort));
			return httpService;
		}

	}

	@Override
	public boolean isClean() {
		return cleanState;
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

	/*
	 * STATIC
	 */
	private static String httpPortsMsg(Object httpPort, Object httpsPort) {
		return "HTTP " + httpPort + (httpsPort != null ? " - HTTPS " + httpsPort : "");
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
