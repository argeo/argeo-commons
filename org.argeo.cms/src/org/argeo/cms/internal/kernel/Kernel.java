package org.argeo.cms.internal.kernel;

import static bitronix.tm.TransactionManagerServices.getTransactionManager;
import static bitronix.tm.TransactionManagerServices.getTransactionSynchronizationRegistry;
import static java.util.Locale.ENGLISH;
import static org.argeo.cms.internal.kernel.KernelUtils.getFrameworkProp;
import static org.argeo.cms.internal.kernel.KernelUtils.getOsgiInstanceDir;
import static org.argeo.node.DataModelNamespace.CMS_DATA_MODEL_NAMESPACE;
import static org.argeo.util.LocaleChoice.asLocaleList;
import static org.osgi.framework.Constants.FRAMEWORK_UUID;

import java.io.File;
import java.io.FileFilter;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.ReflectPermission;
import java.net.SocketPermission;
import java.net.URL;
import java.security.AllPermission;
import java.security.PrivilegedAction;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.Set;

import javax.jcr.Repository;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;
import javax.management.MBeanPermission;
import javax.management.MBeanServerPermission;
import javax.management.MBeanTrustPermission;
import javax.security.auth.AuthPermission;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.util.TransientFileFactory;
import org.argeo.ArgeoException;
import org.argeo.ArgeoLogger;
import org.argeo.cms.CmsException;
import org.argeo.cms.maintenance.MaintenanceUi;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.JcrUtils;
import org.argeo.node.DataModelNamespace;
import org.argeo.node.NodeConstants;
import org.argeo.node.RepoConf;
import org.eclipse.equinox.http.jetty.JettyConfigurator;
import org.eclipse.equinox.http.jetty.JettyConstants;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationPermission;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.condpermadmin.BundleLocationCondition;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.permissionadmin.PermissionInfo;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

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
@Deprecated
final class Kernel implements KernelHeader, KernelConstants {
	/*
	 * SERVICE REFERENCES
	 */
	// private ServiceReference<ConfigurationAdmin> configurationAdmin;
	private final ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> configurationAdmin;
	private final ServiceTracker<LogReaderService, LogReaderService> logReaderService;
	private final ServiceTracker<HttpService, HttpService> httpService;
	private final ConditionalPermissionAdmin permissionAdmin;
	/*
	 * REGISTERED SERVICES
	 */
	private ServiceRegistration<ArgeoLogger> loggerReg;
	private ServiceRegistration<TransactionManager> tmReg;
	private ServiceRegistration<UserTransaction> utReg;
	private ServiceRegistration<TransactionSynchronizationRegistry> tsrReg;
	private ServiceRegistration<?> repositoryReg;
	private ServiceRegistration<RepositoryFactory> repositoryFactoryReg;
	private ServiceRegistration<UserAdmin> userAdminReg;

	/*
	 * SERVICES IMPLEMENTATIONS
	 */
	private NodeLogger logger;
	private BitronixTransactionManager transactionManager;
	private BitronixTransactionSynchronizationRegistry transactionSynchronizationRegistry;
	private NodeRepositoryFactory repositoryFactory;
	private Repository repository;
	private NodeUserAdmin userAdmin;

	// Members
	private final BundleContext bc;// = Activator.getBundleContext();
	private final NodeSecurity nodeSecurity;

	private final static Log log = LogFactory.getLog(Kernel.class);
	ThreadGroup threadGroup = new ThreadGroup(Kernel.class.getSimpleName());
	private DataHttp dataHttp;
	private NodeHttp nodeHttp;
	private KernelThread kernelThread;

	private Locale defaultLocale = null;
	private List<Locale> locales = null;

	public Kernel() {
		// KernelUtils.logFrameworkProperties(log);
		nodeSecurity = new NodeSecurity();
		bc = FrameworkUtil.getBundle(getClass()).getBundleContext();
		configurationAdmin = new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(bc, ConfigurationAdmin.class,
				new PrepareStc<ConfigurationAdmin>());
		configurationAdmin.open();
		logReaderService = new ServiceTracker<LogReaderService, LogReaderService>(bc, LogReaderService.class,
				new PrepareStc<LogReaderService>());
		logReaderService.open();
		httpService = new ServiceTracker<HttpService, HttpService>(bc, HttpService.class, new PrepareHttpStc());
		httpService.open();

		permissionAdmin = bc.getService(bc.getServiceReference(ConditionalPermissionAdmin.class));
	}

