package org.argeo.jcr;

import java.io.File;

import javax.jcr.Repository;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.TransientRepository;
import org.argeo.server.jcr.JcrResourceAdapterTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public abstract class AbstractJcrTestCase extends TestCase {
	private TransientRepository repository;

	@Override
	protected void setUp() throws Exception {
		File homeDir = new File(System.getProperty("java.io.tmpdir"),
				JcrResourceAdapterTest.class.getSimpleName());
		FileUtils.deleteDirectory(homeDir);
		repository = new TransientRepository(getRepositoryFile(), homeDir);
	}

	@Override
	protected void tearDown() throws Exception {
	}

	protected File getRepositoryFile() throws Exception {
		Resource res = new ClassPathResource("org/argeo/jcr/repository.xml");
		return res.getFile();
	}

	protected Repository getRepository() {
		return repository;
	}
}
