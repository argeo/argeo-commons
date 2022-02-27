package org.argeo.cms.jcr.internal;

import static org.argeo.cms.osgi.DataModelNamespace.CMS_DATA_MODEL_NAMESPACE;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.callback.CallbackHandler;
import javax.servlet.Servlet;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.core.RepositoryContext;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsDeployment;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.ArgeoNames;
import org.argeo.cms.internal.jcr.JcrInitUtils;
import org.argeo.cms.jcr.CmsJcrUtils;
import org.argeo.cms.jcr.internal.servlet.CmsRemotingServlet;
import org.argeo.cms.jcr.internal.servlet.CmsWebDavServlet;
import org.argeo.cms.jcr.internal.servlet.JcrHttpUtils;
import org.argeo.cms.osgi.DataModelNamespace;
import org.argeo.cms.security.CryptoKeyring;
import org.argeo.cms.security.Keyring;
import org.argeo.jcr.Jcr;
import org.argeo.jcr.JcrException;
import org.argeo.jcr.JcrUtils;
import org.argeo.util.LangUtils;
import org.argeo.util.naming.LdapAttrs;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;

/** Implementation of a CMS deployment. */
public class CmsJcrDeployment {
	private final CmsLog log = CmsLog.getLog(getClass());
	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

	private DataModels dataModels;
	private String webDavConfig = JcrHttpUtils.WEBDAV_CONFIG;

	private boolean argeoDataModelExtensionsAvailable = false;

	// Readiness
	private boolean nodeAvailable = false;

	CmsDeployment cmsDeployment;

	public void start() {
		dataModels = new DataModels(bc);

		ServiceTracker<?, ?> repoContextSt = new RepositoryContextStc();
		repoContextSt.open();
		//KernelUtils.asyncOpen(repoContextSt);

//		nodeDeployment = CmsJcrActivator.getService(NodeDeployment.class);

		JcrInitUtils.addToDeployment(cmsDeployment);

	}

	public void stop() {
//		if (nodeHttp != null)
//			nodeHttp.destroy();

		try {
			for (ServiceReference<JackrabbitLocalRepository> sr : bc
					.getServiceReferences(JackrabbitLocalRepository.class, null)) {
				bc.getService(sr).destroy();
			}
		} catch (InvalidSyntaxException e1) {
			log.error("Cannot clean repositories", e1);
		}

	}

	public void setCmsDeployment(CmsDeployment cmsDeployment) {
		this.cmsDeployment = cmsDeployment;
	}

	/**
	 * Checks whether the deployment is available according to expectations, and
	 * mark it as available.
	 */
//	private synchronized void checkReadiness() {
//		if (isAvailable())
//			return;
//		if (nodeAvailable && userAdminAvailable && (httpExpected ? httpAvailable : true)) {
//			String data = KernelUtils.getFrameworkProp(KernelUtils.OSGI_INSTANCE_AREA);
//			String state = KernelUtils.getFrameworkProp(KernelUtils.OSGI_CONFIGURATION_AREA);
//			availableSince = System.currentTimeMillis();
//			long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
//			String jvmUptimeStr = " in " + (jvmUptime / 1000) + "." + (jvmUptime % 1000) + "s";
//			log.info("## ARGEO NODE AVAILABLE" + (log.isDebugEnabled() ? jvmUptimeStr : "") + " ##");
//			if (log.isDebugEnabled()) {
//				log.debug("## state: " + state);
//				if (data != null)
//					log.debug("## data: " + data);
//			}
//			long begin = bc.getService(bc.getServiceReference(NodeState.class)).getAvailableSince();
//			long initDuration = System.currentTimeMillis() - begin;
//			if (log.isTraceEnabled())
//				log.trace("Kernel initialization took " + initDuration + "ms");
//			tributeToFreeSoftware(initDuration);
//		}
//	}

	private void prepareNodeRepository(Repository deployedNodeRepository, List<String> publishAsLocalRepo) {
//		if (availableSince != null) {
//			throw new IllegalStateException("Deployment is already available");
//		}

		// home
		prepareDataModel(CmsConstants.NODE_REPOSITORY, deployedNodeRepository, publishAsLocalRepo);

		// init from backup
//		if (deployConfig.isFirstInit()) {
//			Path restorePath = Paths.get(System.getProperty("user.dir"), "restore");
//			if (Files.exists(restorePath)) {
//				if (log.isDebugEnabled())
//					log.debug("Found backup " + restorePath + ", restoring it...");
//				LogicalRestore logicalRestore = new LogicalRestore(bc, deployedNodeRepository, restorePath);
//				KernelUtils.doAsDataAdmin(logicalRestore);
//				log.info("Restored backup from " + restorePath);
//			}
//		}

		// init from repository
		Collection<ServiceReference<Repository>> initRepositorySr;
		try {
			initRepositorySr = bc.getServiceReferences(Repository.class,
					"(" + CmsConstants.CN + "=" + CmsConstants.NODE_INIT + ")");
		} catch (InvalidSyntaxException e1) {
			throw new IllegalArgumentException(e1);
		}
		Iterator<ServiceReference<Repository>> it = initRepositorySr.iterator();
		while (it.hasNext()) {
			ServiceReference<Repository> sr = it.next();
			Object labeledUri = sr.getProperties().get(LdapAttrs.labeledURI.name());
			Repository initRepository = bc.getService(sr);
			if (log.isDebugEnabled())
				log.debug("Found init repository " + labeledUri + ", copying it...");
			initFromRepository(deployedNodeRepository, initRepository);
			log.info("Node repository initialised from " + labeledUri);
		}
	}