	/*
	 * PACKAGE RESTRICTED INTERFACE
	 */
	Subject getKernelSubject() {
		return nodeSecurity.getKernelSubject();
	}

	/*
	 * INITIALISATION
	 */

	final void init() {
		Subject.doAs(nodeSecurity.getKernelSubject(), new PrivilegedAction<Void>() {
			@Override
			public Void run() {
				doInit();
				return null;
			}
		});
	}

	private void doInit() {
		long begin = System.currentTimeMillis();
		// Use CMS bundle classloader
		ClassLoader currentContextCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(Kernel.class.getClassLoader());
		try {
			// Listen to service publication (also ours)
			// bc.addServiceListener(Kernel.this);

			if (nodeSecurity.isFirstInit())
				firstInit();

			defaultLocale = new Locale(getFrameworkProp(NodeConstants.I18N_DEFAULT_LOCALE, ENGLISH.getLanguage()));
			locales = asLocaleList(getFrameworkProp(NodeConstants.I18N_LOCALES));

			// ServiceTracker<LogReaderService, LogReaderService>
			// logReaderService = new ServiceTracker<LogReaderService,
			// LogReaderService>(
			// bc, LogReaderService.class, null);
			// logReaderService.open();
			logger = new NodeLogger(logReaderService.getService());
			// logReaderService.close();

			if (isMaintenance())
				maintenanceInit();
			else
				normalInit();
		} catch (Throwable e) {
			log.error("Cannot initialize Argeo CMS", e);
			throw new ArgeoException("Cannot initialize", e);
		} finally {
			Thread.currentThread().setContextClassLoader(currentContextCl);
			// FIXME better manage lifecycle.
			try {
				new LoginContext(LOGIN_CONTEXT_KERNEL, nodeSecurity.getKernelSubject()).logout();
			} catch (LoginException e) {
				e.printStackTrace();
			}
		}

		long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
		log.info("## ARGEO CMS UP in " + (jvmUptime / 1000) + "." + (jvmUptime % 1000) + "s ##");
		long initDuration = System.currentTimeMillis() - begin;
		if (log.isTraceEnabled())
			log.trace("Kernel initialization took " + initDuration + "ms");
		directorsCut(initDuration);
	}

