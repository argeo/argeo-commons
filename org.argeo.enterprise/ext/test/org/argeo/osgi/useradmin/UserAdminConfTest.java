package org.argeo.osgi.useradmin;

import static org.argeo.osgi.useradmin.UserAdminConf.propertiesAsUri;
import static org.argeo.osgi.useradmin.UserAdminConf.uriAsProperties;

import java.net.URI;
import java.util.Dictionary;

import junit.framework.TestCase;

public class UserAdminConfTest extends TestCase {
	public void testUriFormat() throws Exception {
		// LDAP
		URI uriIn = new URI("ldap://" + "uid=admin,ou=system:secret@localhost:10389" + "/dc=example,dc=com"
				+ "?readOnly=false&userObjectClass=person");
		Dictionary<String, ?> props = uriAsProperties(uriIn.toString());
		System.out.println(props);
		assertEquals("dc=example,dc=com", props.get(UserAdminConf.baseDn.name()));
		assertEquals("false", props.get(UserAdminConf.readOnly.name()));
		assertEquals("person", props.get(UserAdminConf.userObjectClass.name()));
		URI uriOut = propertiesAsUri(props);
		System.out.println(uriOut);
		assertEquals("/dc=example,dc=com?userObjectClass=person&readOnly=false", uriOut.toString());

		// File
		uriIn = new URI("file://some/dir/dc=example,dc=com.ldif");
		props = uriAsProperties(uriIn.toString());
		System.out.println(props);
		assertEquals("dc=example,dc=com", props.get(UserAdminConf.baseDn.name()));

		// Base configuration
		uriIn = new URI("/dc=example,dc=com.ldif?readOnly=true&userBase=ou=CoWorkers,ou=People&groupBase=ou=Roles");
		props = uriAsProperties(uriIn.toString());
		System.out.println(props);
		assertEquals("dc=example,dc=com", props.get(UserAdminConf.baseDn.name()));
		assertEquals("true", props.get(UserAdminConf.readOnly.name()));
		assertEquals("ou=CoWorkers,ou=People", props.get(UserAdminConf.userBase.name()));
		assertEquals("ou=Roles", props.get(UserAdminConf.groupBase.name()));
		uriOut = propertiesAsUri(props);
		System.out.println(uriOut);
		assertEquals("/dc=example,dc=com?userBase=ou=CoWorkers,ou=People&groupBase=ou=Roles&readOnly=true", uriOut.toString());

		// OS
		uriIn = new URI("os:///dc=example,dc=com");
		props = uriAsProperties(uriIn.toString());
		System.out.println(props);
		assertEquals("dc=example,dc=com", props.get(UserAdminConf.baseDn.name()));
		assertEquals("true", props.get(UserAdminConf.readOnly.name()));
		uriOut = propertiesAsUri(props);
		System.out.println(uriOut);
		assertEquals("/dc=example,dc=com?readOnly=true", uriOut.toString());
	}
}
