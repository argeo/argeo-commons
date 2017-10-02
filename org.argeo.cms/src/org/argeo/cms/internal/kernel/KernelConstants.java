package org.argeo.cms.internal.kernel;

import org.argeo.node.NodeConstants;

public interface KernelConstants {
	String[] DEFAULT_CNDS = { "/org/argeo/jcr/argeo.cnd", "/org/argeo/cms/cms.cnd" };

	// Directories
	String DIR_NODE = "node";
	String DIR_REPOS = "repos";
	String DIR_INDEXES = "indexes";
	String DIR_TRANSACTIONS = "transactions";

	// Files
	String DEPLOY_CONFIG_PATH = DIR_NODE + '/' + NodeConstants.DEPLOY_BASEDN + ".ldif";
	String DEFAULT_KEYSTORE_PATH = DIR_NODE + '/' + NodeConstants.NODE + ".p12";
	String NODE_KEY_TAB_PATH = DIR_NODE + "/krb5.keytab";

	// Security
	String JAAS_CONFIG = "/org/argeo/cms/internal/kernel/jaas.cfg";
	String JAAS_CONFIG_NOIPA = "/org/argeo/cms/internal/kernel/jaas-noipa.cfg";

	// Java
	String JAAS_CONFIG_PROP = "java.security.auth.login.config";

	// DEFAULTS JCR PATH
	String DEFAULT_HOME_BASE_PATH = "/home";
	String DEFAULT_USERS_BASE_PATH = "/users";
	String DEFAULT_GROUPS_BASE_PATH = "/groups";

	// RWT / RAP
	// String PATH_WORKBENCH = "/ui";
	// String PATH_WORKBENCH_PUBLIC = PATH_WORKBENCH + "/public";

	String JETTY_FACTORY_PID = "org.eclipse.equinox.http.jetty.config";
	String WHITEBOARD_PATTERN_PROP = "osgi.http.whiteboard.servlet.pattern";

	// avoid dependencies
	String CONTEXT_NAME_PROP = "contextName";
	String JACKRABBIT_REPOSITORY_URI = "org.apache.jackrabbit.repository.uri";
}
