package org.argeo.cms.internal.kernel;

import java.io.IOException;
import java.net.URL;
import java.security.AllPermission;
import java.util.Dictionary;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.security.auth.login.Configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.api.ArgeoLogger;
import org.argeo.api.NodeConstants;
import org.argeo.api.NodeDeployment;
import org.argeo.api.NodeInstance;
import org.argeo.api.NodeState;
import org.argeo.ident.IdentClient;
import org.ietf.jgss.GSSCredential;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.condpermadmin.BundleLocationCondition;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.permissionadmin.PermissionInfo;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Activates the kernel. Gives access to kernel information for the rest of the
 * bundle (and only it)
 */
public class Activator implements BundleActivator {
	private final static Log log = LogFactory.getLog(Activator.class);

	private static Activator instance;

	// TODO make it configurable
	private boolean hardened = false;

	private static BundleContext bundleContext;

	private LogReaderService logReaderService;

	private NodeLogger logger;
	private CmsState nodeState;
	private CmsDeployment nodeDeployment;
	private CmsInstance nodeInstance;

	private ServiceTracker<UserAdmin, NodeUserAdmin> userAdminSt;
	private ExecutorService internalExecutorService;

	static {
		Bundle bundle = FrameworkUtil.getBundle(Activator.class);
		if (bundle != null) {
			bundleContext = bundle.getBundleContext();
		}
	}

	void init() {
		Runtime.getRuntime().addShutdownHook(new CmsShutdown());
		instance = this;
//		this.bc = bundleContext;
		if (bundleContext != null)
			this.logReaderService = getService(LogReaderService.class);
		this.internalExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		try {
			initSecurity();
			initArgeoLogger();
			initNode();

			if (log.isTraceEnabled())
				log.trace("Kernel bundle started");
		} catch (Throwable e) {
			log.error("## FATAL: CMS activator failed", e);
		}
	}

	void destroy() {
		try {
			if (nodeInstance != null)
				nodeInstance.shutdown();
			if (nodeDeployment != null)
				nodeDeployment.shutdown();
			if (nodeState != null)
				nodeState.shutdown();

			if (userAdminSt != null)
				userAdminSt.close();

			internalExecutorService.shutdown();
			instance = null;
			bundleContext = null;
			this.logReaderService = null;
			// this.configurationAdmin = null;
		} catch (Exception e) {
			log.error("CMS activator shutdown failed", e);
		}
	}

	private void initSecurity() {
		if (System.getProperty(KernelConstants.JAAS_CONFIG_PROP) == null) {
			String jaasConfig = KernelConstants.JAAS_CONFIG;
			URL url = getClass().getResource(jaasConfig);
			// System.setProperty(KernelConstants.JAAS_CONFIG_PROP,
			// url.toExternalForm());
			KernelUtils.setJaasConfiguration(url);
		}
		// explicitly load JAAS configuration
		Configuration.getConfiguration();

		// code-level permissions
		String osgiSecurity = KernelUtils.getFrameworkProp(Constants.FRAMEWORK_SECURITY);
		if (osgiSecurity != null && Constants.FRAMEWORK_SECURITY_OSGI.equals(osgiSecurity)) {
			// TODO rather use a tracker?
			ConditionalPermissionAdmin permissionAdmin = bundleContext
					.getService(bundleContext.getServiceReference(ConditionalPermissionAdmin.class));
			if (!hardened) {
				// All permissions to all bundles
				ConditionalPermissionUpdate update = permissionAdmin.newConditionalPermissionUpdate();
				update.getConditionalPermissionInfos().add(permissionAdmin.newConditionalPermissionInfo(null,
						new ConditionInfo[] {
								new ConditionInfo(BundleLocationCondition.class.getName(), new String[] { "*" }) },
						new PermissionInfo[] { new PermissionInfo(AllPermission.class.getName(), null, null) },
						ConditionalPermissionInfo.ALLOW));
				// TODO data admin permission
//				PermissionInfo dataAdminPerm = new PermissionInfo(AuthPermission.class.getName(),
//						"createLoginContext." + NodeConstants.LOGIN_CONTEXT_DATA_ADMIN, null);
//				update.getConditionalPermissionInfos().add(permissionAdmin.newConditionalPermissionInfo(null,
//						new ConditionInfo[] {
//								new ConditionInfo(BundleLocationCondition.class.getName(), new String[] { "*" }) },
//						new PermissionInfo[] { dataAdminPerm }, ConditionalPermissionInfo.DENY));
//				update.getConditionalPermissionInfos().add(permissionAdmin.newConditionalPermissionInfo(null,
//						new ConditionInfo[] {
//								new ConditionInfo(BundleSignerCondition.class.getName(), new String[] { "CN=\"Eclipse.org Foundation, Inc.\", OU=IT, O=\"Eclipse.org Foundation, Inc.\", L=Nepean, ST=Ontario, C=CA" }) },
//						new PermissionInfo[] { dataAdminPerm }, ConditionalPermissionInfo.ALLOW));
				update.commit();
			} else {
				SecurityProfile securityProfile = new SecurityProfile() {
				};
				securityProfile.applySystemPermissions(permissionAdmin);
			}
		}

	}

