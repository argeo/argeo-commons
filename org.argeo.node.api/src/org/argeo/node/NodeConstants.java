package org.argeo.node;

public interface NodeConstants {
	/*
	 * DN ATTRIBUTES (RFC 4514)
	 */
	String CN = "cn";
	String L = "l";
	String ST = "st";
	String O = "o";
	String OU = "ou";
	String C = "c";
	String STREET = "street";
	String DC = "dc";
	String UID = "uid";

	/*
	 * STANDARD ATTRIBUTES
	 */
	String LABELED_URI = "labeledUri";

	/*
	 * COMMON NAMES
	 */
	String NODE = "node";
	String HOME = "home";
	String GROUPS = "groups";
	String GUESTS = "guests";
	String PUBLIC = "public";

	/*
	 * BASE DNs
	 */
	String DEPLOY_BASEDN = "ou=deploy,ou=node";

	/*
	 * STANDARD VALUES
	 */
	String DEFAULT = "default";

	/*
	 * RESERVED ROLES
	 */
	String ROLES_BASEDN = "ou=roles,ou=node";
	String TOKENS_BASEDN = "ou=tokens,ou=node";
	String ROLE_ADMIN = "cn=admin," + ROLES_BASEDN;
	String ROLE_USER_ADMIN = "cn=userAdmin," + ROLES_BASEDN;
	String ROLE_DATA_ADMIN = "cn=dataAdmin," + ROLES_BASEDN;
	// Special system groups that cannot be edited:
	// user U anonymous = everyone
	String ROLE_USER = "cn=user," + ROLES_BASEDN;
	String ROLE_ANONYMOUS = "cn=anonymous," + ROLES_BASEDN;
	// Account lifecycle
	String ROLE_REGISTERING = "cn=registering," + ROLES_BASEDN;

	/*
	 * LOGIN CONTEXTS
	 */
	String LOGIN_CONTEXT_NODE = "NODE";
	String LOGIN_CONTEXT_USER = "USER";
	String LOGIN_CONTEXT_ANONYMOUS = "ANONYMOUS";
	String LOGIN_CONTEXT_DATA_ADMIN = "DATA_ADMIN";
	String LOGIN_CONTEXT_SINGLE_USER = "SINGLE_USER";
	String LOGIN_CONTEXT_KEYRING = "KEYRING";

	/*
	 * PATHS
	 */
	String PATH_DATA = "/data";
	String PATH_JCR = "/jcr";
	String PATH_FILES = "/files";
	// String PATH_JCR_PUB = "/pub";

	/*
	 * FILE SYSTEMS
	 */
	String SCHEME_NODE = NODE;

	/*
	 * KERBEROS
	 */
	String NODE_SERVICE = NODE;

	/*
	 * INIT FRAMEWORK PROPERTIES
	 */
	String NODE_INIT = "argeo.node.init";
	String I18N_DEFAULT_LOCALE = "argeo.i18n.defaultLocale";
	String I18N_LOCALES = "argeo.i18n.locales";
	// Node Security
	String ROLES_URI = "argeo.node.roles.uri";
	String TOKENS_URI = "argeo.node.tokens.uri";
	/** URI to an LDIF file or LDAP server used as initialization or backend */
	String USERADMIN_URIS = "argeo.node.useradmin.uris";
	// Transaction manager
	String TRANSACTION_MANAGER = "argeo.node.transaction.manager";
	String TRANSACTION_MANAGER_SIMPLE = "simple";
	String TRANSACTION_MANAGER_BITRONIX = "bitronix";
	// Node
	/** Properties configuring the node repository */
	String NODE_REPO_PROP_PREFIX = "argeo.node.repo.";
	/** Additional standalone repositories, related to data models. */
	String NODE_REPOS_PROP_PREFIX = "argeo.node.repos.";
	// HTTP
	String HTTP_PORT = "org.osgi.service.http.port";
	String HTTP_PORT_SECURE = "org.osgi.service.http.port.secure";
	/**
	 * The HTTP header used to convey the DN of a client verified by a reverse
	 * proxy. Typically SSL_CLIENT_S_DN for Apache.
	 */
	String HTTP_PROXY_SSL_DN = "argeo.http.proxy.ssl.dn";

	/*
	 * PIDs
	 */
	String NODE_STATE_PID = "org.argeo.node.state";
	String NODE_DEPLOYMENT_PID = "org.argeo.node.deployment";
	String NODE_INSTANCE_PID = "org.argeo.node.instance";

	String NODE_KEYRING_PID = "org.argeo.node.keyring";
	String NODE_FS_PROVIDER_PID = "org.argeo.node.fsProvider";

	/*
	 * FACTORY PIDs
	 */
	String NODE_REPOS_FACTORY_PID = "org.argeo.node.repos";
	String NODE_USER_ADMIN_PID = "org.argeo.node.userAdmin";
}
