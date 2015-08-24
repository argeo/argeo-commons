package org.argeo.osgi.useradmin;

import junit.framework.TestCase;

import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

public class LdifUserAdminTest extends TestCase implements BasicTestConstants {

	public void testBasicUserAdmin() {
		LdifUserAdmin userAdmin = new LdifUserAdmin(getClass()
				.getResourceAsStream("basic.ldif"));
		User rootUser = (User) userAdmin.getRole(ROOT_USER_DN);
		assertNotNull(rootUser);
		Group adminGroup = (Group) userAdmin.getRole(ADMIN_GROUP_DN);
		assertNotNull(adminGroup);
		Role[] members = adminGroup.getMembers();
		assertEquals(1, members.length);
		assertEquals(rootUser.getName(), members[0].getName());
	}
}
