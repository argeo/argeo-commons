package org.argeo.cms.internal.kernel;

import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.net.SocketPermission;
import java.security.AllPermission;
import java.util.PropertyPermission;

import javax.security.auth.AuthPermission;

import org.argeo.node.NodeUtils;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServicePermission;
import org.osgi.service.cm.ConfigurationPermission;
import org.osgi.service.condpermadmin.BundleLocationCondition;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.permissionadmin.PermissionInfo;

/** Security profile based on OSGi {@link PermissionAdmin}. */
public interface SecurityProfile {
	BundleContext bc = FrameworkUtil.getBundle(SecurityProfile.class).getBundleContext();

	default void applySystemPermissions(ConditionalPermissionAdmin permissionAdmin) {
		ConditionalPermissionUpdate update = permissionAdmin.newConditionalPermissionUpdate();
		// Self
		String nodeAPiBundleLocation = locate(NodeUtils.class);
		update.getConditionalPermissionInfos()
				.add(permissionAdmin.newConditionalPermissionInfo(null,
						new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
								new String[] { nodeAPiBundleLocation }) },
						new PermissionInfo[] { new PermissionInfo(AllPermission.class.getName(), null, null) },
						ConditionalPermissionInfo.ALLOW));
		String cmsBundleLocation = locate(SecurityProfile.class);
		update.getConditionalPermissionInfos()
				.add(permissionAdmin.newConditionalPermissionInfo(null,
						new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
								new String[] { cmsBundleLocation }) },
						new PermissionInfo[] { new PermissionInfo(AllPermission.class.getName(), null, null) },
						ConditionalPermissionInfo.ALLOW));
		String frameworkBundleLocation = bc.getBundle(0).getLocation();
		update.getConditionalPermissionInfos()
				.add(permissionAdmin.newConditionalPermissionInfo(null,
						new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
								new String[] { frameworkBundleLocation }) },
						new PermissionInfo[] { new PermissionInfo(AllPermission.class.getName(), null, null) },
						ConditionalPermissionInfo.ALLOW));
		// All
		// FIXME understand why Jetty and Jackrabbit require that
		update.getConditionalPermissionInfos()
				.add(permissionAdmin.newConditionalPermissionInfo(null, null, new PermissionInfo[] {
						new PermissionInfo(SocketPermission.class.getName(), "localhost:7070", "listen,resolve"),
						new PermissionInfo(FilePermission.class.getName(), "<<ALL FILES>>", "read,write,delete"),
						new PermissionInfo(PropertyPermission.class.getName(), "DEBUG", "read"),
						new PermissionInfo(PropertyPermission.class.getName(), "STOP.*", "read"),
						new PermissionInfo(PropertyPermission.class.getName(), "org.apache.jackrabbit.*", "read"),
						new PermissionInfo(RuntimePermission.class.getName(), "*", "*"), },
						ConditionalPermissionInfo.ALLOW));

		// Eclipse
		// update.getConditionalPermissionInfos()
		// .add(permissionAdmin.newConditionalPermissionInfo(null,
		// new ConditionInfo[] { new
		// ConditionInfo(BundleLocationCondition.class.getName(),
		// new String[] { "*/org.eclipse.*" }) },
		// new PermissionInfo[] { new
		// PermissionInfo(RuntimePermission.class.getName(), "*", "*"),
		// new PermissionInfo(AdminPermission.class.getName(), "*", "*"),
		// new PermissionInfo(ServicePermission.class.getName(), "*", "get"),
		// new PermissionInfo(ServicePermission.class.getName(), "*",
		// "register"),
		// new PermissionInfo(TopicPermission.class.getName(), "*", "publish"),
		// new PermissionInfo(TopicPermission.class.getName(), "*",
		// "subscribe"),
		// new PermissionInfo(PropertyPermission.class.getName(), "osgi.*",
		// "read"),
		// new PermissionInfo(PropertyPermission.class.getName(), "eclipse.*",
		// "read"),
		// new PermissionInfo(PropertyPermission.class.getName(),
		// "org.eclipse.*", "read"),
		// new PermissionInfo(PropertyPermission.class.getName(), "equinox.*",
		// "read"),
		// new PermissionInfo(PropertyPermission.class.getName(), "xml.*",
		// "read"),
		// new PermissionInfo("org.eclipse.equinox.log.LogPermission", "*",
		// "log"), },
		// ConditionalPermissionInfo.ALLOW));
		update.getConditionalPermissionInfos()
				.add(permissionAdmin.newConditionalPermissionInfo(null,
						new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
								new String[] { "*/org.eclipse.*" }) },
						new PermissionInfo[] { new PermissionInfo(AllPermission.class.getName(), null, null), },
						ConditionalPermissionInfo.ALLOW));
		update.getConditionalPermissionInfos()
				.add(permissionAdmin.newConditionalPermissionInfo(null,
						new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
								new String[] { "*/org.apache.felix.*" }) },
						new PermissionInfo[] { new PermissionInfo(AllPermission.class.getName(), null, null), },
						ConditionalPermissionInfo.ALLOW));

		// Configuration admin
