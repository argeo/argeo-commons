package org.argeo.server.jackrabbit;

import java.io.File;
import java.io.InputStream;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

public class JackrabbitContainer implements InitializingBean, DisposableBean,
		Repository {
	private Log log = LogFactory.getLog(JackrabbitContainer.class);

	private Resource configuration;
	private File homeDirectory;

	private Boolean inMemory = false;

	private Repository repository;

	public void afterPropertiesSet() throws Exception {
		if (inMemory && homeDirectory.exists()) {
			FileUtils.deleteDirectory(homeDirectory);
			log.warn("Deleted Jackrabbit home directory " + homeDirectory);
		}

		RepositoryConfig config;
		InputStream in = configuration.getInputStream();
		try {
			config = RepositoryConfig.create(in, homeDirectory
					.getCanonicalPath());
		} catch (Exception e) {
			throw new RuntimeException("Cannot read configuration", e);
		} finally {
			IOUtils.closeQuietly(in);
		}

		if (inMemory)
			repository = new TransientRepository(config);
		else
			repository = RepositoryImpl.create(config);

		log.info("Initialized Jackrabbit repository " + repository + " in "
				+ homeDirectory + " with config " + configuration);
	}

	public void destroy() throws Exception {
		if (repository != null) {
			if (repository instanceof RepositoryImpl)
				((RepositoryImpl) repository).shutdown();
			else if (repository instanceof TransientRepository)
				((TransientRepository) repository).shutdown();
		}

		if (inMemory)
			if (homeDirectory.exists()) {
				FileUtils.deleteDirectory(homeDirectory);
				if (log.isDebugEnabled())
					log.debug("Deleted Jackrabbit home directory "
							+ homeDirectory);
			}
		log.info("Destroyed Jackrabbit repository " + repository + " in "
				+ homeDirectory + " with config " + configuration);
	}

	// JCR REPOSITORY (delegated)
	public String getDescriptor(String key) {
		return repository.getDescriptor(key);
	}

	public String[] getDescriptorKeys() {
		return repository.getDescriptorKeys();
	}

	public Session login() throws LoginException, RepositoryException {
		return repository.login();
	}

	public Session login(Credentials credentials, String workspaceName)
			throws LoginException, NoSuchWorkspaceException,
			RepositoryException {
		return repository.login(credentials, workspaceName);
	}

	public Session login(Credentials credentials) throws LoginException,
			RepositoryException {
		return repository.login(credentials);
	}

	public Session login(String workspaceName) throws LoginException,
			NoSuchWorkspaceException, RepositoryException {
		return repository.login(workspaceName);
	}

	// BEANS METHODS
	public void setHomeDirectory(File homeDirectory) {
		this.homeDirectory = homeDirectory;
	}

	public void setConfiguration(Resource configuration) {
		this.configuration = configuration;
	}

	public void setInMemory(Boolean inMemory) {
		this.inMemory = inMemory;
	}

}
