package org.argeo.jackrabbit.unit;

import java.net.URL;

import javax.jcr.Repository;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.argeo.jcr.unit.AbstractJcrTestCase;

/** Factorizes configuration of an in memory transient repository */
public abstract class AbstractJackrabbitTestCase extends AbstractJcrTestCase {
	protected RepositoryImpl repositoryImpl;

	// protected File getRepositoryFile() throws Exception {
	// Resource res = new ClassPathResource(
	// "org/argeo/jackrabbit/unit/repository-memory.xml");
	// return res.getFile();
	// }

	public AbstractJackrabbitTestCase() {
		URL url = AbstractJackrabbitTestCase.class.getResource("jaas.config");
		assert url != null;
		System.setProperty("java.security.auth.login.config", url.toString());
	}

	protected Repository createRepository() throws Exception {
		// Repository repository = new TransientRepository(getRepositoryFile(),
		// getHomeDir());
		RepositoryConfig repositoryConfig = RepositoryConfig.create(
				AbstractJackrabbitTestCase.class
						.getResourceAsStream(getRepositoryConfigResource()),
				getHomeDir().getAbsolutePath());
		RepositoryImpl repositoryImpl = RepositoryImpl.create(repositoryConfig);
		return repositoryImpl;
	}

	protected String getRepositoryConfigResource() {
		return "repository-memory.xml";
	}

	@Override
	protected void clearRepository(Repository repository) throws Exception {
		RepositoryImpl repositoryImpl = (RepositoryImpl) repository;
		if (repositoryImpl != null)
			repositoryImpl.shutdown();
		FileUtils.deleteDirectory(getHomeDir());
	}

}