//		update.getConditionalPermissionInfos().add(permissionAdmin.newConditionalPermissionInfo(null,
//				new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
//						new String[] { locate(configurationAdmin.getService().getClass()) }) },
//				new PermissionInfo[] { new PermissionInfo(ConfigurationPermission.class.getName(), "*", "configure"),
//						new PermissionInfo(AdminPermission.class.getName(), "*", "*"),
//						new PermissionInfo(PropertyPermission.class.getName(), "osgi.*", "read"), },
//				ConditionalPermissionInfo.ALLOW));

		// Bitronix
//		update.getConditionalPermissionInfos().add(permissionAdmin.newConditionalPermissionInfo(null,
//				new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
//						new String[] { locate(BitronixTransactionManager.class) }) },
//				new PermissionInfo[] { new PermissionInfo(PropertyPermission.class.getName(), "bitronix.tm.*", "read"),
//						new PermissionInfo(RuntimePermission.class.getName(), "getClassLoader", null),
//						new PermissionInfo(MBeanServerPermission.class.getName(), "createMBeanServer", null),
//						new PermissionInfo(MBeanPermission.class.getName(), "bitronix.tm.*", "registerMBean"),
//						new PermissionInfo(MBeanTrustPermission.class.getName(), "register", null) },
//				ConditionalPermissionInfo.ALLOW));

		// DS
		Bundle dsBundle = findBundle("org.eclipse.equinox.ds");
		update.getConditionalPermissionInfos().add(permissionAdmin.newConditionalPermissionInfo(null,
				new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
						new String[] { dsBundle.getLocation() }) },
				new PermissionInfo[] { new PermissionInfo(ConfigurationPermission.class.getName(), "*", "configure"),
						new PermissionInfo(AdminPermission.class.getName(), "*", "*"),
						new PermissionInfo(ServicePermission.class.getName(), "*", "get"),
						new PermissionInfo(ServicePermission.class.getName(), "*", "register"),
						new PermissionInfo(PropertyPermission.class.getName(), "osgi.*", "read"),
						new PermissionInfo(PropertyPermission.class.getName(), "xml.*", "read"),
						new PermissionInfo(PropertyPermission.class.getName(), "equinox.*", "read"),
						new PermissionInfo(RuntimePermission.class.getName(), "accessDeclaredMembers", null),
						new PermissionInfo(RuntimePermission.class.getName(), "getClassLoader", null),
						new PermissionInfo(ReflectPermission.class.getName(), "suppressAccessChecks", null), },
				ConditionalPermissionInfo.ALLOW));

		// Jetty
		// Bundle jettyUtilBundle = findBundle("org.eclipse.equinox.http.jetty");
		update.getConditionalPermissionInfos().add(permissionAdmin.newConditionalPermissionInfo(null,
				new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
						new String[] { "*/org.eclipse.jetty.*" }) },
				new PermissionInfo[] {
						new PermissionInfo(FilePermission.class.getName(), "<<ALL FILES>>", "read,write,delete"), },
				ConditionalPermissionInfo.ALLOW));
		Bundle servletBundle = findBundle("javax.servlet");
		update.getConditionalPermissionInfos().add(permissionAdmin.newConditionalPermissionInfo(null,
				new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
						new String[] { servletBundle.getLocation() }) },
				new PermissionInfo[] { new PermissionInfo(PropertyPermission.class.getName(),
						"org.glassfish.web.rfc2109_cookie_names_enforced", "read") },
				ConditionalPermissionInfo.ALLOW));

		// required to be able to get the BundleContext in the customizer
		Bundle jettyCustomizerBundle = findBundle("org.argeo.ext.equinox.jetty");
		update.getConditionalPermissionInfos()
				.add(permissionAdmin.newConditionalPermissionInfo(null,
						new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
								new String[] { jettyCustomizerBundle.getLocation() }) },
						new PermissionInfo[] { new PermissionInfo(AdminPermission.class.getName(), "*", "*"), },
						ConditionalPermissionInfo.ALLOW));

		// Blueprint