	private void normalInit() {
		ConfigurationAdmin conf = findConfigurationAdmin();

		// HTTP
		initWebServer(conf);
		// ServiceReference<ExtendedHttpService> sr =
		// bc.getServiceReference(ExtendedHttpService.class);
		// if (sr != null)
		// addHttpService(sr);
		// else
		// log.warn("No http service found");

		// Initialise services
		initTransactionManager();

		RepositoryServiceFactory jrsf = new RepositoryServiceFactory();
		String[] clazzes = { ManagedServiceFactory.class.getName() };
		Hashtable<String, String> serviceProps = new Hashtable<String, String>();
		serviceProps.put(Constants.SERVICE_PID, ArgeoJcrConstants.JACKRABBIT_REPO_FACTORY_PID);
		bc.registerService(clazzes, jrsf, serviceProps);

		try {
			Configuration nodeConf = conf.createFactoryConfiguration(ArgeoJcrConstants.JACKRABBIT_REPO_FACTORY_PID);
			// Configuration nodeConf =
			// conf.getConfiguration(ArgeoJcrConstants.REPO_PID_NODE);
			if (nodeConf.getProperties() == null) {
				Dictionary<String, Object> props = getNodeConfigFromFrameworkProperties();
				if (props == null) {
					// TODO interactive configuration
					if (log.isDebugEnabled())
						log.debug("No argeo.node.repo.type=localfs|h2|postgresql|memory"
								+ " property defined, entering interactive mode...");
					return;
				}
				// props.put(ConfigurationAdmin.SERVICE_FACTORYPID,
				// ArgeoJcrConstants.JACKRABBIT_REPO_FACTORY_PID);
				props.put(Constants.SERVICE_PID, ArgeoJcrConstants.REPO_PID_NODE);
				nodeConf.update(props);
			}
		} catch (IOException e) {
			throw new CmsException("Cannot get configuration", e);
		}

		// ManagedJackrabbitRepository nodeRepo = new
		// ManagedJackrabbitRepository();
		// String[] clazzes = { ManagedService.class.getName(),
		// Repository.class.getName(),
		// JackrabbitRepository.class.getName() };
		// Hashtable<String, String> serviceProps = new Hashtable<String,
		// String>();
		// serviceProps.put(Constants.SERVICE_PID,
		// ArgeoJcrConstants.REPO_PID_NODE);
		// serviceProps.put(ArgeoJcrConstants.JCR_REPOSITORY_ALIAS,
		// ArgeoJcrConstants.ALIAS_NODE);
		// repositoryReg = bc.registerService(clazzes, nodeRepo, serviceProps);
		// nodeRepo.waitForInit();

		ServiceTracker<JackrabbitRepository, JackrabbitRepository> jackrabbitSt = new ServiceTracker<>(bc,
				JackrabbitRepository.class, new ServiceTrackerCustomizer<JackrabbitRepository, JackrabbitRepository>() {

					@Override
					public JackrabbitRepository addingService(ServiceReference<JackrabbitRepository> reference) {
						JackrabbitRepository nodeRepo = bc.getService(reference);
						// new
						// JackrabbitDataModel(bc).prepareDataModel(nodeRepo);
						prepareDataModel(KernelUtils.openAdminSession(nodeRepo));

						// repository = (JackrabbitRepository)
						// bc.getService(repositoryReg.getReference());
						repository = new HomeRepository(nodeRepo);
						Hashtable<String, String> regProps = new Hashtable<String, String>();
						regProps.put(ArgeoJcrConstants.JCR_REPOSITORY_ALIAS, ArgeoJcrConstants.ALIAS_NODE);
						repositoryReg = (ServiceRegistration<? extends Repository>) bc.registerService(Repository.class,
								repository, regProps);

						// if (repository == null)
						// repository = new NodeRepository();
						if (repositoryFactory == null) {
							repositoryFactory = new NodeRepositoryFactory();
							// repositoryFactory.setBundleContext(bc);
							repositoryFactoryReg = bc.registerService(RepositoryFactory.class, repositoryFactory, null);
						}
						userAdmin = new NodeUserAdmin(transactionManager, repository);
						userAdminReg = bc.registerService(UserAdmin.class, userAdmin, userAdmin.currentState());
						return nodeRepo;
					}

					@Override
					public void modifiedService(ServiceReference<JackrabbitRepository> reference,
							JackrabbitRepository service) {
					}

					@Override
					public void removedService(ServiceReference<JackrabbitRepository> reference,
							JackrabbitRepository service) {
					}
				});
		jackrabbitSt.open();

		// new JackrabbitDataModel(bc).prepareDataModel(nodeRepo);
		// prepareDataModel(nodeRepo);
		//
		// repository = (JackrabbitRepository)
		// bc.getService(repositoryReg.getReference());
		//
		//// if (repository == null)
		//// repository = new NodeRepository();
		// if (repositoryFactory == null) {
		// repositoryFactory = new OsgiJackrabbitRepositoryFactory();
		// repositoryFactory.setBundleContext(bc);
		// }
		// userAdmin = new NodeUserAdmin(transactionManager, repository);

		// ADMIN UIs
		UserUi userUi = new UserUi();
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put("contextName", "user");
		bc.registerService(ApplicationConfiguration.class, userUi, props);

		// Bundle rapWorkbenchBundle =
		// findBundle("org.eclipse.rap.ui.workbench");
		// if (rapWorkbenchBundle != null)
		// try {
		// Class<?> clss = rapWorkbenchBundle
		// .loadClass("org.eclipse.rap.ui.internal.servlet.WorkbenchApplicationConfiguration");
		//
		// Hashtable<String, String> rapWorkbenchProps = new Hashtable<String,
		// String>();
		// rapWorkbenchProps.put("contextName", "ui");
		// ApplicationConfiguration workbenchApplicationConfiguration =
		// (ApplicationConfiguration) clss
		// .newInstance();
		// bc.registerService(ApplicationConfiguration.class,
		// workbenchApplicationConfiguration,
		// rapWorkbenchProps);
		// } catch (Exception e) {
		// log.error("Cannot initalize RAP workbench", e);
		// }

		// Kernel thread
//		kernelThread = new KernelThread(this);
//		kernelThread.setContextClassLoader(Kernel.class.getClassLoader());
//		kernelThread.start();

		// Publish services to OSGi
		publish();
	}

