package org.argeo.cms.internal.kernel;

import static org.argeo.api.DataModelNamespace.CMS_DATA_MODEL_NAMESPACE;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.callback.CallbackHandler;
import javax.servlet.Servlet;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.core.RepositoryContext;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.argeo.api.DataModelNamespace;
import org.argeo.api.NodeConstants;
import org.argeo.api.NodeDeployment;
import org.argeo.api.NodeState;
import org.argeo.api.security.CryptoKeyring;
import org.argeo.api.security.Keyring;
import org.argeo.cms.ArgeoNames;
import org.argeo.cms.CmsException;
import org.argeo.cms.internal.http.CmsRemotingServlet;
import org.argeo.cms.internal.http.CmsWebDavServlet;
import org.argeo.cms.internal.http.HttpUtils;
import org.argeo.jcr.JcrUtils;
import org.argeo.osgi.useradmin.UserAdminConf;
import org.argeo.util.LangUtils;
import org.eclipse.equinox.http.jetty.JettyConfigurator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.util.tracker.ServiceTracker;

/** Implementation of a CMS deployment. */
public class CmsDeployment implements NodeDeployment {
	private final Log log = LogFactory.getLog(getClass());
	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

	private DataModels dataModels;
	private DeployConfig deployConfig;

	private Long availableSince;

//	private final boolean cleanState;

//	private NodeHttp nodeHttp;
	private String webDavConfig = HttpUtils.WEBDAV_CONFIG;

	private boolean argeoDataModelExtensionsAvailable = false;

	// Readiness
	private boolean nodeAvailable = false;
	private boolean userAdminAvailable = false;
	private boolean httpExpected = false;
	private boolean httpAvailable = false;

	public CmsDeployment() {
//		ServiceReference<NodeState> nodeStateSr = bc.getServiceReference(NodeState.class);
//		if (nodeStateSr == null)
//			throw new CmsException("No node state available");

//		NodeState nodeState = bc.getService(nodeStateSr);
//		cleanState = nodeState.isClean();

//		nodeHttp = new NodeHttp();
		dataModels = new DataModels(bc);
		initTrackers();
	}

	private void initTrackers() {
		ServiceTracker<?, ?> httpSt = new ServiceTracker<HttpService, HttpService>(bc, HttpService.class, null) {

			@Override
			public HttpService addingService(ServiceReference<HttpService> sr) {
				httpAvailable = true;
				Object httpPort = sr.getProperty("http.port");
				Object httpsPort = sr.getProperty("https.port");
				log.info(httpPortsMsg(httpPort, httpsPort));
				checkReadiness();
				return super.addingService(sr);
			}
		};
		// httpSt.open();
		KernelUtils.asyncOpen(httpSt);

		ServiceTracker<?, ?> repoContextSt = new RepositoryContextStc();
		// repoContextSt.open();
		KernelUtils.asyncOpen(repoContextSt);

		ServiceTracker<?, ?> userAdminSt = new ServiceTracker<UserAdmin, UserAdmin>(bc, UserAdmin.class, null) {
			@Override
			public UserAdmin addingService(ServiceReference<UserAdmin> reference) {
				UserAdmin userAdmin = super.addingService(reference);
				addStandardSystemRoles(userAdmin);
				userAdminAvailable = true;
				checkReadiness();
				return userAdmin;
			}
		};
		// userAdminSt.open();
		KernelUtils.asyncOpen(userAdminSt);

		ServiceTracker<?, ?> confAdminSt = new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(bc,
				ConfigurationAdmin.class, null) {
			@Override
			public ConfigurationAdmin addingService(ServiceReference<ConfigurationAdmin> reference) {
				ConfigurationAdmin configurationAdmin = bc.getService(reference);
				boolean isClean;
				try {
					Configuration[] confs = configurationAdmin
							.listConfigurations("(service.factoryPid=" + NodeConstants.NODE_USER_ADMIN_PID + ")");
					isClean = confs == null || confs.length == 0;
				} catch (Exception e) {
					throw new CmsException("Cannot analize clean state", e);
				}
				deployConfig = new DeployConfig(configurationAdmin, dataModels, isClean);
				httpExpected = deployConfig.getProps(KernelConstants.JETTY_FACTORY_PID, "default") != null;
				try {
					Configuration[] configs = configurationAdmin
							.listConfigurations("(service.factoryPid=" + NodeConstants.NODE_USER_ADMIN_PID + ")");

					boolean hasDomain = false;
					for (Configuration config : configs) {
						Object realm = config.getProperties().get(UserAdminConf.realm.name());
						if (realm != null) {
							log.debug("Found realm: " + realm);
							hasDomain = true;
						}
					}
					if (hasDomain) {
						loadIpaJaasConfiguration();
					}
				} catch (Exception e) {
					throw new CmsException("Cannot initialize config", e);
				}
				return super.addingService(reference);
			}
		};
		// confAdminSt.open();
		KernelUtils.asyncOpen(confAdminSt);
	}

