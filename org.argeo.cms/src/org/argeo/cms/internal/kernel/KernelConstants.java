package org.argeo.cms.internal.kernel;

public interface KernelConstants {
	final static String NODE_INIT = "argeo.node.init";

	// Node
	final static String REPO_HOME = "argeo.node.repo.home";
	final static String REPO_TYPE = "argeo.node.repo.type";
	// final static String REPO_CONFIGURATION = "argeo.node.repo.configuration";
	final static String REPO_DEFAULT_WORKSPACE = "argeo.node.repo.defaultWorkspace";
	final static String REPO_DBURL = "argeo.node.repo.dburl";
	final static String REPO_DBUSER = "argeo.node.repo.dbuser";
	final static String REPO_DBPASSWORD = "argeo.node.repo.dbpassword";
	final static String REPO_MAX_POOL_SIZE = "argeo.node.repo.maxPoolSize";
	final static String REPO_MAX_CACHE_MB = "argeo.node.repo.maxCacheMB";
	final static String REPO_BUNDLE_CACHE_MB = "argeo.node.repo.bundleCacheMB";
	final static String REPO_EXTRACTOR_POOL_SIZE = "argeo.node.repo.extractorPoolSize";
	final static String REPO_SEARCH_CACHE_SIZE = "argeo.node.repo.searchCacheSize";
	final static String REPO_MAX_VOLATILE_INDEX_SIZE = "argeo.node.repo.maxVolatileIndexSize";

	final static String TRANSACTIONS_HOME = "argeo.node.transactions.home";

	final static String I18N_DEFAULT_LOCALE = "argeo.i18n.defaultLocale";
	final static String I18N_LOCALES = "argeo.i18n.locales";

	// Node Security
	final static String ROLES_URI = "argeo.node.roles.uri";
	/** URI to an LDIF file or LDAP server used as initialization or backend */
	final static String USERADMIN_URIS = "argeo.node.useradmin.uris";
	final static String[] DEFAULT_CNDS = { "/org/argeo/jcr/argeo.cnd",
			"/org/argeo/cms/cms.cnd" };

	// Directories
	final static String DIR_NODE = "node";
	final static String DIR_TRANSACTIONS = "transactions";
	final static String DIR_PKI = "pki";
	final static String DIR_PKI_PRIVATE = DIR_PKI + "/private";

	// Security
	final static String DEFAULT_SECURITY_KEY = "argeo";
	final static String JAAS_CONFIG = "/org/argeo/cms/internal/kernel/jaas.cfg";
	final static String LOGIN_CONTEXT_KERNEL = "KERNEL";

	// DAV
	final static String WEBDAV_CONFIG = "/org/argeo/cms/internal/kernel/webdav-config.xml";
	final static String PATH_DATA = "/data";
	final static String WEBDAV_PUBLIC = PATH_DATA + "/public";
	final static String WEBDAV_PRIVATE = PATH_DATA + "/files";
	final static String REMOTING_PUBLIC = PATH_DATA + "/pub";
	final static String REMOTING_PRIVATE = PATH_DATA + "/jcr";

	// RWT / RAP
	final static String PATH_WORKBENCH = "/ui";
	final static String PATH_WORKBENCH_PUBLIC = PATH_WORKBENCH + "/public";

	final static String JETTY_FACTORY_PID = "org.eclipse.equinox.http.jetty.config"; //$NON-NLS-1$

}
