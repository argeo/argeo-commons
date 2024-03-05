package org.argeo.cms.internal.osgi;

import java.security.AllPermission;

import org.argeo.api.cms.CmsState;
import org.argeo.api.uuid.NodeIdSupplier;
import org.argeo.cms.internal.runtime.CmsStateImpl;
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
//	private final static CmsLog log = CmsLog.getLog(CmsActivator.class);

	// TODO make it configurable
	private boolean hardened = false;

	private static BundleContext bundleContext;

	void init() {
	}

	void destroy() {
		new GogoShellKiller().start();
	}

	protected void initSecurity() {
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

//	static <T> void registerService(Class<T> clss, T service, Dictionary<String, ?> properties) {
//		if (bundleContext != null) {
//			bundleContext.registerService(clss, service, properties);
//		}
//
//	}
//
	static <T> T getService(Class<T> clss) {
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
		CmsStateImpl cmsState = new CmsStateImpl();
		cmsState.start();
		bundleContext.registerService(new String[] { CmsState.class.getName(), NodeIdSupplier.class.getName() },
				cmsState, null);
		init();

	}

	@Override
	public void stop(BundleContext bc) throws Exception {
		try {
			destroy();
			CmsStateImpl cmsState = (CmsStateImpl) getService(CmsState.class);
			cmsState.stop();
		} finally {
			bundleContext = null;
		}
	}

	static BundleContext getBundleContext() {
		return bundleContext;
	}

	public static String getFrameworkProperty(String key) {
		if (bundleContext == null)
			return null;
		return getBundleContext().getProperty(key);
	}
}
