package org.argeo.cms.internal.osgi;

import java.security.AllPermission;
import java.util.Dictionary;

import org.argeo.api.cms.CmsLog;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.condpermadmin.BundleLocationCondition;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * Activates the kernel. Gives access to kernel information for the rest of the
 * bundle (and only it)
 */
public class CmsActivator implements BundleActivator {
	private final static CmsLog log = CmsLog.getLog(CmsActivator.class);

	// TODO make it configurable
	private boolean hardened = false;

	private static BundleContext bundleContext;

//	private LogReaderService logReaderService;
//
//	private CmsOsgiLogger logger;

	void init() {
//		Runtime.getRuntime().addShutdownHook(new CmsShutdown());
//		instance = this;
//		this.bc = bundleContext;
//		if (bundleContext != null)
//			this.logReaderService = getService(LogReaderService.class);
//		initArgeoLogger();
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
			bundleContext = null;
//			this.logReaderService = null;
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

//	private void initArgeoLogger() {
//		logger = new CmsOsgiLogger(logReaderService);
//		if (bundleContext != null)
//			bundleContext.registerService(ArgeoLogger.class, logger, null);
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

		init();

	}

	@Override
	public void stop(BundleContext bc) throws Exception {

		destroy();
		bundleContext = null;
	}


	public static BundleContext getBundleContext() {
		return bundleContext;
	}

}