	private String httpPortsMsg(Object httpPort, Object httpsPort) {
		return (httpPort != null ? "HTTP " + httpPort + " " : " ") + (httpsPort != null ? "HTTPS " + httpsPort : "");
	}

	private void addStandardSystemRoles(UserAdmin userAdmin) {
		// we assume UserTransaction is already available (TODO make it more robust)
		UserTransaction userTransaction = bc.getService(bc.getServiceReference(UserTransaction.class));
		try {
			userTransaction.begin();
			Role adminRole = userAdmin.getRole(NodeConstants.ROLE_ADMIN);
			if (adminRole == null) {
				adminRole = userAdmin.createRole(NodeConstants.ROLE_ADMIN, Role.GROUP);
			}
			if (userAdmin.getRole(NodeConstants.ROLE_USER_ADMIN) == null) {
				Group userAdminRole = (Group) userAdmin.createRole(NodeConstants.ROLE_USER_ADMIN, Role.GROUP);
				userAdminRole.addMember(adminRole);
			}
			userTransaction.commit();
		} catch (Exception e) {
			try {
				userTransaction.rollback();
			} catch (Exception e1) {
				// silent
			}
			throw new CmsException("Cannot add standard system roles", e);
		}
	}

	private void loadIpaJaasConfiguration() {
		if (System.getProperty(KernelConstants.JAAS_CONFIG_PROP) == null) {
			String jaasConfig = KernelConstants.JAAS_CONFIG_IPA;
			URL url = getClass().getClassLoader().getResource(jaasConfig);
			KernelUtils.setJaasConfiguration(url);
			log.debug("Set IPA JAAS configuration.");
		}
	}

	public void shutdown() {
//		if (nodeHttp != null)
//			nodeHttp.destroy();

		try {
			for (ServiceReference<JackrabbitLocalRepository> sr : bc
					.getServiceReferences(JackrabbitLocalRepository.class, null)) {
				bc.getService(sr).destroy();
			}
		} catch (InvalidSyntaxException e1) {
			log.error("Cannot sclean repsoitories", e1);
		}

		try {
			JettyConfigurator.stopServer(KernelConstants.DEFAULT_JETTY_SERVER);
		} catch (Exception e) {
			log.error("Cannot stop default Jetty server.", e);
		}

		if (deployConfig != null) {
			new Thread(() -> deployConfig.save(), "Save Argeo Deploy Config").start();
		}
	}

