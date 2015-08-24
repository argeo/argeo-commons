package org.argeo.osgi.useradmin;

interface BasicTestConstants {
	final static String ROOT_USER_DN = "uid=root+cn=Super Admin,ou=People,dc=demo,dc=example,dc=org";
	final static String DEMO_USER_DN = "uid=demo,ou=People,dc=demo,dc=example,dc=org";
	final static String ADMIN_GROUP_DN = "cn=admin,ou=Roles,dc=demo,dc=example,dc=org";
	final static String EDITOR_GROUP_DN = "cn=editor,ou=Roles,dc=demo,dc=example,dc=org";
}
