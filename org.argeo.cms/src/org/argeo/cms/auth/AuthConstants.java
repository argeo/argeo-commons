package org.argeo.cms.auth;

/** Public properties of the CMS Kernel */
public interface AuthConstants {
	// LOGIN CONTEXTS
	final static String LOGIN_CONTEXT_USER = "USER";
	final static String LOGIN_CONTEXT_ANONYMOUS = "ANONYMOUS";
	final static String LOGIN_CONTEXT_SYSTEM = "SYSTEM";
	final static String LOGIN_CONTEXT_SINGLE_USER = "SINGLE_USER";

	// HTTP
	final static String ACCESS_CONTROL_CONTEXT = "org.argeo.node.accessControlContext";

	// RESERVED ROLES
	public final static String ROLE_KERNEL = "OU=node";
	public final static String ROLES_BASEDN = "ou=roles,ou=node";
	public final static String ROLE_ADMIN = "cn=admin," + ROLES_BASEDN;
	public final static String ROLE_GROUP_ADMIN = "cn=groupAdmin,"
			+ ROLES_BASEDN;
	public final static String ROLE_USER_ADMIN = "cn=userAdmin," + ROLES_BASEDN;
	// Special system groups that cannot be edited:
	// user U anonymous = everyone
	public final static String ROLE_USER = "cn=user," + ROLES_BASEDN;
	public final static String ROLE_ANONYMOUS = "cn=anonymous," + ROLES_BASEDN;

	// SHARED STATE KEYS
	public final static String BUNDLE_CONTEXT_KEY = "org.argeo.security.bundleContext";
	public final static String AUTHORIZATION_KEY = "org.argeo.security.authorization";
}