	private Dictionary<String, Object> getNodeConfigFromFrameworkProperties() {
		String repoType = KernelUtils.getFrameworkProp(NodeConstants.NODE_REPO_PROP_PREFIX + RepoConf.type.name());
		if (repoType == null)
			return null;

		Hashtable<String, Object> props = new Hashtable<String, Object>();
		for (RepoConf repoConf : RepoConf.values()) {
			String value = KernelUtils.getFrameworkProp(NodeConstants.NODE_REPO_PROP_PREFIX + repoConf.name());
			if (value != null)
				props.put(repoConf.name(), value);
		}
		return props;
	}

	/** Session is logged out. */
	private void prepareDataModel(Session adminSession) {
		try {
			Set<String> processed = new HashSet<String>();
			bundles: for (Bundle bundle : bc.getBundles()) {
				BundleWiring wiring = bundle.adapt(BundleWiring.class);
				if (wiring == null) {
					if (log.isTraceEnabled())
						log.error("No wiring for " + bundle.getSymbolicName());
					continue bundles;
				}
				processWiring(adminSession, wiring, processed);
			}
		} finally {
			JcrUtils.logoutQuietly(adminSession);
		}
	}

	private void processWiring(Session adminSession, BundleWiring wiring, Set<String> processed) {
		// recursively process requirements first
		List<BundleWire> requiredWires = wiring.getRequiredWires(CMS_DATA_MODEL_NAMESPACE);
		for (BundleWire wire : requiredWires) {
			processWiring(adminSession, wire.getProviderWiring(), processed);
			// registerCnd(adminSession, wire.getCapability(), processed);
		}
		List<BundleCapability> capabilities = wiring.getCapabilities(CMS_DATA_MODEL_NAMESPACE);
		for (BundleCapability capability : capabilities) {
			registerCnd(adminSession, capability, processed);
		}
	}

	private void registerCnd(Session adminSession, BundleCapability capability, Set<String> processed) {
		Map<String, Object> attrs = capability.getAttributes();
		String name = attrs.get(DataModelNamespace.CAPABILITY_NAME_ATTRIBUTE).toString();
		if (processed.contains(name)) {
			if (log.isTraceEnabled())
				log.trace("Data model " + name + " has already been processed");
			return;
		}
		String path = attrs.get(DataModelNamespace.CAPABILITY_CND_ATTRIBUTE).toString();
		URL url = capability.getRevision().getBundle().getResource(path);
		try (Reader reader = new InputStreamReader(url.openStream())) {
			CndImporter.registerNodeTypes(reader, adminSession, true);
			processed.add(name);
			if (log.isDebugEnabled())
				log.debug("Registered CND " + url);
		} catch (Exception e) {
			throw new CmsException("Cannot read cnd " + url, e);
		}

		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(ArgeoJcrConstants.JCR_REPOSITORY_ALIAS, name);
		bc.registerService(Repository.class, adminSession.getRepository(), properties);
		if (log.isDebugEnabled())
			log.debug("Published data model " + name);
	}

	private boolean isMaintenance() {
		String startLevel = KernelUtils.getFrameworkProp("osgi.startLevel");
		if (startLevel == null)
			return false;
		int bundleStartLevel = bc.getBundle().adapt(BundleStartLevel.class).getStartLevel();
		// int frameworkStartLevel =
		// bc.getBundle(0).adapt(BundleStartLevel.class)
		// .getStartLevel();
		int frameworkStartLevel = Integer.parseInt(startLevel);
		// int frameworkStartLevel = bc.getBundle(0)
		// .adapt(FrameworkStartLevel.class).getStartLevel();
		return bundleStartLevel == frameworkStartLevel;
	}

	private void maintenanceInit() {
		log.info("## MAINTENANCE ##");
		// bc.addServiceListener(Kernel.this);
		initWebServer(null);
		MaintenanceUi maintenanceUi = new MaintenanceUi();
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put("contextName", "maintenance");
		bc.registerService(ApplicationConfiguration.class, maintenanceUi, props);
	}

	private void firstInit() {
		log.info("## FIRST INIT ##");
		String nodeInit = getFrameworkProp(NodeConstants.NODE_INIT);
		if (nodeInit == null)
			nodeInit = "../../init";
		if (nodeInit.startsWith("http")) {
			// remoteFirstInit(nodeInit);
			return;
		}
		File initDir;
		if (nodeInit.startsWith("."))
			initDir = KernelUtils.getExecutionDir(nodeInit);
		else
			initDir = new File(nodeInit);
		// TODO also uncompress archives
		if (initDir.exists())
			try {
				FileUtils.copyDirectory(initDir, getOsgiInstanceDir(), new FileFilter() {

					@Override
					public boolean accept(File pathname) {
						if (pathname.getName().equals(".svn") || pathname.getName().equals(".git"))
							return false;
						return true;
					}
				});
				log.info("CMS initialized from " + initDir.getCanonicalPath());
			} catch (IOException e) {
				throw new CmsException("Cannot initialize from " + initDir, e);
			}
	}