	private void initArgeoLogger() {
		logger = new NodeLogger(logReaderService);
		if (bundleContext != null)
			bundleContext.registerService(ArgeoLogger.class, logger, null);
	}

	private void initNode() throws IOException {
		// Node state
		nodeState = new CmsState();
		registerService(NodeState.class, nodeState, null);

		// Node deployment
		nodeDeployment = new CmsDeployment();
//		registerService(NodeDeployment.class, nodeDeployment, null);

		// Node instance
		nodeInstance = new CmsInstance();
		registerService(NodeInstance.class, nodeInstance, null);
	}

	public static <T> void registerService(Class<T> clss, T service, Dictionary<String, ?> properties) {
		if (bundleContext != null) {
			bundleContext.registerService(clss, service, properties);
		}

	}

	public static <T> T getService(Class<T> clss) {
		if (bundleContext != null) {
			return bundleContext.getService(bundleContext.getServiceReference(clss));
		} else {
			return null;
		}
	}

	/*
	 * OSGi
	 */

	@Override
	public void start(BundleContext bc) throws Exception {
		if (!bc.getBundle().equals(bundleContext.getBundle()))
			throw new IllegalStateException(
					"Bundle " + bc.getBundle() + " is not consistent with " + bundleContext.getBundle());
		init();
		userAdminSt = new ServiceTracker<>(bundleContext, UserAdmin.class, null);
		userAdminSt.open();
	}

	@Override
	public void stop(BundleContext bc) throws Exception {
		if (!bc.getBundle().equals(bundleContext.getBundle()))
			throw new IllegalStateException(
					"Bundle " + bc.getBundle() + " is not consistent with " + bundleContext.getBundle());
		destroy();
	}

//	private <T> T getService(Class<T> clazz) {
//		ServiceReference<T> sr = bundleContext.getServiceReference(clazz);
//		if (sr == null)
//			throw new IllegalStateException("No service available for " + clazz);
//		return bundleContext.getService(sr);
//	}

	public static NodeState getNodeState() {
		return instance.nodeState;
	}

	public static GSSCredential getAcceptorCredentials() {
		return getNodeUserAdmin().getAcceptorCredentials();
	}

	@Deprecated
	public static boolean isSingleUser() {
		return getNodeUserAdmin().isSingleUser();
	}

	public static UserAdmin getUserAdmin() {
		return (UserAdmin) getNodeUserAdmin();
	}

	public static String getHttpProxySslHeader() {
		return KernelUtils.getFrameworkProp(NodeConstants.HTTP_PROXY_SSL_DN);
	}

	public static IdentClient getIdentClient(String remoteAddr) {
		if (!IdentClient.isDefaultAuthdPassphraseFileAvailable())
			return null;
		// TODO make passphrase more configurable
		return new IdentClient(remoteAddr);
	}

	private static NodeUserAdmin getNodeUserAdmin() {
		NodeUserAdmin res;
		try {
			res = instance.userAdminSt.waitForService(60000);
		} catch (InterruptedException e) {
			throw new IllegalStateException("Cannot retrieve Node user admin", e);
		}
		if (res == null)
			throw new IllegalStateException("No Node user admin found");

		return res;
		// ServiceReference<UserAdmin> sr =
		// instance.bc.getServiceReference(UserAdmin.class);
		// NodeUserAdmin userAdmin = (NodeUserAdmin) instance.bc.getService(sr);
		// return userAdmin;

	}

	static ExecutorService getInternalExecutorService() {
		return instance.internalExecutorService;
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

	static BundleContext getBundleContext() {
		return bundleContext;
	}

	public static void main(String[] args) {
		instance = new Activator();
		instance.init();
	}

}
