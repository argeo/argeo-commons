package org.argeo.osgi.useradmin;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

public class LdifUserAdminTest extends TestCase implements BasicTestConstants {

	public void testBasicUserAdmin() throws Exception {
		LdifUserAdmin userAdmin = new LdifUserAdmin(getClass()
				.getResourceAsStream("basic.ldif"));

		// users
		User rootUser = (User) userAdmin.getRole(ROOT_USER_DN);
		assertNotNull(rootUser);
		User demoUser = (User) userAdmin.getRole(DEMO_USER_DN);
		assertNotNull(demoUser);

		// groups
		Group adminGroup = (Group) userAdmin.getRole(ADMIN_GROUP_DN);
		assertNotNull(adminGroup);
		Role[] members = adminGroup.getMembers();
		assertEquals(1, members.length);
		assertEquals(rootUser, members[0]);

		Group editorGroup = (Group) userAdmin.getRole(EDITOR_GROUP_DN);
		assertNotNull(editorGroup);
		members = editorGroup.getMembers();
		assertEquals(2, members.length);
		assertEquals(adminGroup, members[0]);
		assertEquals(demoUser, members[1]);

		Authorization rootAuth = userAdmin.getAuthorization(rootUser);
		List<String> rootRoles = Arrays.asList(rootAuth.getRoles());
		assertEquals(3, rootRoles.size());
		assertTrue(rootRoles.contains(ROOT_USER_DN));
		assertTrue(rootRoles.contains(ADMIN_GROUP_DN));
		assertTrue(rootRoles.contains(EDITOR_GROUP_DN));

		// properties
		assertEquals("root@localhost", rootUser.getProperties().get("mail"));

		// credentials
		byte[] hashedPassword = ("{SHA}" + Base64
				.encodeBase64String(DigestUtils.sha1("demo".getBytes())))
				.getBytes();
		assertTrue(rootUser.hasCredential("userpassword", hashedPassword));
		assertTrue(demoUser.hasCredential("userpassword", hashedPassword));
		
		// search
		Role[] search = userAdmin.getRoles(null);
		assertEquals(4, search.length);
		search = userAdmin.getRoles("(objectClass=groupOfNames)");
		assertEquals(2, search.length);
		search = userAdmin.getRoles("(objectclass=inetOrgPerson)");
		assertEquals(2, search.length);
		search = userAdmin.getRoles("(&(objectclass=inetOrgPerson)(uid=demo))");
		assertEquals(1, search.length);
	}
}
