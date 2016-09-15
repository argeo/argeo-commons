package org.argeo.osgi.useradmin;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.SortedMap;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import junit.framework.TestCase;

import org.argeo.naming.LdifParser;

public class LdifParserTest extends TestCase implements BasicTestConstants {
	public void testBasicLdif() throws Exception {
		LdifParser ldifParser = new LdifParser();
		SortedMap<LdapName, Attributes> res = ldifParser.read(getClass()
				.getResourceAsStream("basic.ldif"));
		LdapName rootDn = new LdapName(ROOT_USER_DN);
		Attributes rootAttributes = res.get(rootDn);
		assertNotNull(rootAttributes);
		assertEquals("Superuser",
				rootAttributes.get(LdifName.description.name()).get());
		byte[] rawPwEntry = (byte[]) rootAttributes.get(
				LdifName.userPassword.name()).get();
		assertEquals("{SHA}ieSV55Qc+eQOaYDRSha/AjzNTJE=",
				new String(rawPwEntry));
		byte[] hashedPassword = DigestUtils.sha1("demo".getBytes());
		assertEquals("{SHA}" + Base64.getEncoder().encodeToString(hashedPassword),
				new String(rawPwEntry));

		LdapName adminDn = new LdapName(ADMIN_GROUP_DN);
		Attributes adminAttributes = res.get(adminDn);
		assertNotNull(adminAttributes);
		Attribute memberAttribute = adminAttributes.get(LdifName.member.name());
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