	/** Init from a (typically remote) repository. */
	private void initFromRepository(Repository deployedNodeRepository, Repository initRepository) {
		Session initSession = null;
		try {
			initSession = initRepository.login();
			workspaces: for (String workspaceName : initSession.getWorkspace().getAccessibleWorkspaceNames()) {
				if ("security".equals(workspaceName))
					continue workspaces;
				if (log.isDebugEnabled())
					log.debug("Copying workspace " + workspaceName + " from init repository...");
				long begin = System.currentTimeMillis();
				Session targetSession = null;
				Session sourceSession = null;
				try {
					try {
						targetSession = CmsJcrUtils.openDataAdminSession(deployedNodeRepository, workspaceName);
					} catch (IllegalArgumentException e) {// no such workspace
						Session adminSession = CmsJcrUtils.openDataAdminSession(deployedNodeRepository, null);
						try {
							adminSession.getWorkspace().createWorkspace(workspaceName);
						} finally {
							Jcr.logout(adminSession);
						}
						targetSession = CmsJcrUtils.openDataAdminSession(deployedNodeRepository, workspaceName);
					}
					sourceSession = initRepository.login(workspaceName);
//					JcrUtils.copyWorkspaceXml(sourceSession, targetSession);
					// TODO deal with referenceable nodes
					JcrUtils.copy(sourceSession.getRootNode(), targetSession.getRootNode());
					targetSession.save();
					long duration = System.currentTimeMillis() - begin;
					if (log.isDebugEnabled())
						log.debug("Copied workspace " + workspaceName + " from init repository in " + (duration / 1000)
								+ " s");
				} catch (Exception e) {
					log.error("Cannot copy workspace " + workspaceName + " from init repository.", e);
				} finally {
					Jcr.logout(sourceSession);
					Jcr.logout(targetSession);
				}
			}
		} catch (RepositoryException e) {
			throw new JcrException(e);
		} finally {
			Jcr.logout(initSession);
		}
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
		regProps.put(CmsConstants.CN, CmsConstants.EGO_REPOSITORY);
		regProps.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
		Repository egoRepository = new EgoRepository(deployedRepository, false);
		bc.registerService(Repository.class, egoRepository, regProps);
		registerRepositoryServlets(CmsConstants.EGO_REPOSITORY, egoRepository);

		// Keyring only if Argeo extensions are available
		if (argeoDataModelExtensionsAvailable) {
			new ServiceTracker<CallbackHandler, CallbackHandler>(bc, CallbackHandler.class, null) {

				@Override
				public CallbackHandler addingService(ServiceReference<CallbackHandler> reference) {
					NodeKeyRing nodeKeyring = new NodeKeyRing(egoRepository);
					CallbackHandler callbackHandler = bc.getService(reference);
					nodeKeyring.setDefaultCallbackHandler(callbackHandler);
					bc.registerService(LangUtils.names(Keyring.class, CryptoKeyring.class, ManagedService.class),
							nodeKeyring, LangUtils.dict(Constants.SERVICE_PID, CmsConstants.NODE_KEYRING_PID));
					return callbackHandler;
				}

			}.open();
		}
	}

	/** Session is logged out. */
	private void prepareDataModel(String cn, Repository repository, List<String> publishAsLocalRepo) {
		Session adminSession = KernelUtils.openAdminSession(repository);
		try {
			Set<String> processed = new HashSet<String>();
			bundles: for (Bundle bundle : bc.getBundles()) {
				BundleWiring wiring = bundle.adapt(BundleWiring.class);
				if (wiring == null)
					continue bundles;
				if (CmsConstants.NODE_REPOSITORY.equals(cn))// process all data models
					processWiring(cn, adminSession, wiring, processed, false, publishAsLocalRepo);
				else {
					List<BundleCapability> capabilities = wiring.getCapabilities(CMS_DATA_MODEL_NAMESPACE);
					for (BundleCapability capability : capabilities) {
						String dataModelName = (String) capability.getAttributes().get(DataModelNamespace.NAME);
						if (dataModelName.equals(cn))// process only own data model
							processWiring(cn, adminSession, wiring, processed, false, publishAsLocalRepo);
					}
				}
			}
		} finally {
			JcrUtils.logoutQuietly(adminSession);
		}
	}

