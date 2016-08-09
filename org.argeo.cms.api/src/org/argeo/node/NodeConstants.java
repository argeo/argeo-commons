package org.argeo.node;

public interface NodeConstants {
	/*
	 * PIDs
	 */
	String NODE_STATE_PID = "org.argeo.node.state";
	String NODE_DEPLOYMENT_PID = "org.argeo.node.deployment";
	String NODE_INSTANCE_PID = "org.argeo.node.instance";

	String NODE_REPO_PID = "org.argeo.node.repo";
	String NODE_USER_ADMIN_PID = "org.argeo.node.userAdmin";

	/*
	 * FACTORY PIDs
	 */
	String JACKRABBIT_FACTORY_PID = "org.argeo.jackrabbit.config";

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
	String LABELED_URI = "labeledUri";
}
