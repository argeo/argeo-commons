package org.argeo.osgi.useradmin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Base64;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import javax.transaction.TransactionManager;

import org.argeo.naming.LdapAttrs;
import org.argeo.transaction.simple.SimpleTransactionManager;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.ehcache.EhCacheXAResourceProducer;
import junit.framework.TestCase;

public class LdifUserAdminTest extends TestCase implements BasicTestConstants {
	final static int TM_SIMPLE = 0;
	final static int TM_BITRONIX = 1;

	private int tmType = TM_SIMPLE;
	private TransactionManager tm;
	private URI uri;
	private AbstractUserDirectory userAdmin;
	private Path tempDir;

	// public void testConcurrent() throws Exception {
	// }

	@SuppressWarnings("unchecked")
	public void testEdition() throws Exception {
		User demoUser = (User) userAdmin.getRole(DEMO_USER_DN);
		assertNotNull(demoUser);

		tm.begin();
		String newName = "demo";
		demoUser.getProperties().put("cn", newName);
		assertEquals(newName, demoUser.getProperties().get("cn"));
		tm.commit();
		persistAndRestart();
		assertEquals(newName, demoUser.getProperties().get("cn"));

		tm.begin();
		userAdmin.removeRole(DEMO_USER_DN);
		tm.commit();
		persistAndRestart();

		// check data
		Role[] search = userAdmin.getRoles("(objectclass=inetOrgPerson)");
		assertEquals(1, search.length);
		Group editorGroup = (Group) userAdmin.getRole(EDITORS_GROUP_DN);
		assertNotNull(editorGroup);
		Role[] members = editorGroup.getMembers();
		assertEquals(1, members.length);
	}

	public void testRetrieve() throws Exception {
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

		Group editorGroup = (Group) userAdmin.getRole(EDITORS_GROUP_DN);
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
		assertTrue(rootRoles.contains(EDITORS_GROUP_DN));

		// properties
		assertEquals("root@localhost", rootUser.getProperties().get("mail"));

		// credentials
		byte[] hashedPassword = ("{SHA}" + Base64.getEncoder().encodeToString(DigestUtils.sha1("demo".getBytes())))
				.getBytes();
		assertTrue(rootUser.hasCredential(LdapAttrs.userPassword.name(), hashedPassword));
		assertTrue(demoUser.hasCredential(LdapAttrs.userPassword.name(), hashedPassword));

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

	public void testReadWriteRead() throws Exception {
		if (userAdmin instanceof LdifUserAdmin) {
			Dictionary<String, Object> props = userAdmin.getProperties();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			((LdifUserAdmin) userAdmin).save(out);
			byte[] arr = out.toByteArray();
			out.close();
			userAdmin.destroy();
			// String written = new String(arr);
			// System.out.print(written);
			try (ByteArrayInputStream in = new ByteArrayInputStream(arr)) {
				userAdmin = new LdifUserAdmin(props);
				((LdifUserAdmin) userAdmin).load(in);
			}
			Role[] search = userAdmin.getRoles(null);
			assertEquals(4, search.length);
		} else {
			// test not relevant for LDAP
		}
	}

	@Override
	protected void setUp() throws Exception {
		tempDir = Files.createTempDirectory(getClass().getName());
		tempDir.toFile().deleteOnExit();
		String uriProp = System.getProperty("argeo.userdirectory.uri");
		if (uriProp != null)
			uri = new URI(uriProp);
		else {
			tempDir.toFile().deleteOnExit();
			Path ldifPath = tempDir.resolve(BASE_DN + ".ldif");
			try (InputStream in = getClass().getResource("basic.ldif").openStream()) {
				Files.copy(in, ldifPath);
			}
			uri = ldifPath.toUri();
		}

		// Init transaction manager
		if (TM_SIMPLE == tmType) {
			tm = new SimpleTransactionManager();
		} else if (TM_BITRONIX == tmType) {
			bitronix.tm.Configuration tmConf = TransactionManagerServices.getConfiguration();
			tmConf.setServerId(UUID.randomUUID().toString());
			tmConf.setLogPart1Filename(new File(tempDir.toFile(), "btm1.tlog").getAbsolutePath());
			tmConf.setLogPart2Filename(new File(tempDir.toFile(), "btm2.tlog").getAbsolutePath());
			tm = TransactionManagerServices.getTransactionManager();
		}

		userAdmin = initUserAdmin(uri, tm);
	}

	private AbstractUserDirectory initUserAdmin(URI uri, TransactionManager tm) {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(UserAdminConf.uri.name(), uri.toString());
		props.put(UserAdminConf.baseDn.name(), BASE_DN);
		props.put(UserAdminConf.userBase.name(), "ou=users");
		props.put(UserAdminConf.groupBase.name(), "ou=groups");
		AbstractUserDirectory userAdmin;
		if (uri.getScheme().startsWith("ldap"))
			userAdmin = new LdapUserAdmin(props);
		else
			userAdmin = new LdifUserAdmin(props);
		userAdmin.init();
		// JTA
		if (TM_BITRONIX == tmType)
			EhCacheXAResourceProducer.registerXAResource(UserDirectory.class.getName(), userAdmin.getXaResource());
		userAdmin.setTransactionManager(tm);
		return userAdmin;
	}

	private void persistAndRestart() {
		if (TM_BITRONIX == tmType)
			EhCacheXAResourceProducer.unregisterXAResource(UserDirectory.class.getName(), userAdmin.getXaResource());
		if (userAdmin instanceof LdifUserAdmin)
			((LdifUserAdmin) userAdmin).save();
		userAdmin.destroy();
		userAdmin = initUserAdmin(uri, tm);
	}

	@Override
	protected void tearDown() throws Exception {
		if (TM_BITRONIX == tmType) {
			EhCacheXAResourceProducer.unregisterXAResource(UserDirectory.class.getName(), userAdmin.getXaResource());
			((BitronixTransactionManager) tm).shutdown();
		}
		if (userAdmin != null)
			userAdmin.destroy();
		if (tempDir != null)
			Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}

			});
	}

}
