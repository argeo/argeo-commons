package org.argeo.server.jackrabbit;

import java.io.File;
import java.io.InputStream;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

@SuppressWarnings("restriction")
public class JackrabbitContainer implements InitializingBean, DisposableBean,
		Repository {
	private Resource configuration;
	private File homeDirectory;

	private RepositoryImpl repository;

	public void afterPropertiesSet() throws Exception {
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

		repository = RepositoryImpl.create(config);
	}

	public void destroy() throws Exception {
		if (repository != null)
			repository.shutdown();
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

}
