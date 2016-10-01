package org.argeo.cms.internal.kernel;

import static org.argeo.node.DataModelNamespace.CMS_DATA_MODEL_NAMESPACE;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.security.auth.callback.CallbackHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.core.RepositoryContext;
import org.argeo.cms.CmsException;
import org.argeo.jcr.JcrUtils;
import org.argeo.node.DataModelNamespace;
import org.argeo.node.NodeConstants;
import org.argeo.node.NodeDeployment;
import org.argeo.node.NodeState;
import org.argeo.node.security.CryptoKeyring;
import org.argeo.util.LangUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpService;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class CmsDeployment implements NodeDeployment {
	private final static String LEGACY_JCR_REPOSITORY_ALIAS = "argeo.jcr.repository.alias";
	
	private final Log log = LogFactory.getLog(getClass());
	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

	private DeployConfig deployConfig;
	private HomeRepository homeRepository;

	private Long availableSince;

	private final boolean cleanState;
	// Readiness
	private boolean nodeAvailable = false;
	private boolean userAdminAvailable = false;
	private boolean httpExpected = false;
	private boolean httpAvailable = false;

	public CmsDeployment() {
		ServiceReference<NodeState> nodeStateSr = bc.getServiceReference(NodeState.class);
		if (nodeStateSr == null)
			throw new CmsException("No node state available");

		NodeState nodeState = bc.getService(nodeStateSr);
		cleanState = nodeState.isClean();

		initTrackers();
	}

	private void initTrackers() {
		new PrepareHttpStc().open();
		new RepositoryContextStc().open();
		new ServiceTracker<UserAdmin, UserAdmin>(bc, UserAdmin.class, null) {
			@Override
			public UserAdmin addingService(ServiceReference<UserAdmin> reference) {
				userAdminAvailable = true;
				checkReadiness();
				return super.addingService(reference);
			}
		}.open();
		new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(bc, ConfigurationAdmin.class, null) {
			@Override
			public ConfigurationAdmin addingService(ServiceReference<ConfigurationAdmin> reference) {
				ConfigurationAdmin configurationAdmin = bc.getService(reference);
				deployConfig = new DeployConfig(configurationAdmin, cleanState);
				httpExpected = deployConfig.getProps(KernelConstants.JETTY_FACTORY_PID, "default") != null;
				return super.addingService(reference);
			}
		}.open();
	}

	public void shutdown() {
		if (deployConfig != null)
			deployConfig.save();
	}

	private void checkReadiness() {
		if (nodeAvailable && userAdminAvailable && (httpExpected ? httpAvailable : true)) {
			availableSince = System.currentTimeMillis();
			long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
			log.info("## ARGEO CMS AVAILABLE in " + (jvmUptime / 1000) + "." + (jvmUptime % 1000) + "s ##");
			long begin = bc.getService(bc.getServiceReference(NodeState.class)).getAvailableSince();
			long initDuration = System.currentTimeMillis() - begin;
			if (log.isTraceEnabled())
				log.trace("Kernel initialization took " + initDuration + "ms");
			directorsCut(initDuration);
		}
	}

	final private void directorsCut(long initDuration) {
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

	private void prepareNodeRepository(Repository deployedNodeRepository) {
		if (availableSince != null) {
			throw new CmsException("Deployment is already available");
		}

		// home
		prepareDataModel(KernelUtils.openAdminSession(deployedNodeRepository));
	}

	private void prepareHomeRepository(Repository deployedRepository) {
		Hashtable<String, String> regProps = new Hashtable<String, String>();
		regProps.put(NodeConstants.CN, NodeConstants.HOME);
		regProps.put(LEGACY_JCR_REPOSITORY_ALIAS, NodeConstants.HOME);
		homeRepository = new HomeRepository(deployedRepository);
		// register
		bc.registerService(Repository.class, homeRepository, regProps);

		new ServiceTracker<CallbackHandler, CallbackHandler>(bc, CallbackHandler.class, null) {

			@Override
			public CallbackHandler addingService(ServiceReference<CallbackHandler> reference) {
				NodeKeyRing nodeKeyring = new NodeKeyRing(homeRepository);
				CallbackHandler callbackHandler = bc.getService(reference);
				nodeKeyring.setDefaultCallbackHandler(callbackHandler);
				bc.registerService(LangUtils.names(CryptoKeyring.class, ManagedService.class), nodeKeyring,
						LangUtils.dico(Constants.SERVICE_PID, NodeConstants.NODE_KEYRING_PID));
				return callbackHandler;
			}

		}.open();
	}

	/** Session is logged out. */
	private void prepareDataModel(Session adminSession) {
		try {
			Set<String> processed = new HashSet<String>();
			bundles: for (Bundle bundle : bc.getBundles()) {
				BundleWiring wiring = bundle.adapt(BundleWiring.class);
				if (wiring == null)
					continue bundles;
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
		String name = (String) attrs.get(DataModelNamespace.CAPABILITY_NAME_ATTRIBUTE);
		if (processed.contains(name)) {
			if (log.isTraceEnabled())
				log.trace("Data model " + name + " has already been processed");
			return;
		}
		String path = (String) attrs.get(DataModelNamespace.CAPABILITY_CND_ATTRIBUTE);
		URL url = capability.getRevision().getBundle().getResource(path);
		try (Reader reader = new InputStreamReader(url.openStream())) {
			CndImporter.registerNodeTypes(reader, adminSession, true);
			processed.add(name);
			if (log.isDebugEnabled())
				log.debug("Registered CND " + url);
		} catch (Exception e) {
			throw new CmsException("Cannot import CND " + url, e);
		}

		if (!asBoolean((String) attrs.get(DataModelNamespace.CAPABILITY_ABSTRACT_ATTRIBUTE))) {
			Hashtable<String, Object> properties = new Hashtable<>();
			properties.put(LEGACY_JCR_REPOSITORY_ALIAS, name);
			properties.put(NodeConstants.CN, name);
			if (name.equals(NodeConstants.NODE))
				properties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
			LocalRepository localRepository = new LocalRepository(adminSession.getRepository(), capability);
			bc.registerService(Repository.class, localRepository, properties);
			if (log.isDebugEnabled())
				log.debug("Published data model " + name);
		}
	}

	private boolean asBoolean(String value) {
		if (value == null)
			return false;
		switch (value) {
		case "true":
			return true;
		case "false":
			return false;
		default:
			throw new CmsException("Unsupported value for attribute " + DataModelNamespace.CAPABILITY_ABSTRACT_ATTRIBUTE
					+ ": " + value);
		}
	}

	@Override
	public Long getAvailableSince() {
		return availableSince;
	}

	private class RepositoryContextStc extends ServiceTracker<RepositoryContext, RepositoryContext> {

		public RepositoryContextStc() {
			super(bc, RepositoryContext.class, null);
		}

		@Override
		public RepositoryContext addingService(ServiceReference<RepositoryContext> reference) {
			RepositoryContext nodeRepo = bc.getService(reference);
			Object cn = reference.getProperty(NodeConstants.CN);
			if (cn != null) {
				if (cn.equals(NodeConstants.NODE)) {
					prepareNodeRepository(nodeRepo.getRepository());
					prepareHomeRepository(nodeRepo.getRepository());
					nodeAvailable = true;
					checkReadiness();
				}else{
					// TODO standalone
				}
			}
			return nodeRepo;
		}

		@Override
		public void modifiedService(ServiceReference<RepositoryContext> reference, RepositoryContext service) {
		}

		@Override
		public void removedService(ServiceReference<RepositoryContext> reference, RepositoryContext service) {
		}

	}

	private class PrepareHttpStc extends ServiceTracker<HttpService, HttpService> {
		private DataHttp dataHttp;
		private NodeHttp nodeHttp;

		public PrepareHttpStc() {
			super(bc, HttpService.class, null);
		}

		@Override
		public HttpService addingService(ServiceReference<HttpService> reference) {
			HttpService httpService = addHttpService(reference);
			return httpService;
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
			log.info(httpPortsMsg(httpPort, httpsPort));
			httpAvailable = true;
			checkReadiness();
			return httpService;
		}

		private String httpPortsMsg(Object httpPort, Object httpsPort) {
			return "HTTP " + httpPort + (httpsPort != null ? " - HTTPS " + httpsPort : "");
		}
	}

}