	// private void remoteFirstInit(String uri) {
	// try {
	// repository = new NodeRepository();
	// repositoryFactory = new OsgiJackrabbitRepositoryFactory();
	// Repository remoteRepository =
	// ArgeoJcrUtils.getRepositoryByUri(repositoryFactory, uri);
	// Session remoteSession = remoteRepository.login(new
	// SimpleCredentials("root", "demo".toCharArray()), "main");
	// Session localSession = this.repository.login();
	// // FIXME register node type
	// // if (false)
	// // CndImporter.registerNodeTypes(null, localSession);
	// ByteArrayOutputStream out = new ByteArrayOutputStream();
	// remoteSession.exportSystemView("/", out, true, false);
	// ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
	// localSession.importXML("/", in,
	// ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
	// // JcrUtils.copy(remoteSession.getRootNode(),
	// // localSession.getRootNode());
	// } catch (Exception e) {
	// throw new CmsException("Cannot first init from " + uri, e);
	// }
	// }

	/** Can be null */
	private ConfigurationAdmin findConfigurationAdmin() {
		// configurationAdmin =
		// bc.getServiceReference(ConfigurationAdmin.class);
		// if (configurationAdmin == null) {
		// return null;
		// }
		// return bc.getService(configurationAdmin);
		return configurationAdmin.getService();
	}

	/** Can be null */
	Bundle findBundle(String symbolicName) {
		for (Bundle b : bc.getBundles())
			if (b.getSymbolicName().equals(symbolicName))
				return b;
		return null;
	}

	private void initTransactionManager() {
		bitronix.tm.Configuration tmConf = TransactionManagerServices.getConfiguration();
		tmConf.setServerId(getFrameworkProp(FRAMEWORK_UUID));

		// File tmBaseDir = new File(getFrameworkProp(TRANSACTIONS_HOME,
		// getOsgiInstancePath(DIR_TRANSACTIONS)));
		Bundle bitronixBundle = FrameworkUtil.getBundle(bitronix.tm.Configuration.class);
		File tmBaseDir = bitronixBundle.getDataFile(DIR_TRANSACTIONS);
		// File tmBaseDir = bc.getDataFile(DIR_TRANSACTIONS);
		File tmDir1 = new File(tmBaseDir, "btm1");
		tmDir1.mkdirs();
		tmConf.setLogPart1Filename(new File(tmDir1, tmDir1.getName() + ".tlog").getAbsolutePath());
		File tmDir2 = new File(tmBaseDir, "btm2");
		tmDir2.mkdirs();
		tmConf.setLogPart2Filename(new File(tmDir2, tmDir2.getName() + ".tlog").getAbsolutePath());
		transactionManager = getTransactionManager();
		transactionSynchronizationRegistry = getTransactionSynchronizationRegistry();
	}

