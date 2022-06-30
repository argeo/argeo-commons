package org.argeo.cms.internal.runtime;

/** Internal CMS constants. */
interface KernelConstants {
	// Directories
	String DIR_NODE = "node";
//	String DIR_REPOS = "repos";
//	String DIR_INDEXES = "indexes";
//	String DIR_TRANSACTIONS = "transactions";

	// Files
//	String DEPLOY_CONFIG_PATH = DIR_NODE + '/' + CmsConstants.DEPLOY_BASEDN + ".ldif";
	String NODE_KEY_TAB_PATH = DIR_NODE + "/krb5.keytab";

	// Security
	String JAAS_CONFIG = "/org/argeo/cms/internal/runtime/jaas.cfg";
	String JAAS_CONFIG_IPA = "/org/argeo/cms/internal/runtime/jaas-ipa.cfg";

	// Java
//	String JAAS_CONFIG_PROP = "java.security.auth.login.config";

	// DEFAULTS JCR PATH
//	String DEFAULT_HOME_BASE_PATH = "/home";
//	String DEFAULT_USERS_BASE_PATH = "/users";
//	String DEFAULT_GROUPS_BASE_PATH = "/groups";
	
	// KERBEROS
	String DEFAULT_KERBEROS_SERVICE = "HTTP";

	// HTTP client
	String COOKIE_POLICY_BROWSER_COMPATIBILITY = "compatibility";

	// RWT / RAP
	// String PATH_WORKBENCH = "/ui";
	// String PATH_WORKBENCH_PUBLIC = PATH_WORKBENCH + "/public";

//	String JETTY_FACTORY_PID = "org.eclipse.equinox.http.jetty.config";
//	String JETTY_FACTORY_PID = "org.argeo.equinox.jetty.config";
	// default Jetty server configured via JettyConfigurator
//	String DEFAULT_JETTY_SERVER = "default";
//	String CMS_JETTY_CUSTOMIZER_CLASS = "org.argeo.equinox.jetty.CmsJettyCustomizer";

	// avoid dependencies
//	String JACKRABBIT_REPOSITORY_URI = "org.apache.jackrabbit.repository.uri";
//	String JACKRABBIT_REMOTE_DEFAULT_WORKSPACE = "org.apache.jackrabbit.spi2davex.WorkspaceNameDefault";
}
