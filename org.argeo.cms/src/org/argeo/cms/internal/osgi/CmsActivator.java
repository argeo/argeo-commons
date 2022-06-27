package org.argeo.cms.internal.osgi;

import java.security.AllPermission;
import java.util.Dictionary;

import org.argeo.api.cms.CmsLog;
import org.argeo.cms.ArgeoLogger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.condpermadmin.BundleLocationCondition;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * Activates the kernel. Gives access to kernel information for the rest of the
 * bundle (and only it)
 */
public class CmsActivator implements BundleActivator {
	private final static CmsLog log = CmsLog.getLog(CmsActivator.class);

//	private static Activator instance;

	// TODO make it configurable
	private boolean hardened = false;

	private static BundleContext bundleContext;

	private LogReaderService logReaderService;

	private CmsOsgiLogger logger;
//	private CmsStateImpl nodeState;
//	private CmsDeploymentImpl nodeDeployment;
//	private CmsContextImpl nodeInstance;

//	private ServiceTracker<UserAdmin, NodeUserAdmin> userAdminSt;

//	static {
//		Bundle bundle = FrameworkUtil.getBundle(Activator.class);
//		if (bundle != null) {
//			bundleContext = bundle.getBundleContext();
//		}
//	}

	void init() {
//		Runtime.getRuntime().addShutdownHook(new CmsShutdown());
//		instance = this;
//		this.bc = bundleContext;
		if (bundleContext != null)
			this.logReaderService = getService(LogReaderService.class);
		initArgeoLogger();
//		this.internalExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
//
//		try {
//			initSecurity();
////			initArgeoLogger();
//			initNode();
//
//			if (log.isTraceEnabled())
//				log.trace("Kernel bundle started");
//		} catch (Throwable e) {
//			log.error("## FATAL: CMS activator failed", e);
//		}
	}

	void destroy() {
		try {
//			if (nodeInstance != null)
//				nodeInstance.shutdown();
//			if (nodeDeployment != null)
//				nodeDeployment.shutdown();
//			if (nodeState != null)
//				nodeState.shutdown();
//
//			if (userAdminSt != null)
//				userAdminSt.close();

//			internalExecutorService.shutdown();
//			instance = null;
			bundleContext = null;
			this.logReaderService = null;
			// this.configurationAdmin = null;
		} catch (Exception e) {
			log.error("CMS activator shutdown failed", e);
		}
		
		new GogoShellKiller().start();
	}

	private void initSecurity() {
		// code-level permissions
		String osgiSecurity = bundleContext.getProperty(Constants.FRAMEWORK_SECURITY);
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
		logger = new CmsOsgiLogger(logReaderService);
		if (bundleContext != null)
			bundleContext.registerService(ArgeoLogger.class, logger, null);
	}

//	private void initNode() throws IOException {
//		// Node state
//		nodeState = new CmsStateImpl();
//		registerService(CmsState.class, nodeState, null);
//
//		// Node deployment
//		nodeDeployment = new CmsDeploymentImpl();
////		registerService(NodeDeployment.class, nodeDeployment, null);
//
//		// Node instance
//		nodeInstance = new CmsContextImpl();
//		registerService(CmsContext.class, nodeInstance, null);
//	}

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
		bundleContext = bc;
//		if (!bc.getBundle().equals(bundleContext.getBundle()))
//			throw new IllegalStateException(
//					"Bundle " + bc.getBundle() + " is not consistent with " + bundleContext.getBundle());
		init();
//		userAdminSt = new ServiceTracker<>(bundleContext, UserAdmin.class, null);
//		userAdminSt.open();

//		ServiceTracker<?, ?> httpSt = new ServiceTracker<HttpService, HttpService>(bc, HttpService.class, null) {
//
//			@Override
//			public HttpService addingService(ServiceReference<HttpService> sr) {
//				Object httpPort = sr.getProperty("http.port");
//				Object httpsPort = sr.getProperty("https.port");
//				log.info(httpPortsMsg(httpPort, httpsPort));
//				close();
//				return super.addingService(sr);
//			}
//		};
//		httpSt.open();
	}

	private String httpPortsMsg(Object httpPort, Object httpsPort) {
		return (httpPort != null ? "HTTP " + httpPort + " " : " ") + (httpsPort != null ? "HTTPS " + httpsPort : "");
	}

	@Override
	public void stop(BundleContext bc) throws Exception {
//		if (!bc.getBundle().equals(bundleContext.getBundle()))
//			throw new IllegalStateException(
//					"Bundle " + bc.getBundle() + " is not consistent with " + bundleContext.getBundle());
		destroy();
		bundleContext = null;
	}

//	private <T> T getService(Class<T> clazz) {
//		ServiceReference<T> sr = bundleContext.getServiceReference(clazz);
//		if (sr == null)
//			throw new IllegalStateException("No service available for " + clazz);
//		return bundleContext.getService(sr);
//	}

//	public static GSSCredential getAcceptorCredentials() {
//		return getNodeUserAdmin().getAcceptorCredentials();
//	}
//
//	@Deprecated
//	public static boolean isSingleUser() {
//		return getNodeUserAdmin().isSingleUser();
//	}
//
//	public static UserAdmin getUserAdmin() {
//		return (UserAdmin) getNodeUserAdmin();
//	}
//
//	public static String getHttpProxySslHeader() {
//		return KernelUtils.getFrameworkProp(CmsConstants.HTTP_PROXY_SSL_DN);
//	}
//
//	private static NodeUserAdmin getNodeUserAdmin() {
//		NodeUserAdmin res;
//		try {
//			res = instance.userAdminSt.waitForService(60000);
//		} catch (InterruptedException e) {
//			throw new IllegalStateException("Cannot retrieve Node user admin", e);
//		}
//		if (res == null)
//			throw new IllegalStateException("No Node user admin found");
//
//		return res;
//		// ServiceReference<UserAdmin> sr =
//		// instance.bc.getServiceReference(UserAdmin.class);
//		// NodeUserAdmin userAdmin = (NodeUserAdmin) instance.bc.getService(sr);
//		// return userAdmin;
//
//	}

//	public static ExecutorService getInternalExecutorService() {
//		return instance.internalExecutorService;
//	}

	// static CmsSecurity getCmsSecurity() {
	// return instance.nodeSecurity;
	// }

//	public String[] getLocales() {
//		// TODO optimize?
//		List<Locale> locales = CmsStateImpl.getNodeState().getLocales();
//		String[] res = new String[locales.size()];
//		for (int i = 0; i < locales.size(); i++)
//			res[i] = locales.get(i).toString();
//		return res;
//	}

	public static BundleContext getBundleContext() {
		return bundleContext;
	}

}
