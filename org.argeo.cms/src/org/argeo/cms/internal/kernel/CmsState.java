package org.argeo.cms.internal.kernel;

import static bitronix.tm.TransactionManagerServices.getTransactionManager;
import static bitronix.tm.TransactionManagerServices.getTransactionSynchronizationRegistry;
import static java.util.Locale.ENGLISH;
import static org.argeo.cms.internal.kernel.KernelUtils.getFrameworkProp;
import static org.argeo.util.LocaleChoice.asLocaleList;
import static org.osgi.framework.Constants.FRAMEWORK_UUID;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import javax.jcr.RepositoryFactory;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.argeo.cms.CmsException;
import org.argeo.jackrabbit.OsgiJackrabbitRepositoryFactory;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.node.NodeConstants;
import org.argeo.node.NodeDeployment;
import org.argeo.node.NodeState;
import org.argeo.node.RepoConf;
import org.argeo.util.LangUtils;
import org.eclipse.equinox.http.jetty.JettyConfigurator;
import org.eclipse.equinox.http.jetty.JettyConstants;
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
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.BitronixTransactionSynchronizationRegistry;
import bitronix.tm.TransactionManagerServices;

public class CmsState implements NodeState, ManagedService {
	private final Log log = LogFactory.getLog(CmsState.class);
	private final BundleContext bc = FrameworkUtil.getBundle(CmsState.class).getBundleContext();

	// REFERENCES
	private ConfigurationAdmin configurationAdmin;

	// i18n
	private Locale defaultLocale;
	private List<Locale> locales = null;

	// Standalone services
	private BitronixTransactionManager transactionManager;
	private BitronixTransactionSynchronizationRegistry transactionSynchronizationRegistry;
	private OsgiJackrabbitRepositoryFactory repositoryFactory;

	// Security
	private NodeUserAdmin userAdmin;
	private JackrabbitRepositoryServiceFactory repositoryServiceFactory;

	// Deployment
	private final CmsDeployment nodeDeployment = new CmsDeployment();

	private boolean cleanState = false;
	private URI nodeRepoUri = null;

	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		if (properties == null) {
			this.cleanState = true;
			if (log.isTraceEnabled())
				log.trace("Clean state");
			return;
		}

