package org.argeo.osgi.useradmin;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import junit.framework.TestCase;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

public class LdifParserTest extends TestCase {
	public void testSimpleLdif() throws Exception {
		LdifParser ldifParser = new LdifParser();
		SortedMap<LdapName, Attributes> res = ldifParser.read(getClass()
				.getResourceAsStream("test.ldif"));
		LdapName rootDn = new LdapName(
				"uid=root,ou=People,dc=demo,dc=example,dc=org");
		Attributes rootAttributes = res.get(rootDn);
		assertNotNull(rootAttributes);
		assertEquals("Superuser", rootAttributes.get("description").get());
		byte[] rawPwEntry = (byte[]) rootAttributes.get("userpassword").get();
		assertEquals("{SHA}ieSV55Qc+eQOaYDRSha/AjzNTJE=",
				new String(rawPwEntry));
		byte[] hashedPassword = DigestUtils.sha1("demo".getBytes());
		assertEquals("{SHA}" + Base64.encodeBase64String(hashedPassword),
				new String(rawPwEntry));

		LdapName adminDn = new LdapName(
				"cn=admin,ou=Roles,dc=demo,dc=example,dc=org");
		Attributes adminAttributes = res.get(adminDn);
		assertNotNull(adminAttributes);
		Attribute memberAttribute = adminAttributes.get("member");
		assertNotNull(memberAttribute);
		NamingEnumeration<?> members = memberAttribute.getAll();
		List<String> users = new ArrayList<String>();
		while (members.hasMore()) {
			Object value = members.next();
			users.add(value.toString());
		}
		assertEquals(1, users.size());
		assertEquals(rootDn, new LdapName(users.get(0)));
	}
}
