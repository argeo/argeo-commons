package org.argeo.cms.internal.kernel;

public interface KernelConstants {
	// (internal) API
	public final static String SPRING_SECURITY_CONTEXT_KEY = "SPRING_SECURITY_CONTEXT";

	// Node
	final static String REPO_HOME = "argeo.node.repo.home";
	final static String REPO_CONFIGURATION = "argeo.node.repo.configuration";
	final static String REPO_DEFAULT_WORKSPACE = "argeo.node.repo.defaultWorkspace";
	final static String REPO_DBURL = "argeo.node.repo.dburl";
	final static String REPO_DBUSER = "argeo.node.repo.dbuser";
	final static String REPO_DBPASSWORD = "argeo.node.repo.dbpassword";
	final static String REPO_MAX_POOL_SIZE = "argeo.node.repo.maxPoolSize";

	final static String[] DEFAULT_CNDS = { "/org/argeo/jcr/argeo.cnd",
			"/org/argeo/cms/cms.cnd" };

	// Security
	final static String DEFAULT_SECURITY_KEY = "argeo";
	final static String ANONYMOUS_USER = "anonymous";
	final static String ADMIN_USER = "root";
	final static String JAAS_CONFIG = "/org/argeo/cms/internal/kernel/jaas.cfg";

	// Roles
	final static String ROLE_USER = "ROLE_USER";
	final static String ROLE_ADMIN = "ROLE_ADMIN";
	final static String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";

	// DAV
	final static String WEBDAV_CONFIG = "/org/argeo/cms/internal/kernel/webdav-config.xml";
	final static String PATH_WEBDAV_PUBLIC = "/data/public";
	final static String PATH_WEBDAV_PRIVATE = "/data/files";
	final static String PATH_REMOTING_PUBLIC = "/data/pub";
	final static String PATH_REMOTING_PRIVATE = "/data/jcr";
	final static String PATH_WORKBENCH_PUBLIC = "/ui/public";

}
