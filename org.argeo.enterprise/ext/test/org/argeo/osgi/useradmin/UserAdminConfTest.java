package org.argeo.osgi.useradmin;

import static org.argeo.osgi.useradmin.UserAdminConf.propertiesAsUri;
import static org.argeo.osgi.useradmin.UserAdminConf.uriAsProperties;

import java.net.URI;
import java.util.Dictionary;

/** {@link UserAdminConf} tests. */
public class UserAdminConfTest {
	public void testUriFormat() throws Exception {
		// LDAP
		URI uriIn = new URI("ldap://" + "uid=admin,ou=system:secret@localhost:10389" + "/dc=example,dc=com"
				+ "?readOnly=false&userObjectClass=person");
		Dictionary<String, ?> props = uriAsProperties(uriIn.toString());
		System.out.println(props);
		assert "dc=example,dc=com".equals(props.get(UserAdminConf.baseDn.name()));
		assert "false".equals(props.get(UserAdminConf.readOnly.name()));
		assert "person".equals(props.get(UserAdminConf.userObjectClass.name()));
		URI uriOut = propertiesAsUri(props);
		System.out.println(uriOut);
		assert "/dc=example,dc=com?userObjectClass=person&readOnly=false".equals(uriOut.toString());

		// File
		uriIn = new URI("file://some/dir/dc=example,dc=com.ldif");
		props = uriAsProperties(uriIn.toString());
		System.out.println(props);
		assert "dc=example,dc=com".equals(props.get(UserAdminConf.baseDn.name()));

		// Base configuration
		uriIn = new URI("/dc=example,dc=com.ldif?readOnly=true&userBase=ou=CoWorkers,ou=People&groupBase=ou=Roles");
		props = uriAsProperties(uriIn.toString());
		System.out.println(props);
		assert "dc=example,dc=com".equals(props.get(UserAdminConf.baseDn.name()));
		assert "true".equals(props.get(UserAdminConf.readOnly.name()));
		assert "ou=CoWorkers,ou=People".equals(props.get(UserAdminConf.userBase.name()));
		assert "ou=Roles".equals(props.get(UserAdminConf.groupBase.name()));
		uriOut = propertiesAsUri(props);
		System.out.println(uriOut);
		assert "/dc=example,dc=com?userBase=ou=CoWorkers,ou=People&groupBase=ou=Roles&readOnly=true"
				.equals(uriOut.toString());

		// OS
		uriIn = new URI("os:///dc=example,dc=com");
		props = uriAsProperties(uriIn.toString());
		System.out.println(props);
		assert "dc=example,dc=com".equals(props.get(UserAdminConf.baseDn.name()));
		assert "true".equals(props.get(UserAdminConf.readOnly.name()));
		uriOut = propertiesAsUri(props);
		System.out.println(uriOut);
		assert "/dc=example,dc=com?readOnly=true".equals(uriOut.toString());
	}
}
