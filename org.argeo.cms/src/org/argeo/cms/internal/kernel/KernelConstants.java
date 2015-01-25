package org.argeo.cms.internal.kernel;

interface KernelConstants {
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
}
