package org.argeo.node;

import javax.jcr.Repository;

public interface NodeConstants {
	/*
	 * PIDs
	 */
	String NODE_STATE_PID = "org.argeo.node.state";
	String NODE_DEPLOYMENT_PID = "org.argeo.node.deployment";
	String NODE_INSTANCE_PID = "org.argeo.node.instance";

	String NODE_USER_ADMIN_PID = "org.argeo.node.userAdmin";
	String NODE_KEYRING_PID = "org.argeo.node.keyring";

	/*
	 * FACTORY PIDs
	 */
	String NODE_REPOS_FACTORY_PID = "org.argeo.node.repos";
	String NODE_USER_DIRECTORIES_FACTORY_PID = "org.argeo.node.userDirectories";

	/*
	 * DEPLOY
	 */
	String DEPLOY_BASEDN = "ou=deploy,ou=node";

	/*
	 * FRAMEWORK PROPERTIES
	 */
	String NODE_INIT = "argeo.node.init";
	String I18N_DEFAULT_LOCALE = "argeo.i18n.defaultLocale";
	String I18N_LOCALES = "argeo.i18n.locales";
	// Node Security
	String ROLES_URI = "argeo.node.roles.uri";
	/** URI to an LDIF file or LDAP server used as initialization or backend */
	String USERADMIN_URIS = "argeo.node.useradmin.uris";
	// Node
	/** Properties configuring the node repository */
	String NODE_REPO_PROP_PREFIX = "argeo.node.repo.";

	/*
	 * STANDARD ATTRIBUTES
	 */
	String CN = "cn";
	String OU = "ou";
	String URI = "uri";

	/*
	 * STANDARD VALUES
	 */
	String DEFAULT = "default";

	/*
	 * RESERVED ROLES
	 */
	String ROLES_BASEDN = "ou=roles,ou=node";
	String ROLE_ADMIN = "cn=admin," + ROLES_BASEDN;
	String ROLE_USER_ADMIN = "cn=userAdmin," + ROLES_BASEDN;
	// Special system groups that cannot be edited:
	// user U anonymous = everyone
	String ROLE_USER = "cn=user," + ROLES_BASEDN;
	String ROLE_ANONYMOUS = "cn=anonymous," + ROLES_BASEDN;

	/*
	 * LOGIN CONTEXTS
	 */
	String LOGIN_CONTEXT_USER = "USER";
	String LOGIN_CONTEXT_ANONYMOUS = "ANONYMOUS";
	String LOGIN_CONTEXT_DATA_ADMIN = "DATA_ADMIN";
	String LOGIN_CONTEXT_SINGLE_USER = "SINGLE_USER";

	/*
	 * LEGACY
	 */
	String ARGEO_BASE_PATH = "/argeo:system";
	String PEOPLE_BASE_PATH = NodeConstants.ARGEO_BASE_PATH + "/argeo:people";
	String DATA_MODELS_BASE_PATH = NodeConstants.ARGEO_BASE_PATH + "/argeo:dataModels";
	String ALIAS_HOME = "home";
	// standard aliases
	/**
	 * Reserved alias for the "node" {@link Repository}, that is, the default
	 * JCR repository.
	 */
	String ALIAS_NODE = "node";
	/** Key for a JCR repository URI */
	String JCR_REPOSITORY_URI = "argeo.jcr.repository.uri";
	// parameters (typically for call to a RepositoryFactory)
	/** Key for a JCR repository alias */
	String JCR_REPOSITORY_ALIAS = "argeo.jcr.repository.alias";
}
