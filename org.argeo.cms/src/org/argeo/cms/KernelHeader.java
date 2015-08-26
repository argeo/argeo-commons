package org.argeo.cms;

/** Public properties of the CMS Kernel */
public interface KernelHeader {
	// LOGIN CONTEXTS
	final static String LOGIN_CONTEXT_USER = "USER";
	final static String LOGIN_CONTEXT_ANONYMOUS = "ANONYMOUS";
	final static String LOGIN_CONTEXT_SYSTEM = "SYSTEM";
	final static String LOGIN_CONTEXT_SINGLE_USER = "SINGLE_USER";

	// RESERVED ROLES
	public final static String ROLE_ADMIN = "cn=admin,ou=system,ou=node";
	public final static String ROLE_GROUP_ADMIN = "cn=groupAdmin,ou=system,ou=node";
	public final static String ROLE_USER_ADMIN = "cn=userAdmin,ou=system,ou=node";
	// Special system groups that cannot be edited:
	// user U anonymous = everyone
	public final static String ROLE_USER = "cn=user,ou=system,ou=node";
	public final static String ROLE_ANONYMOUS = "cn=anonymous,ou=system,ou=node";

	// RESERVED USERNAMES
	public final static String USERNAME_ADMIN = "root";
	public final static String USERNAME_DEMO = "demo";
	@Deprecated
	public final static String USERNAME_ANONYMOUS = "anonymous";
}
