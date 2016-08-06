package org.argeo.cms.internal.kernel;

public interface KernelConstants {
	

	//final static String TRANSACTIONS_HOME = "argeo.node.transactions.home";

	final static String[] DEFAULT_CNDS = { "/org/argeo/jcr/argeo.cnd", "/org/argeo/cms/cms.cnd" };

	// Directories
	final static String DIR_NODE = "node";
	final static String DIR_TRANSACTIONS = "transactions";
	final static String DIR_PKI = "pki";
	final static String DIR_PKI_PRIVATE = DIR_PKI + "/private";

	// Security
	final static String DEFAULT_SECURITY_KEY = "argeo";
	final static String JAAS_CONFIG = "/org/argeo/cms/internal/kernel/jaas.cfg";
	final static String LOGIN_CONTEXT_KERNEL = "KERNEL";
	final static String LOGIN_CONTEXT_HARDENED_KERNEL = "HARDENED_KERNEL";

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