//		Bundle blueprintBundle = findBundle("org.eclipse.gemini.blueprint.core");
//		update.getConditionalPermissionInfos()
//				.add(permissionAdmin.newConditionalPermissionInfo(null,
//						new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
//								new String[] { blueprintBundle.getLocation() }) },
//						new PermissionInfo[] { new PermissionInfo(RuntimePermission.class.getName(), "*", null),
//								new PermissionInfo(AdminPermission.class.getName(), "*", "*"), },
//						ConditionalPermissionInfo.ALLOW));
//		Bundle blueprintExtenderBundle = findBundle("org.eclipse.gemini.blueprint.extender");
//		update.getConditionalPermissionInfos()
//				.add(permissionAdmin
//						.newConditionalPermissionInfo(null,
//								new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
//										new String[] { blueprintExtenderBundle.getLocation() }) },
//								new PermissionInfo[] { new PermissionInfo(RuntimePermission.class.getName(), "*", null),
//										new PermissionInfo(PropertyPermission.class.getName(), "org.eclipse.gemini.*",
//												"read"),
//										new PermissionInfo(AdminPermission.class.getName(), "*", "*"),
//										new PermissionInfo(ServicePermission.class.getName(), "*", "register"), },
//								ConditionalPermissionInfo.ALLOW));
//		Bundle springCoreBundle = findBundle("org.springframework.core");
//		update.getConditionalPermissionInfos()
//				.add(permissionAdmin.newConditionalPermissionInfo(null,
//						new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
//								new String[] { springCoreBundle.getLocation() }) },
//						new PermissionInfo[] { new PermissionInfo(RuntimePermission.class.getName(), "*", null),
//								new PermissionInfo(AdminPermission.class.getName(), "*", "*"), },
//						ConditionalPermissionInfo.ALLOW));
//		Bundle blueprintIoBundle = findBundle("org.eclipse.gemini.blueprint.io");
//		update.getConditionalPermissionInfos()
//				.add(permissionAdmin.newConditionalPermissionInfo(null,
//						new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
//								new String[] { blueprintIoBundle.getLocation() }) },
//						new PermissionInfo[] { new PermissionInfo(RuntimePermission.class.getName(), "*", null),
//								new PermissionInfo(AdminPermission.class.getName(), "*", "*"), },
//						ConditionalPermissionInfo.ALLOW));

		// Equinox
		Bundle registryBundle = findBundle("org.eclipse.equinox.registry");
		update.getConditionalPermissionInfos().add(permissionAdmin.newConditionalPermissionInfo(null,
				new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
						new String[] { registryBundle.getLocation() }) },
				new PermissionInfo[] { new PermissionInfo(PropertyPermission.class.getName(), "eclipse.*", "read"),
						new PermissionInfo(PropertyPermission.class.getName(), "osgi.*", "read"),
						new PermissionInfo(FilePermission.class.getName(), "<<ALL FILES>>", "read,write,delete"), },
				ConditionalPermissionInfo.ALLOW));

		Bundle equinoxUtilBundle = findBundle("org.eclipse.equinox.util");
		update.getConditionalPermissionInfos().add(permissionAdmin.newConditionalPermissionInfo(null,
				new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
						new String[] { equinoxUtilBundle.getLocation() }) },
				new PermissionInfo[] { new PermissionInfo(PropertyPermission.class.getName(), "equinox.*", "read"),
						new PermissionInfo(ServicePermission.class.getName(), "*", "get"),
						new PermissionInfo(ServicePermission.class.getName(), "*", "register"), },
				ConditionalPermissionInfo.ALLOW));
		Bundle equinoxCommonBundle = findBundle("org.eclipse.equinox.common");
		update.getConditionalPermissionInfos()
				.add(permissionAdmin.newConditionalPermissionInfo(null,
						new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
								new String[] { equinoxCommonBundle.getLocation() }) },
						new PermissionInfo[] { new PermissionInfo(AdminPermission.class.getName(), "*", "*"), },
						ConditionalPermissionInfo.ALLOW));

		Bundle consoleBundle = findBundle("org.eclipse.equinox.console");
		update.getConditionalPermissionInfos()
				.add(permissionAdmin.newConditionalPermissionInfo(null,
						new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
								new String[] { consoleBundle.getLocation() }) },
						new PermissionInfo[] { new PermissionInfo(ServicePermission.class.getName(), "*", "register"),
								new PermissionInfo(AdminPermission.class.getName(), "*", "listener") },
						ConditionalPermissionInfo.ALLOW));
		Bundle preferencesBundle = findBundle("org.eclipse.equinox.preferences");
		update.getConditionalPermissionInfos().add(permissionAdmin.newConditionalPermissionInfo(null,
				new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
						new String[] { preferencesBundle.getLocation() }) },
				new PermissionInfo[] {
						new PermissionInfo(FilePermission.class.getName(), "<<ALL FILES>>", "read,write,delete"), },
				ConditionalPermissionInfo.ALLOW));
		Bundle appBundle = findBundle("org.eclipse.equinox.app");
		update.getConditionalPermissionInfos().add(permissionAdmin.newConditionalPermissionInfo(null,
				new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
						new String[] { appBundle.getLocation() }) },
				new PermissionInfo[] {
						new PermissionInfo(FilePermission.class.getName(), "<<ALL FILES>>", "read,write,delete"), },
				ConditionalPermissionInfo.ALLOW));

		// Jackrabbit
		Bundle jackrabbitCoreBundle = findBundle("org.apache.jackrabbit.core");
		update.getConditionalPermissionInfos().add(permissionAdmin.newConditionalPermissionInfo(null,
				new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
						new String[] { jackrabbitCoreBundle.getLocation() }) },
				new PermissionInfo[] {
						new PermissionInfo(FilePermission.class.getName(), "<<ALL FILES>>", "read,write,delete"),
						new PermissionInfo(PropertyPermission.class.getName(), "*", "read,write"),
						new PermissionInfo(AuthPermission.class.getName(), "getSubject", null),
						new PermissionInfo(AuthPermission.class.getName(), "getLoginConfiguration", null),
						new PermissionInfo(AuthPermission.class.getName(), "createLoginContext.Jackrabbit", null), },
				ConditionalPermissionInfo.ALLOW));
		Bundle jackrabbitDataBundle = findBundle("org.apache.jackrabbit.data");
		update.getConditionalPermissionInfos().add(permissionAdmin.newConditionalPermissionInfo(null,
				new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
						new String[] { jackrabbitDataBundle.getLocation() }) },
				new PermissionInfo[] { new PermissionInfo(PropertyPermission.class.getName(), "*", "read,write") },
				ConditionalPermissionInfo.ALLOW));
		Bundle jackrabbitCommonBundle = findBundle("org.apache.jackrabbit.jcr.commons");
		update.getConditionalPermissionInfos().add(permissionAdmin.newConditionalPermissionInfo(null,
				new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
						new String[] { jackrabbitCommonBundle.getLocation() }) },
				new PermissionInfo[] { new PermissionInfo(AuthPermission.class.getName(), "getSubject", null),
						new PermissionInfo(AuthPermission.class.getName(), "createLoginContext.Jackrabbit", null), },
				ConditionalPermissionInfo.ALLOW));

		Bundle jackrabbitExtBundle = findBundle("org.argeo.ext.jackrabbit");
		update.getConditionalPermissionInfos()
				.add(permissionAdmin.newConditionalPermissionInfo(null,
						new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
								new String[] { jackrabbitExtBundle.getLocation() }) },
						new PermissionInfo[] { new PermissionInfo(AuthPermission.class.getName(), "*", "*"), },
						ConditionalPermissionInfo.ALLOW));

		// Tika
		Bundle tikaCoreBundle = findBundle("org.apache.tika.core");
		update.getConditionalPermissionInfos().add(permissionAdmin.newConditionalPermissionInfo(null,
				new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
						new String[] { tikaCoreBundle.getLocation() }) },
				new PermissionInfo[] { new PermissionInfo(PropertyPermission.class.getName(), "*", "read,write"),
						new PermissionInfo(AdminPermission.class.getName(), "*", "*") },
				ConditionalPermissionInfo.ALLOW));
		Bundle luceneBundle = findBundle("org.apache.lucene");
		update.getConditionalPermissionInfos().add(permissionAdmin.newConditionalPermissionInfo(null,
				new ConditionInfo[] { new ConditionInfo(BundleLocationCondition.class.getName(),
						new String[] { luceneBundle.getLocation() }) },
				new PermissionInfo[] {
						new PermissionInfo(FilePermission.class.getName(), "<<ALL FILES>>", "read,write,delete"),
						new PermissionInfo(PropertyPermission.class.getName(), "*", "read"),
						new PermissionInfo(AdminPermission.class.getName(), "*", "*") },
				ConditionalPermissionInfo.ALLOW));

		// COMMIT
		update.commit();
	}

	/** @return bundle location */
	default String locate(Class<?> clzz) {
		return FrameworkUtil.getBundle(clzz).getLocation();
	}

	/** Can be null */
	default Bundle findBundle(String symbolicName) {
		for (Bundle b : bc.getBundles())
			if (b.getSymbolicName().equals(symbolicName))
				return b;
		return null;
	}

}