	/**
	 * Checks whether the deployment is available according to expectations, and
	 * mark it as available.
	 */
	private synchronized void checkReadiness() {
		if (isAvailable())
			return;
		if (nodeAvailable && userAdminAvailable && (httpExpected ? httpAvailable : true)) {
			String data = KernelUtils.getFrameworkProp(KernelUtils.OSGI_INSTANCE_AREA);
			String state = KernelUtils.getFrameworkProp(KernelUtils.OSGI_CONFIGURATION_AREA);
			availableSince = System.currentTimeMillis();
			long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
			String jvmUptimeStr = " in " + (jvmUptime / 1000) + "." + (jvmUptime % 1000) + "s";
			log.info("## ARGEO NODE AVAILABLE" + (log.isDebugEnabled() ? jvmUptimeStr : "") + " ##");
			if (log.isDebugEnabled()) {
				log.debug("## state: " + state);
				if (data != null)
					log.debug("## data: " + data);
			}
			long begin = bc.getService(bc.getServiceReference(NodeState.class)).getAvailableSince();
			long initDuration = System.currentTimeMillis() - begin;
			if (log.isTraceEnabled())
				log.trace("Kernel initialization took " + initDuration + "ms");
			tributeToFreeSoftware(initDuration);
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

	private void prepareNodeRepository(Repository deployedNodeRepository) {
		if (availableSince != null) {
			throw new CmsException("Deployment is already available");
		}

		// home
		prepareDataModel(NodeConstants.NODE_REPOSITORY, deployedNodeRepository);
	}

	private void prepareHomeRepository(RepositoryImpl deployedRepository) {
		Session adminSession = KernelUtils.openAdminSession(deployedRepository);
		try {
			argeoDataModelExtensionsAvailable = Arrays
					.asList(adminSession.getWorkspace().getNamespaceRegistry().getURIs())
					.contains(ArgeoNames.ARGEO_NAMESPACE);
		} catch (RepositoryException e) {
			log.warn("Cannot check whether Argeo namespace is registered assuming it isn't.", e);
			argeoDataModelExtensionsAvailable = false;
		} finally {
			JcrUtils.logoutQuietly(adminSession);
		}

		// Publish home with the highest service ranking
		Hashtable<String, Object> regProps = new Hashtable<>();
		regProps.put(NodeConstants.CN, NodeConstants.EGO_REPOSITORY);
		regProps.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
		Repository egoRepository = new EgoRepository(deployedRepository, false);
		bc.registerService(Repository.class, egoRepository, regProps);
		registerRepositoryServlets(NodeConstants.EGO_REPOSITORY, egoRepository);

		// Keyring only if Argeo extensions are available
		if (argeoDataModelExtensionsAvailable) {
			new ServiceTracker<CallbackHandler, CallbackHandler>(bc, CallbackHandler.class, null) {

				@Override
				public CallbackHandler addingService(ServiceReference<CallbackHandler> reference) {
					NodeKeyRing nodeKeyring = new NodeKeyRing(egoRepository);
					CallbackHandler callbackHandler = bc.getService(reference);
					nodeKeyring.setDefaultCallbackHandler(callbackHandler);
					bc.registerService(LangUtils.names(Keyring.class, CryptoKeyring.class, ManagedService.class),
							nodeKeyring, LangUtils.dico(Constants.SERVICE_PID, NodeConstants.NODE_KEYRING_PID));
					return callbackHandler;
				}

			}.open();
		}
	}

	/** Session is logged out. */
	private void prepareDataModel(String cn, Repository repository) {
		Session adminSession = KernelUtils.openAdminSession(repository);
		try {
			Set<String> processed = new HashSet<String>();
			bundles: for (Bundle bundle : bc.getBundles()) {
				BundleWiring wiring = bundle.adapt(BundleWiring.class);
				if (wiring == null)
					continue bundles;
				if (NodeConstants.NODE_REPOSITORY.equals(cn))// process all data models
					processWiring(cn, adminSession, wiring, processed, false);
				else {
					List<BundleCapability> capabilities = wiring.getCapabilities(CMS_DATA_MODEL_NAMESPACE);
					for (BundleCapability capability : capabilities) {
						String dataModelName = (String) capability.getAttributes().get(DataModelNamespace.NAME);
						if (dataModelName.equals(cn))// process only own data model
							processWiring(cn, adminSession, wiring, processed, false);
					}
				}
			}
		} finally {
			JcrUtils.logoutQuietly(adminSession);
		}
	}

	private void processWiring(String cn, Session adminSession, BundleWiring wiring, Set<String> processed,
			boolean importListedAbstractModels) {
		// recursively process requirements first
		List<BundleWire> requiredWires = wiring.getRequiredWires(CMS_DATA_MODEL_NAMESPACE);
		for (BundleWire wire : requiredWires) {
			processWiring(cn, adminSession, wire.getProviderWiring(), processed, true);
		}

		List<String> publishAsLocalRepo = new ArrayList<>();
		List<BundleCapability> capabilities = wiring.getCapabilities(CMS_DATA_MODEL_NAMESPACE);
		capabilities: for (BundleCapability capability : capabilities) {
			if (!importListedAbstractModels
					&& KernelUtils.asBoolean((String) capability.getAttributes().get(DataModelNamespace.ABSTRACT))) {
				continue capabilities;
			}
			boolean publish = registerDataModelCapability(cn, adminSession, capability, processed);
			if (publish)
				publishAsLocalRepo.add((String) capability.getAttributes().get(DataModelNamespace.NAME));
		}
		// Publish all at once, so that bundles with multiple CNDs are consistent
		for (String dataModelName : publishAsLocalRepo)
			publishLocalRepo(dataModelName, adminSession.getRepository());
	}

	private boolean registerDataModelCapability(String cn, Session adminSession, BundleCapability capability,
			Set<String> processed) {
		Map<String, Object> attrs = capability.getAttributes();
		String name = (String) attrs.get(DataModelNamespace.NAME);
		if (processed.contains(name)) {
			if (log.isTraceEnabled())
				log.trace("Data model " + name + " has already been processed");
			return false;
		}

		// CND
		String path = (String) attrs.get(DataModelNamespace.CND);
		if (path != null) {
			File dataModel = bc.getBundle().getDataFile("dataModels/" + path);
			if (!dataModel.exists()) {
				URL url = capability.getRevision().getBundle().getResource(path);
				if (url == null)
					throw new CmsException("No data model '" + name + "' found under path " + path);
				try (Reader reader = new InputStreamReader(url.openStream())) {
					CndImporter.registerNodeTypes(reader, adminSession, true);
					processed.add(name);
					dataModel.getParentFile().mkdirs();
					dataModel.createNewFile();
					if (log.isDebugEnabled())
						log.debug("Registered CND " + url);
				} catch (Exception e) {
					throw new CmsException("Cannot import CND " + url, e);
				}
			}
		}

		if (KernelUtils.asBoolean((String) attrs.get(DataModelNamespace.ABSTRACT)))
			return false;
		// Non abstract
		boolean isStandalone = deployConfig.isStandalone(name);
		boolean publishLocalRepo;
		if (isStandalone && name.equals(cn))// includes the node itself
			publishLocalRepo = true;
		else if (!isStandalone && cn.equals(NodeConstants.NODE_REPOSITORY))
			publishLocalRepo = true;
		else
			publishLocalRepo = false;

		return publishLocalRepo;
	}

	private void publishLocalRepo(String dataModelName, Repository repository) {
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(NodeConstants.CN, dataModelName);
		LocalRepository localRepository;
		String[] classes;
		if (repository instanceof RepositoryImpl) {
			localRepository = new JackrabbitLocalRepository((RepositoryImpl) repository, dataModelName);
			classes = new String[] { Repository.class.getName(), LocalRepository.class.getName(),
					JackrabbitLocalRepository.class.getName() };
		} else {
			localRepository = new LocalRepository(repository, dataModelName);
			classes = new String[] { Repository.class.getName(), LocalRepository.class.getName() };
		}
		bc.registerService(classes, localRepository, properties);

		// TODO make it configurable
		registerRepositoryServlets(dataModelName, localRepository);
		if (log.isTraceEnabled())
			log.trace("Published data model " + dataModelName);
	}

	@Override
	public synchronized Long getAvailableSince() {
		return availableSince;
	}

	public synchronized boolean isAvailable() {
		return availableSince != null;
	}

	protected void registerRepositoryServlets(String alias, Repository repository) {
		registerRemotingServlet(alias, repository);
		registerWebdavServlet(alias, repository);
	}

	protected void registerWebdavServlet(String alias, Repository repository) {
		CmsWebDavServlet webdavServlet = new CmsWebDavServlet(alias, repository);
		Hashtable<String, String> ip = new Hashtable<>();
		ip.put(HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX + CmsWebDavServlet.INIT_PARAM_RESOURCE_CONFIG, webDavConfig);
		ip.put(HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX + CmsWebDavServlet.INIT_PARAM_RESOURCE_PATH_PREFIX,
				"/" + alias);

		ip.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/" + alias + "/*");
		ip.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH + "=" + NodeConstants.PATH_DATA + ")");
		bc.registerService(Servlet.class, webdavServlet, ip);
	}

	protected void registerRemotingServlet(String alias, Repository repository) {
		CmsRemotingServlet remotingServlet = new CmsRemotingServlet(alias, repository);
		Hashtable<String, String> ip = new Hashtable<>();
		ip.put(NodeConstants.CN, alias);
		// Properties ip = new Properties();
		ip.put(HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX + CmsRemotingServlet.INIT_PARAM_RESOURCE_PATH_PREFIX,
				"/" + alias);
		ip.put(HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX + CmsRemotingServlet.INIT_PARAM_AUTHENTICATE_HEADER,
				"Negotiate");

		// Looks like a bug in Jackrabbit remoting init
		Path tmpDir;
		try {
			tmpDir = Files.createTempDirectory("remoting_" + alias);
		} catch (IOException e) {
			throw new CmsException("Cannot create temp directory for remoting servlet", e);
		}
		ip.put(HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX + CmsRemotingServlet.INIT_PARAM_HOME, tmpDir.toString());
		ip.put(HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX + CmsRemotingServlet.INIT_PARAM_TMP_DIRECTORY,
				"remoting_" + alias);
		ip.put(HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX + CmsRemotingServlet.INIT_PARAM_PROTECTED_HANDLERS_CONFIG,
				HttpUtils.DEFAULT_PROTECTED_HANDLERS);
		ip.put(HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX + CmsRemotingServlet.INIT_PARAM_CREATE_ABSOLUTE_URI, "false");

		ip.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/" + alias + "/*");
		ip.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH + "=" + NodeConstants.PATH_JCR + ")");
		bc.registerService(Servlet.class, remotingServlet, ip);
	}

	private class RepositoryContextStc extends ServiceTracker<RepositoryContext, RepositoryContext> {

		public RepositoryContextStc() {
			super(bc, RepositoryContext.class, null);
		}

		@Override
		public RepositoryContext addingService(ServiceReference<RepositoryContext> reference) {
			RepositoryContext repoContext = bc.getService(reference);
			String cn = (String) reference.getProperty(NodeConstants.CN);
			if (cn != null) {
				if (cn.equals(NodeConstants.NODE_REPOSITORY)) {
					prepareNodeRepository(repoContext.getRepository());
					// TODO separate home repository
					prepareHomeRepository(repoContext.getRepository());
					registerRepositoryServlets(cn, repoContext.getRepository());
					nodeAvailable = true;
					checkReadiness();
				} else {
					prepareDataModel(cn, repoContext.getRepository());
				}
			}
			return repoContext;
		}

		@Override
		public void modifiedService(ServiceReference<RepositoryContext> reference, RepositoryContext service) {
		}

		@Override
		public void removedService(ServiceReference<RepositoryContext> reference, RepositoryContext service) {
		}

	}

}