		try {
			if (log.isDebugEnabled())
				log.debug(
						"## CMS STARTED " + (cleanState ? " (clean state) " : "") + LangUtils.toJson(properties, true));
			configurationAdmin = bc.getService(bc.getServiceReference(ConfigurationAdmin.class));

			initI18n(properties);
			initTrackers();
			initTransactionManager();
			initRepositoryFactory();
			initUserAdmin();
			initRepositories(properties);
			initWebServer();
			initNodeDeployment();
		} catch (Exception e) {
			throw new CmsException("Cannot get configuration", e);
		}
	}

	private void initTrackers() {
		new ServiceTracker<HttpService, HttpService>(bc, HttpService.class, new PrepareHttpStc()).open();
		new ServiceTracker<>(bc, JackrabbitRepository.class, new JackrabbitrepositoryStc()).open();
	}

	private void initI18n(Dictionary<String, ?> stateProps) {
		Object defaultLocaleValue = stateProps.get(NodeConstants.I18N_DEFAULT_LOCALE);
		defaultLocale = defaultLocaleValue != null ? new Locale(defaultLocaleValue.toString())
				: new Locale(ENGLISH.getLanguage());
		locales = asLocaleList(stateProps.get(NodeConstants.I18N_LOCALES));
	}

	private void initTransactionManager() {
		bitronix.tm.Configuration tmConf = TransactionManagerServices.getConfiguration();
		tmConf.setServerId(getFrameworkProp(FRAMEWORK_UUID));

		Bundle bitronixBundle = FrameworkUtil.getBundle(bitronix.tm.Configuration.class);
		File tmBaseDir = bitronixBundle.getDataFile(KernelConstants.DIR_TRANSACTIONS);
		File tmDir1 = new File(tmBaseDir, "btm1");
		tmDir1.mkdirs();
		tmConf.setLogPart1Filename(new File(tmDir1, tmDir1.getName() + ".tlog").getAbsolutePath());
		File tmDir2 = new File(tmBaseDir, "btm2");
		tmDir2.mkdirs();
		tmConf.setLogPart2Filename(new File(tmDir2, tmDir2.getName() + ".tlog").getAbsolutePath());
		transactionManager = getTransactionManager();
		transactionSynchronizationRegistry = getTransactionSynchronizationRegistry();
		// register
		bc.registerService(TransactionManager.class, transactionManager, null);
		bc.registerService(UserTransaction.class, transactionManager, null);
		bc.registerService(TransactionSynchronizationRegistry.class, transactionSynchronizationRegistry, null);
	}

	private void initRepositoryFactory() {
		// TODO rationalise RepositoryFactory
		repositoryFactory = new OsgiJackrabbitRepositoryFactory();
		repositoryFactory.setBundleContext(bc);
		// register
		bc.registerService(RepositoryFactory.class, repositoryFactory, null);
	}

	private void initUserAdmin() {
		userAdmin = new NodeUserAdmin();
		// register
		Dictionary<String, Object> props = userAdmin.currentState();
		props.put(Constants.SERVICE_PID, NodeConstants.NODE_REPO_PID);
		// TODO use ManagedService
		bc.registerService(UserAdmin.class, userAdmin, props);
	}

	private void initRepositories(Dictionary<String, ?> stateProps) throws IOException {
		nodeRepoUri = KernelUtils.getOsgiInstanceUri("repos/node");
		// register
		repositoryServiceFactory = new JackrabbitRepositoryServiceFactory();
		bc.registerService(ManagedServiceFactory.class, repositoryServiceFactory,
				LangUtils.init(Constants.SERVICE_PID, NodeConstants.JACKRABBIT_FACTORY_PID));

		if (cleanState) {
			Configuration newNodeConf = configurationAdmin
					.createFactoryConfiguration(NodeConstants.JACKRABBIT_FACTORY_PID);
			Dictionary<String, Object> props = getNodeConfig(stateProps);
			if (props == null) {
				if (log.isDebugEnabled())
					log.debug("No argeo.node.repo.type=localfs|h2|postgresql|memory"
							+ " property defined, entering interactive mode...");
				// TODO interactive configuration
				return;
			}
			// props.put(Constants.SERVICE_PID, NodeConstants.NODE_REPO_PID);
			props.put(RepoConf.uri.name(), nodeRepoUri.toString());
			newNodeConf.update(props);
		}
	}

	private void initWebServer() {
		String httpPort = getFrameworkProp("org.osgi.service.http.port");
		String httpsPort = getFrameworkProp("org.osgi.service.http.port.secure");
		try {
			if (httpPort != null || httpsPort != null) {
				final Hashtable<String, Object> jettyProps = new Hashtable<String, Object>();
				if (httpPort != null) {
					jettyProps.put(JettyConstants.HTTP_PORT, httpPort);
					jettyProps.put(JettyConstants.HTTP_ENABLED, true);
				}
				if (httpsPort != null) {
					jettyProps.put(JettyConstants.HTTPS_PORT, httpsPort);
					jettyProps.put(JettyConstants.HTTPS_ENABLED, true);
					jettyProps.put(JettyConstants.SSL_KEYSTORETYPE, "PKCS12");
					// jettyProps.put(JettyConstants.SSL_KEYSTORE,
					// nodeSecurity.getHttpServerKeyStore().getCanonicalPath());
					jettyProps.put(JettyConstants.SSL_PASSWORD, "changeit");
					jettyProps.put(JettyConstants.SSL_WANTCLIENTAUTH, true);
				}
				if (configurationAdmin != null) {
					// TODO make filter more generic
					String filter = "(" + JettyConstants.HTTP_PORT + "=" + httpPort + ")";
					if (configurationAdmin.listConfigurations(filter) != null)
						return;
					Configuration jettyConf = configurationAdmin
							.createFactoryConfiguration(KernelConstants.JETTY_FACTORY_PID, null);
					jettyConf.update(jettyProps);

				} else {
					JettyConfigurator.startServer("default", jettyProps);
				}
			}
		} catch (Exception e) {
			throw new CmsException("Cannot initialize web server on " + httpPortsMsg(httpPort, httpsPort), e);
		}
	}

	private void initNodeDeployment() throws IOException {
		Configuration nodeDeploymentConf = configurationAdmin.getConfiguration(NodeConstants.NODE_DEPLOYMENT_PID);
		nodeDeploymentConf.update(new Hashtable<>());
	}

	void shutdown() {
		if (transactionManager != null)
			transactionManager.shutdown();
		if (userAdmin != null)
			userAdmin.destroy();
		if (repositoryServiceFactory != null)
			repositoryServiceFactory.shutdown();

		// Clean hanging Gogo shell thread
		new GogoShellKiller().start();
		
		if (log.isDebugEnabled())
			log.debug("## CMS STOPPED");
	}

	private Dictionary<String, Object> getNodeConfig(Dictionary<String, ?> properties) {
		Object repoType = properties.get(NodeConstants.NODE_REPO_PROP_PREFIX + RepoConf.type.name());
		if (repoType == null)
			return null;

		Hashtable<String, Object> props = new Hashtable<String, Object>();
		for (RepoConf repoConf : RepoConf.values()) {
			Object value = properties.get(NodeConstants.NODE_REPO_PROP_PREFIX + repoConf.name());
			if (value != null)
				props.put(repoConf.name(), value);
		}
		return props;
	}

	private class JackrabbitrepositoryStc
			implements ServiceTrackerCustomizer<JackrabbitRepository, JackrabbitRepository> {

		@Override
		public JackrabbitRepository addingService(ServiceReference<JackrabbitRepository> reference) {
			JackrabbitRepository nodeRepo = bc.getService(reference);
			Object repoUri = reference.getProperty(ArgeoJcrConstants.JCR_REPOSITORY_URI);
			if (repoUri != null && repoUri.equals(nodeRepoUri.toString())) {
				nodeDeployment.setDeployedNodeRepository(nodeRepo);
				// register
				bc.registerService(LangUtils.names(NodeDeployment.class, ManagedService.class), nodeDeployment,
						LangUtils.init(Constants.SERVICE_PID, NodeConstants.NODE_DEPLOYMENT_PID));
			}

			return nodeRepo;
		}

		@Override
		public void modifiedService(ServiceReference<JackrabbitRepository> reference, JackrabbitRepository service) {
		}

		@Override
		public void removedService(ServiceReference<JackrabbitRepository> reference, JackrabbitRepository service) {
		}

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
			dataHttp.destroy();
			dataHttp = null;
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

	/*
	 * ACCESSORS
	 */
	public Locale getDefaultLocale() {
		return defaultLocale;
	}

	public List<Locale> getLocales() {
		return locales;
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