	private void initWebServer(final ConfigurationAdmin conf) {
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
					jettyProps.put(JettyConstants.SSL_KEYSTORE,
							nodeSecurity.getHttpServerKeyStore().getCanonicalPath());
					jettyProps.put(JettyConstants.SSL_PASSWORD, "changeit");
					jettyProps.put(JettyConstants.SSL_WANTCLIENTAUTH, true);
				}
				if (conf != null) {
					// TODO make filter more generic
					String filter = "(" + JettyConstants.HTTP_PORT + "=" + httpPort + ")";
					if (conf.listConfigurations(filter) != null)
						return;
					Configuration jettyConf = conf.createFactoryConfiguration(JETTY_FACTORY_PID, null);
					jettyConf.update(jettyProps);

				} else {
					JettyConfigurator.startServer("default", jettyProps);
				}
			}
		} catch (Exception e) {
			throw new CmsException("Cannot initialize web server on " + httpPortsMsg(httpPort, httpsPort), e);
		}
	}

	private void publish() {

		// Logging
		loggerReg = bc.registerService(ArgeoLogger.class, logger, null);
		// Transaction
		tmReg = bc.registerService(TransactionManager.class, transactionManager, null);
		utReg = bc.registerService(UserTransaction.class, transactionManager, null);
		tsrReg = bc.registerService(TransactionSynchronizationRegistry.class, transactionSynchronizationRegistry, null);
		// User admin
		// userAdminReg = bc.registerService(UserAdmin.class, userAdmin,
		// userAdmin.currentState());
		// JCR
		// Hashtable<String, String> regProps = new Hashtable<String, String>();
		// regProps.put(ArgeoJcrConstants.JCR_REPOSITORY_ALIAS,
		// ArgeoJcrConstants.ALIAS_NODE);
		// repositoryReg = (ServiceRegistration<? extends Repository>)
		// bc.registerService(Repository.class, repository,
		// regProps);
		// repositoryFactoryReg = bc.registerService(RepositoryFactory.class,
		// repositoryFactory, null);
	}

	void destroy() {
		long begin = System.currentTimeMillis();
		unpublish();

		kernelThread.destroyAndJoin();

		if (dataHttp != null)
			dataHttp.destroy();
		if (nodeHttp != null)
			nodeHttp.destroy();
		if (userAdmin != null)
			userAdmin.destroy();
		// if (repository != null)
		// repository.shutdown();
		if (transactionManager != null)
			transactionManager.shutdown();

		// bc.removeServiceListener(this);

		// Clean hanging threads from Jackrabbit
		TransientFileFactory.shutdown();

		// Clean hanging Gogo shell thread
		new GogoShellKiller().start();

		nodeSecurity.destroy();
		long duration = System.currentTimeMillis() - begin;
		log.info("## ARGEO CMS DOWN in " + (duration / 1000) + "." + (duration % 1000) + "s ##");
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

	// @Override
	// public void serviceChanged(ServiceEvent event) {
	// ServiceReference<?> sr = event.getServiceReference();
	// Object service = bc.getService(sr);
	// if (service instanceof Repository) {
	// Object jcrRepoAlias =
	// sr.getProperty(ArgeoJcrConstants.JCR_REPOSITORY_ALIAS);
	// if (jcrRepoAlias != null) {// JCR repository
	// String alias = jcrRepoAlias.toString();
	// Repository repository = (Repository) bc.getService(sr);
	// Map<String, Object> props = new HashMap<String, Object>();
	// for (String key : sr.getPropertyKeys())
	// props.put(key, sr.getProperty(key));
	// if (ServiceEvent.REGISTERED == event.getType()) {
	// try {
	// // repositoryFactory.register(repository, props);
	// dataHttp.registerRepositoryServlets(alias, repository);
	// } catch (Exception e) {
	// throw new CmsException("Could not publish JCR repository " + alias, e);
	// }
	// } else if (ServiceEvent.UNREGISTERING == event.getType()) {
	// // repositoryFactory.unregister(repository, props);
	// dataHttp.unregisterRepositoryServlets(alias);
	// }
	// }
	// }
	// // else if (service instanceof ExtendedHttpService) {
	// // if (ServiceEvent.REGISTERED == event.getType()) {
	// // addHttpService(sr);
	// // } else if (ServiceEvent.UNREGISTERING == event.getType()) {
	// // dataHttp.destroy();
	// // dataHttp = null;
	// // }
	// // }
	// }

	private HttpService addHttpService(ServiceReference<HttpService> sr) {
		// for (String key : sr.getPropertyKeys())
		// log.debug(key + "=" + sr.getProperty(key));
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

	private String httpPortsMsg(Object httpPort, Object httpsPort) {
		return "HTTP " + httpPort + (httpsPort != null ? " - HTTPS " + httpsPort : "");
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
		log.info("Spend " + ms + "ms" + " reflecting on the progress brought to mankind" + " by Free Software...");
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
			log.debug("Sleep accuracy: " + String.format("%.2f", 100 - (sleepAccuracy * 100 - 100)) + " %");
	}

	private class PrepareStc<T> implements ServiceTrackerCustomizer<T, T> {

		@Override
		public T addingService(ServiceReference<T> reference) {
			T service = bc.getService(reference);
			System.out.println("addingService " + service);
			return service;
		}

		@Override
		public void modifiedService(ServiceReference<T> reference, T service) {
			System.out.println("modifiedService " + service);
		}

		@Override
		public void removedService(ServiceReference<T> reference, T service) {
			System.out.println("removedService " + service);
		}

	}

	private class PrepareHttpStc implements ServiceTrackerCustomizer<HttpService, HttpService> {

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