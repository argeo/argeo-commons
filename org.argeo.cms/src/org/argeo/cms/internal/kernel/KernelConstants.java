package org.argeo.cms.internal.kernel;

import org.argeo.node.NodeConstants;

public interface KernelConstants {
	String[] DEFAULT_CNDS = { "/org/argeo/jcr/argeo.cnd", "/org/argeo/cms/cms.cnd" };

	// Directories
	String DIR_NODE = "node";
	String DIR_REPOS = "repos";
	String DIR_TRANSACTIONS = "transactions";
	// String DIR_PKI = "pki";
	// String DIR_PKI_PRIVATE = DIR_PKI + "/private";

	// Files
	String DEPLOY_CONFIG_PATH = DIR_NODE + '/' + NodeConstants.DEPLOY_BASEDN + ".ldif";
	String DEFAULT_KEYSTORE_PATH = DIR_NODE + '/' + NodeConstants.NODE + ".p12";

	// Security
	// String DEFAULT_SECURITY_KEY = "argeo";
	String JAAS_CONFIG = "/org/argeo/cms/internal/kernel/jaas.cfg";
	String JAAS_CONFIG_IPA = "/org/argeo/cms/internal/kernel/jaas-ipa.cfg";
	// String LOGIN_CONTEXT_KERNEL = "KERNEL";
	// String LOGIN_CONTEXT_HARDENED_KERNEL = "HARDENED_KERNEL";

	// DAV
//	String WEBDAV_CONFIG = "/org/argeo/cms/internal/http/webdav-config.xml";
	// String PATH_DATA = "/data";
	// String WEBDAV_PUBLIC = PATH_DATA + "/public";
	// String WEBDAV_PRIVATE = PATH_DATA + "/files";
	// String REMOTING_PUBLIC = PATH_DATA + "/pub";
	// String REMOTING_PRIVATE = PATH_DATA + "/jcr";

	// Java
	String JAAS_CONFIG_PROP = "java.security.auth.login.config";

	// RWT / RAP
	String PATH_WORKBENCH = "/ui";
	String PATH_WORKBENCH_PUBLIC = PATH_WORKBENCH + "/public";

	String JETTY_FACTORY_PID = "org.eclipse.equinox.http.jetty.config";
	String WHITEBOARD_PATTERN_PROP = "osgi.http.whiteboard.servlet.pattern";

	// avoid dependencies
	String CONTEXT_NAME_PROP = "contextName";
	String JACKRABBIT_REPOSITORY_URI = "org.apache.jackrabbit.repository.uri";
}