	private void processWiring(String cn, Session adminSession, BundleWiring wiring, Set<String> processed,
			boolean importListedAbstractModels, List<String> publishAsLocalRepo) {
		// recursively process requirements first
		List<BundleWire> requiredWires = wiring.getRequiredWires(CMS_DATA_MODEL_NAMESPACE);
		for (BundleWire wire : requiredWires) {
			processWiring(cn, adminSession, wire.getProviderWiring(), processed, true, publishAsLocalRepo);
		}

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
					throw new IllegalArgumentException("No data model '" + name + "' found under path " + path);
				try (Reader reader = new InputStreamReader(url.openStream())) {
					CndImporter.registerNodeTypes(reader, adminSession, true);
					processed.add(name);
					dataModel.getParentFile().mkdirs();
					dataModel.createNewFile();
					if (log.isDebugEnabled())
						log.debug("Registered CND " + url);
				} catch (Exception e) {
					log.error("Cannot import CND " + url, e);
				}
			}
		}

		if (KernelUtils.asBoolean((String) attrs.get(DataModelNamespace.ABSTRACT)))
			return false;
		// Non abstract
		boolean isStandalone = isStandalone(name);
		boolean publishLocalRepo;
		if (isStandalone && name.equals(cn))// includes the node itself
			publishLocalRepo = true;
		else if (!isStandalone && cn.equals(CmsConstants.NODE_REPOSITORY))
			publishLocalRepo = true;
		else
			publishLocalRepo = false;

		return publishLocalRepo;
	}

	boolean isStandalone(String dataModelName) {
		return cmsDeployment.getProps(CmsConstants.NODE_REPOS_FACTORY_PID, dataModelName) != null;
	}

	private void publishLocalRepo(String dataModelName, Repository repository) {
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(CmsConstants.CN, dataModelName);
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

//	@Override
//	public synchronized Long getAvailableSince() {
//		return availableSince;
//	}
//
//	public synchronized boolean isAvailable() {
//		return availableSince != null;
//	}

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
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH + "=" + CmsConstants.PATH_DATA + ")");
		bc.registerService(Servlet.class, webdavServlet, ip);
	}

	protected void registerRemotingServlet(String alias, Repository repository) {
		CmsRemotingServlet remotingServlet = new CmsRemotingServlet(alias, repository);
		Hashtable<String, String> ip = new Hashtable<>();
		ip.put(CmsConstants.CN, alias);
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
			throw new RuntimeException("Cannot create temp directory for remoting servlet", e);
		}
		ip.put(HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX + CmsRemotingServlet.INIT_PARAM_HOME, tmpDir.toString());
		ip.put(HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX + CmsRemotingServlet.INIT_PARAM_TMP_DIRECTORY,
				"remoting_" + alias);
		ip.put(HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX + CmsRemotingServlet.INIT_PARAM_PROTECTED_HANDLERS_CONFIG,
				JcrHttpUtils.DEFAULT_PROTECTED_HANDLERS);
		ip.put(HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX + CmsRemotingServlet.INIT_PARAM_CREATE_ABSOLUTE_URI, "false");

		ip.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/" + alias + "/*");
		ip.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH + "=" + CmsConstants.PATH_JCR + ")");
		bc.registerService(Servlet.class, remotingServlet, ip);
	}

	private class RepositoryContextStc extends ServiceTracker<RepositoryContext, RepositoryContext> {

		public RepositoryContextStc() {
			super(bc, RepositoryContext.class, null);
		}

		@Override
		public RepositoryContext addingService(ServiceReference<RepositoryContext> reference) {
			RepositoryContext repoContext = bc.getService(reference);
			String cn = (String) reference.getProperty(CmsConstants.CN);
			if (cn != null) {
				List<String> publishAsLocalRepo = new ArrayList<>();
				if (cn.equals(CmsConstants.NODE_REPOSITORY)) {
//					JackrabbitDataModelMigration.clearRepositoryCaches(repoContext.getRepositoryConfig());
					prepareNodeRepository(repoContext.getRepository(), publishAsLocalRepo);
					// TODO separate home repository
					prepareHomeRepository(repoContext.getRepository());
					registerRepositoryServlets(cn, repoContext.getRepository());
					nodeAvailable = true;
//					checkReadiness();
				} else {
					prepareDataModel(cn, repoContext.getRepository(), publishAsLocalRepo);
				}
				// Publish all at once, so that bundles with multiple CNDs are consistent
				for (String dataModelName : publishAsLocalRepo)
					publishLocalRepo(dataModelName, repoContext.getRepository());
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
