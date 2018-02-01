package org.argeo.cms.internal.kernel;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.List;
import java.util.Locale;

import javax.security.auth.login.Configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.node.ArgeoLogger;
import org.argeo.node.NodeConstants;
import org.argeo.node.NodeDeployment;
import org.argeo.node.NodeInstance;
import org.argeo.node.NodeState;
import org.argeo.util.LangUtils;
import org.ietf.jgss.GSSCredential;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.useradmin.UserAdmin;

/**
 * Activates the kernel. Gives access to kernel information for the rest of the
 * bundle (and only it)
 */
public class Activator implements BundleActivator {
	private final static Log log = LogFactory.getLog(Activator.class);

	private static Activator instance;

	private BundleContext bc;
	// private CmsSecurity nodeSecurity;
	private LogReaderService logReaderService;
	// private ConfigurationAdmin configurationAdmin;

	private NodeLogger logger;
	private CmsState nodeState;
	private CmsDeployment nodeDeployment;
	private CmsInstance nodeInstance;

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		Runtime.getRuntime().addShutdownHook(new CmsShutdown());
		instance = this;
		this.bc = bundleContext;
		this.logReaderService = getService(LogReaderService.class);
		// this.configurationAdmin = getService(ConfigurationAdmin.class);

		try {
			// nodeSecurity = new CmsSecurity();
			initSecurity();
			initArgeoLogger();
			initNode();
		} catch (Exception e) {
			log.error("## FATAL: CMS activator failed", e);
		}
	}

	private void initSecurity() {
		if (System.getProperty(KernelConstants.JAAS_CONFIG_PROP) == null) {
			String jaasConfig = KernelConstants.JAAS_CONFIG;
			URL url = getClass().getClassLoader().getResource(jaasConfig);
			// System.setProperty(KernelConstants.JAAS_CONFIG_PROP,
			// url.toExternalForm());
			KernelUtils.setJaasConfiguration(url);
		}
		// explicitly load JAAS configuration
		Configuration.getConfiguration();

		// ConditionalPermissionAdmin permissionAdmin = bc
		// .getService(bc.getServiceReference(ConditionalPermissionAdmin.class));
		// ConditionalPermissionUpdate update =
		// permissionAdmin.newConditionalPermissionUpdate();
		// // Self
		// update.getConditionalPermissionInfos()
		// .add(permissionAdmin.newConditionalPermissionInfo(null,
		// new ConditionInfo[] {
		// new ConditionInfo(BundleLocationCondition.class.getName(), new
		// String[] { "*" }) },
		// new PermissionInfo[] { new
		// PermissionInfo(AllPermission.class.getName(), null, null) },
		// ConditionalPermissionInfo.ALLOW));
		//
	}

	private void initArgeoLogger() {
		// Jetty
		// disable integration of Jetty logging with SLF4J
		// in order to avoid chicken and egg problems
		// org.eclipse.jetty.util.log.Log.setLog(new StdErrLog());
		// org.eclipse.jetty.util.log.Logger jettyLog =
		// org.eclipse.jetty.util.log.Log.getLog();
		// if (jettyLog != null) {
		// jettyLog.warn("TEST JETTY LOG", new Object[0]);
		// }

		logger = new NodeLogger(logReaderService);
		bc.registerService(ArgeoLogger.class, logger, null);
	}

	private void initNode() throws IOException {
		// Node state
		Path stateUuidPath = bc.getDataFile("stateUuid").toPath();
		String stateUuid;
		if (Files.exists(stateUuidPath)) {
			stateUuid = Files.readAllLines(stateUuidPath).get(0);
		} else {
			stateUuid = bc.getProperty(Constants.FRAMEWORK_UUID);
			Files.write(stateUuidPath, stateUuid.getBytes());
		}
		nodeState = new CmsState(stateUuid);
		Dictionary<String, Object> regProps = LangUtils.dico(Constants.SERVICE_PID, NodeConstants.NODE_STATE_PID);
		regProps.put(NodeConstants.CN, stateUuid);
		bc.registerService(NodeState.class, nodeState, regProps);

		// Node deployment
		nodeDeployment = new CmsDeployment();
		bc.registerService(NodeDeployment.class, nodeDeployment, null);

		// Node instance
		nodeInstance = new CmsInstance();
		bc.registerService(NodeInstance.class, nodeInstance, null);
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		try {
			if (nodeInstance != null)
				nodeInstance.shutdown();
			if (nodeDeployment != null)
				nodeDeployment.shutdown();
			if (nodeState != null)
				nodeState.shutdown();

			instance = null;
			this.bc = null;
			this.logReaderService = null;
			// this.configurationAdmin = null;
		} catch (Exception e) {
			log.error("CMS activator shutdown failed", e);
		}
	}

	private <T> T getService(Class<T> clazz) {
		ServiceReference<T> sr = bc.getServiceReference(clazz);
		if (sr == null)
			throw new CmsException("No service available for " + clazz);
		return bc.getService(sr);
	}

	public static NodeState getNodeState() {
		return instance.nodeState;
	}

	public static GSSCredential getAcceptorCredentials() {
		return getNodeUserAdmin().getAcceptorCredentials();
	}

	public static boolean isSingleUser() {
		return getNodeUserAdmin().isSingleUser();
	}

	private static NodeUserAdmin getNodeUserAdmin() {
		ServiceReference<UserAdmin> sr = instance.bc.getServiceReference(UserAdmin.class);
		NodeUserAdmin userAdmin = (NodeUserAdmin) instance.bc.getService(sr);
		return userAdmin;

	}

	// static CmsSecurity getCmsSecurity() {
	// return instance.nodeSecurity;
	// }

	public String[] getLocales() {
		// TODO optimize?
		List<Locale> locales = getNodeState().getLocales();
		String[] res = new String[locales.size()];
		for (int i = 0; i < locales.size(); i++)
			res[i] = locales.get(i).toString();
		return res;
	}

}
