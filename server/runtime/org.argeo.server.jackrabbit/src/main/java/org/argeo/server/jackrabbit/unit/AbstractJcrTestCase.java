package org.argeo.server.jackrabbit.unit;

import java.io.File;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.TransientRepository;
import org.argeo.ArgeoException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public abstract class AbstractJcrTestCase extends TestCase {
	private TransientRepository repository;
	private Session session = null;

	@Override
	protected void setUp() throws Exception {
		File homeDir = new File(System.getProperty("java.io.tmpdir"),
				AbstractJcrTestCase.class.getSimpleName());
		FileUtils.deleteDirectory(homeDir);
		repository = new TransientRepository(getRepositoryFile(), homeDir);
	}

	@Override
	protected void tearDown() throws Exception {
		if (session != null)
			session.logout();
	}

	protected Session session() {
		if (session == null) {
			try {
				session = getRepository().login(
						new SimpleCredentials("demo", "demo".toCharArray()));
			} catch (Exception e) {
				throw new ArgeoException("Cannot login to repository", e);
			}
		}
		return session;
	}

	protected File getRepositoryFile() throws Exception {
		Resource res = new ClassPathResource(
				"org/argeo/server/jackrabbit/repository-inMemory.xml");
		return res.getFile();
	}

	protected Repository getRepository() {
		return repository;
	}
}
