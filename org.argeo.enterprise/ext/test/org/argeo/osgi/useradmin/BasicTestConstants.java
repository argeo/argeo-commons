package org.argeo.osgi.useradmin;

interface BasicTestConstants {
	String BASE_DN = "dc=example,dc=com";
	String ROOT_USER_DN = "uid=root,ou=users," + BASE_DN;
	String DEMO_USER_DN = "uid=demo,ou=users," + BASE_DN;
	String ADMIN_GROUP_DN = "cn=admin,ou=groups," + BASE_DN;
	String EDITORS_GROUP_DN = "cn=editors,ou=groups," + BASE_DN;
}
