/*
 * Copyright (C) 2010 Mathieu Baudier <mbaudier@argeo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.argeo.server.jackrabbit;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.NamespaceHelper;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.RepositoryConfigurationParser;
import org.argeo.ArgeoException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.xml.sax.InputSource;

/**
 * Wrapper around a Jackrabbit repository which allows to configure it in Spring
 * and expose it as a {@link Repository}.
 */
public class JackrabbitContainer implements InitializingBean, DisposableBean,
		Repository, ResourceLoaderAware {
	private Log log = LogFactory.getLog(JackrabbitContainer.class);

	private Resource configuration;
	private File homeDirectory;
	private Resource variables;

	private Boolean inMemory = false;

	private Repository repository;

	private ResourceLoader resourceLoader;

	/** Node type definitions in CND format */
	private List<byte[]> cnds = new ArrayList<byte[]>();
	private List<String> cndFiles = new ArrayList<String>();

	/** Namespaces to register: key is prefix, value namespace */
	private Map<String, String> namespaces = new HashMap<String, String>();

	private Boolean autocreateWorkspaces = false;

	public void afterPropertiesSet() throws Exception {
		// Load cnds as resources
		for (String resUrl : cndFiles) {

			Resource res = resourceLoader.getResource(resUrl);
			byte[] arr = IOUtils.toByteArray(res.getInputStream());
			cnds.add(arr);
		}

		if (inMemory && homeDirectory.exists()) {
			FileUtils.deleteDirectory(homeDirectory);
			log.warn("Deleted Jackrabbit home directory " + homeDirectory);
		}

		RepositoryConfig config;
		InputStream in = configuration.getInputStream();
		InputStream propsIn = null;
		try {
			Properties vars = new Properties();
			if (variables != null) {
				propsIn = variables.getInputStream();
				vars.load(propsIn);
			}
			// override with system properties
			vars.putAll(System.getProperties());
			vars.put(RepositoryConfigurationParser.REPOSITORY_HOME_VARIABLE,
					homeDirectory.getCanonicalPath());
			config = RepositoryConfig.create(new InputSource(in), vars);
		} catch (Exception e) {
			throw new RuntimeException("Cannot read configuration", e);
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(propsIn);
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
		Session session = repository.login();
		processNewSession(session);
		return session;
	}

	public Session login(Credentials credentials, String workspaceName)
			throws LoginException, NoSuchWorkspaceException,
			RepositoryException {
		Session session;
		try {
			session = repository.login(credentials, workspaceName);
		} catch (NoSuchWorkspaceException e) {
			if (autocreateWorkspaces)
				session = createWorkspaceAndLogsIn(credentials, workspaceName);
			else
				throw e;
		}
		processNewSession(session);
		return session;
	}

	public Session login(Credentials credentials) throws LoginException,
			RepositoryException {
		Session session = repository.login(credentials);
		processNewSession(session);
		return session;
	}

	public Session login(String workspaceName) throws LoginException,
			NoSuchWorkspaceException, RepositoryException {
		Session session;
		try {
			session = repository.login(workspaceName);
		} catch (NoSuchWorkspaceException e) {
			if (autocreateWorkspaces)
				session = createWorkspaceAndLogsIn(null, workspaceName);
			else
				throw e;
		}
		processNewSession(session);
		return session;
	}

	protected synchronized void processNewSession(Session session) {
		try {
			NamespaceHelper namespaceHelper = new NamespaceHelper(session);
			namespaceHelper.registerNamespaces(namespaces);

			for (byte[] arr : cnds)
				CndImporter.registerNodeTypes(new InputStreamReader(
						new ByteArrayInputStream(arr)), session, true);
		} catch (Exception e) {
			throw new ArgeoException("Cannot process new session", e);
		}
	}

	/**
	 * Logs in to the default workspace, creates the required workspace, logs
	 * out, logs in to the required workspace.
	 */
	protected Session createWorkspaceAndLogsIn(Credentials credentials,
			String workspaceName) throws RepositoryException {
		if (workspaceName == null)
			throw new ArgeoException("No workspace specified.");
		Session session = repository.login(credentials);
		session.getWorkspace().createWorkspace(workspaceName);
		session.logout();
		return repository.login(credentials, workspaceName);
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public boolean isStandardDescriptor(String key) {
		return repository.isStandardDescriptor(key);
	}

	public boolean isSingleValueDescriptor(String key) {
		return repository.isSingleValueDescriptor(key);
	}

	public Value getDescriptorValue(String key) {
		return repository.getDescriptorValue(key);
	}

	public Value[] getDescriptorValues(String key) {
		return repository.getDescriptorValues(key);
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

	public void setNamespaces(Map<String, String> namespaces) {
		this.namespaces = namespaces;
	}

	public void setCndFiles(List<String> cndFiles) {
		this.cndFiles = cndFiles;
	}

	public void setVariables(Resource variables) {
		this.variables = variables;
	}

}
