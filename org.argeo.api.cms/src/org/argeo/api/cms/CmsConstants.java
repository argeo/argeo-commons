package org.argeo.api.cms;

public interface CmsConstants {
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

	/*
	 * JCR CONVENTIONS
	 */
	String NODE_REPOSITORY = NODE;
	String EGO_REPOSITORY = "ego";
	String SYS_WORKSPACE = "sys";
	String HOME_WORKSPACE = "home";
	String SRV_WORKSPACE = "srv";
	String GUESTS_WORKSPACE = "guests";
	String PUBLIC_WORKSPACE = "public";
	String SECURITY_WORKSPACE = "security";

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
	 * COMPONENT PROPERTIES
	 */
	String CONTEXT_PATH = "context.path";
	String CONTEXT_PUBLIC = "context.public";
	String EVENT_TOPICS = "event.topics";
	String ACR_MOUNT_PATH = "acr.mount.path";


	/*
	 * FILE SYSTEM
	 */
	String CMS_FS_SCHEME = "cms";

	/*
	 * PIDs
	 */
	String NODE_STATE_PID = "org.argeo.api.state";
	String NODE_DEPLOYMENT_PID = "org.argeo.api.deployment";
	String NODE_INSTANCE_PID = "org.argeo.api.instance";

	String NODE_KEYRING_PID = "org.argeo.api.keyring";
	String NODE_FS_PROVIDER_PID = "org.argeo.api.fsProvider";

	/*
	 * FACTORY PIDs
	 */
	String NODE_REPOS_FACTORY_PID = "org.argeo.api.repos";
	String NODE_USER_ADMIN_PID = "org.argeo.api.userAdmin";
}
