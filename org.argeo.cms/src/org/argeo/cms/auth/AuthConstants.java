package org.argeo.cms.auth;

import org.osgi.service.http.HttpContext;

/** Public properties of the CMS Kernel */
public interface AuthConstants {
	// LOGIN CONTEXTS
	final static String LOGIN_CONTEXT_USER = "USER";
	final static String LOGIN_CONTEXT_ANONYMOUS = "ANONYMOUS";
	final static String LOGIN_CONTEXT_DATA_ADMIN = "DATA_ADMIN";
	final static String LOGIN_CONTEXT_SINGLE_USER = "SINGLE_USER";

	// RESERVED ROLES
	public final static String ROLE_KERNEL = "OU=node";
	public final static String ROLES_BASEDN = "ou=roles,ou=node";
	public final static String ROLE_ADMIN = "cn=admin," + ROLES_BASEDN;
	public final static String ROLE_GROUP_ADMIN = "cn=groupAdmin," + ROLES_BASEDN;
	public final static String ROLE_USER_ADMIN = "cn=userAdmin," + ROLES_BASEDN;
	// Special system groups that cannot be edited:
	// user U anonymous = everyone
	public final static String ROLE_USER = "cn=user," + ROLES_BASEDN;
	public final static String ROLE_ANONYMOUS = "cn=anonymous," + ROLES_BASEDN;

	// SHARED STATE KEYS
	// compatible with com.sun.security.auth.module.*LoginModule
	public static final String SHARED_STATE_USERNAME = "javax.security.auth.login.name";
	public static final String SHARED_STATE_PASSWORD = "javax.security.auth.login.password";
	public static final String SHARED_STATE_AUTHORIZATION = HttpContext.AUTHORIZATION;

}
