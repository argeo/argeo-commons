package org.argeo.cms;

/** Public properties of the CMS Kernel */
public interface KernelHeader {
	// LOGIN CONTEXTS
	final static String LOGIN_CONTEXT_USER = "USER";
	final static String LOGIN_CONTEXT_ANONYMOUS = "ANONYMOUS";
	final static String LOGIN_CONTEXT_SYSTEM = "SYSTEM";
	final static String LOGIN_CONTEXT_SINGLE_USER = "SINGLE_USER";

	// RESERVED ROLES
	public final static String ROLE_ADMIN = "ROLE_ADMIN";
	public final static String ROLE_GROUP_ADMIN = "ROLE_GROUP_ADMIN";
	public final static String ROLE_USER_ADMIN = "ROLE_USER_ADMIN";
	public final static String ROLE_USER = "ROLE_USER";
	public final static String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";

	// RESERVED USERNAMES
	public final static String USERNAME_ADMIN = "root";
	public final static String USERNAME_DEMO = "demo";
	public final static String USERNAME_ANONYMOUS = "anonymous";
}
