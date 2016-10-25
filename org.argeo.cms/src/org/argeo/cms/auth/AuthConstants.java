package org.argeo.cms.auth;

import org.argeo.node.NodeConstants;

/** Public properties of the CMS Kernel */
@Deprecated
interface AuthConstants {
	// LOGIN CONTEXTS
	/**
	 * @deprecated Use {@link NodeConstants#LOGIN_CONTEXT_USER} instead
	 */
	final static String LOGIN_CONTEXT_USER = NodeConstants.LOGIN_CONTEXT_USER;
	/**
	 * @deprecated Use {@link NodeConstants#LOGIN_CONTEXT_ANONYMOUS} instead
	 */
//	final static String LOGIN_CONTEXT_ANONYMOUS = NodeConstants.LOGIN_CONTEXT_ANONYMOUS;
	/**
	 * @deprecated Use {@link NodeConstants#LOGIN_CONTEXT_DATA_ADMIN} instead
	 */
	final static String LOGIN_CONTEXT_DATA_ADMIN = NodeConstants.LOGIN_CONTEXT_DATA_ADMIN;
	/**
	 * @deprecated Use {@link NodeConstants#LOGIN_CONTEXT_SINGLE_USER} instead
	 */
	final static String LOGIN_CONTEXT_SINGLE_USER = NodeConstants.LOGIN_CONTEXT_SINGLE_USER;

	// RESERVED ROLES
	// public final static String ROLE_KERNEL = "OU=node";
	/**
	 * @deprecated Use {@link NodeConstants#ROLES_BASEDN} instead
	 */
	public final static String ROLES_BASEDN = NodeConstants.ROLES_BASEDN;
	/**
	 * @deprecated Use {@link NodeConstants#ROLE_ADMIN} instead
	 */
	public final static String ROLE_ADMIN = NodeConstants.ROLE_ADMIN;
	public final static String ROLE_GROUP_ADMIN = "cn=groupAdmin," + NodeConstants.ROLES_BASEDN;
	/**
	 * @deprecated Use {@link NodeConstants#ROLE_USER_ADMIN} instead
	 */
	public final static String ROLE_USER_ADMIN = NodeConstants.ROLE_USER_ADMIN;
	// Special system groups that cannot be edited:
	// user U anonymous = everyone
	/**
	 * @deprecated Use {@link NodeConstants#ROLE_USER} instead
	 */
	public final static String ROLE_USER = NodeConstants.ROLE_USER;
	/**
	 * @deprecated Use {@link NodeConstants#ROLE_ANONYMOUS} instead
	 */
	public final static String ROLE_ANONYMOUS = NodeConstants.ROLE_ANONYMOUS;

}
