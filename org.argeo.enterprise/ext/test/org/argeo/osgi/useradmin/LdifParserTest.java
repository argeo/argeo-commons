package org.argeo.osgi.useradmin;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.SortedMap;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;

import org.argeo.naming.LdapAttrs;
import org.argeo.naming.LdifParser;

/** {@link LdifParser} tests. */
public class LdifParserTest implements BasicTestConstants {
	public void testBasicLdif() throws Exception {
		LdifParser ldifParser = new LdifParser();
		SortedMap<LdapName, Attributes> res = ldifParser.read(getClass().getResourceAsStream("basic.ldif"));
		LdapName rootDn = new LdapName(ROOT_USER_DN);
		Attributes rootAttributes = res.get(rootDn);
		assert rootAttributes != null;
		assert "Superuser".equals(rootAttributes.get(LdapAttrs.description.name()).get());
		byte[] rawPwEntry = (byte[]) rootAttributes.get(LdapAttrs.userPassword.name()).get();
		assert "{SHA}ieSV55Qc+eQOaYDRSha/AjzNTJE=".contentEquals(new String(rawPwEntry));
		byte[] hashedPassword = DigestUtils.sha1("demo".getBytes());
		assert ("{SHA}" + Base64.getEncoder().encodeToString(hashedPassword)).equals(new String(rawPwEntry));

		LdapName adminDn = new LdapName(ADMIN_GROUP_DN);
		Attributes adminAttributes = res.get(adminDn);
		assert adminAttributes != null;
		Attribute memberAttribute = adminAttributes.get(LdapAttrs.member.name());
		assert memberAttribute != null;
		NamingEnumeration<?> members = memberAttribute.getAll();
		List<String> users = new ArrayList<String>();
		while (members.hasMore()) {
			Object value = members.next();
			users.add(value.toString());
		}
		assert 1 == users.size();
		assert rootDn.equals(new LdapName(users.get(0)));
	}
}
