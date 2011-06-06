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

package org.argeo.jackrabbit;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;

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
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.commons.NamespaceHelper;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.RepositoryConfigurationParser;
import org.apache.jackrabbit.jcr2dav.Jcr2davRepositoryFactory;
import org.argeo.ArgeoException;
import org.argeo.jcr.JcrUtils;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.SystemPropertyUtils;
import org.xml.sax.InputSource;

/**
 * Wrapper around a Jackrabbit repository which allows to configure it in Spring
 * and expose it as a {@link Repository}.
 */
public class JackrabbitContainer implements Repository, ResourceLoaderAware {
	private Log log = LogFactory.getLog(JackrabbitContainer.class);

	private Resource configuration;
	private String homeDirectory;
	private Resource variables;
	/** cache home */
	private File home;

	private Boolean inMemory = false;
	private String uri = null;

	private Repository repository;

	private ResourceLoader resourceLoader;

	/** Node type definitions in CND format */
	private List<String> cndFiles = new ArrayList<String>();

	/** Migrations to execute (if not already done) */
	private Set<JackrabbitDataModelMigration> dataModelMigrations = new HashSet<JackrabbitDataModelMigration>();

	/** Namespaces to register: key is prefix, value namespace */
	private Map<String, String> namespaces = new HashMap<String, String>();

	private Boolean autocreateWorkspaces = false;

	private Executor systemExecutor;

	public void init() throws Exception {
		if (repository != null) {
			// we are just wrapping another repository
			importNodeTypeDefinitions(repository);
			return;
		}

		repository = createJackrabbitRepository();

		// migrate if needed
		migrate();

		// apply new CND files after migration
		if (cndFiles != null && cndFiles.size() > 0)
			importNodeTypeDefinitions(repository);
	}

	/** Actually creates a new repository. */
	protected JackrabbitRepository createJackrabbitRepository() {
		JackrabbitRepository repository;
		try {
			// remote repository
			if (uri != null && !uri.trim().equals("")) {
				Map<String, String> params = new HashMap<String, String>();
				params.put(
						org.apache.jackrabbit.commons.JcrUtils.REPOSITORY_URI,
						uri);
				repository = (JackrabbitRepository) new Jcr2davRepositoryFactory()
						.getRepository(params);
				if (repository == null)
					throw new ArgeoException("Remote Davex repository " + uri
							+ " not found");
				log.info("Initialized Jackrabbit repository " + repository
						+ " from URI " + uri);
				// do not perform further initialization since we assume that
				// the
				// remote repository has been properly configured
				return repository;
			}

			// local repository
			if (inMemory && getHome().exists()) {
				FileUtils.deleteDirectory(getHome());
				log.warn("Deleted Jackrabbit home directory " + getHome());
			}

			RepositoryConfig config;
			Properties vars = getConfigurationProperties();
			InputStream in = configuration.getInputStream();
			try {
				vars.put(
						RepositoryConfigurationParser.REPOSITORY_HOME_VARIABLE,
						getHome().getCanonicalPath());
				config = RepositoryConfig.create(new InputSource(in), vars);
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
					+ getHome() + " with config " + configuration);

			return repository;
		} catch (Exception e) {
			throw new ArgeoException("Cannot create Jackrabbit repository "
					+ getHome(), e);
		}
	}

	/** Executes migrations, if needed. */
	protected void migrate() {
		Boolean restartAndClearCaches = false;

		// migrate data
		Session session = null;
		try {
			session = login();
			for (JackrabbitDataModelMigration dataModelMigration : new TreeSet<JackrabbitDataModelMigration>(
					dataModelMigrations)) {
				if (dataModelMigration.migrate(session)) {
					restartAndClearCaches = true;
				}
			}
		} catch (ArgeoException e) {
			throw e;
		} catch (Exception e) {
			throw new ArgeoException("Cannot migrate", e);
		} finally {
			JcrUtils.logoutQuietly(session);
		}

		// restart repository
		if (restartAndClearCaches) {
			((JackrabbitRepository) repository).shutdown();
			JackrabbitDataModelMigration.clearRepositoryCaches(getHome());
			repository = createJackrabbitRepository();
		}
	}

	/** Lazy init. */
	protected File getHome() {
		if (home != null)
			return home;

		try {
			String osgiData = System.getProperty("osgi.instance.area");
			if (osgiData != null)
				osgiData = osgiData.substring("file:".length());
			String path;
			if (homeDirectory == null)
				path = "./jackrabbit";
			else
				path = homeDirectory;
			if (path.startsWith(".") && osgiData != null) {
				home = new File(osgiData + '/' + path).getCanonicalFile();
			} else
				home = new File(path).getCanonicalFile();
			return home;
		} catch (Exception e) {
			throw new ArgeoException("Cannot define Jackrabbit home based on "
					+ homeDirectory, e);
		}
	}

	public void dispose() throws Exception {
		if (repository != null) {
			if (repository instanceof JackrabbitRepository)
				((JackrabbitRepository) repository).shutdown();
			else if (repository instanceof RepositoryImpl)
				((RepositoryImpl) repository).shutdown();
			else if (repository instanceof TransientRepository)
				((TransientRepository) repository).shutdown();
		}

		if (inMemory)
			if (getHome().exists()) {
				FileUtils.deleteDirectory(getHome());
				if (log.isDebugEnabled())
					log.debug("Deleted Jackrabbit home directory " + getHome());
			}

		if (uri != null && !uri.trim().equals(""))
			log.info("Destroyed Jackrabbit repository with uri " + uri);
		else
			log.info("Destroyed Jackrabbit repository " + repository + " in "
					+ getHome() + " with config " + configuration);
	}

	/**
	 * @deprecated explicitly declare {@link #dispose()} as destroy-method
	 *             instead.
	 */
	public void destroy() throws Exception {
		log.error("## Declare destroy-method=\"dispose\". in the Jackrabbit container bean");
	}

	/** @deprecated explicitly declare {@link #init()} as init-method instead. */
	public void afterPropertiesSet() throws Exception {
		log.error("## Declare init-method=\"init\". in the Jackrabbit container bean");
	}

	protected Properties getConfigurationProperties() {
		InputStream propsIn = null;
		Properties vars;
		try {
			vars = new Properties();
			if (variables != null) {
				propsIn = variables.getInputStream();
				vars.load(propsIn);
			}
			// resolve system properties
			for (Object key : vars.keySet()) {
				// TODO: implement a smarter mechanism to resolve nested ${}
				String newValue = SystemPropertyUtils.resolvePlaceholders(vars
						.getProperty(key.toString()));
				vars.put(key, newValue);
			}
			// override with system properties
			vars.putAll(System.getProperties());
		} catch (IOException e) {
			throw new ArgeoException("Cannot read configuration properties", e);
		} finally {
			IOUtils.closeQuietly(propsIn);
		}
		return vars;
	}

	/**
	 * Import declared node type definitions, trying to update them if they have
	 * changed. In case of failures an error will be logged but no exception
	 * will be thrown.
	 */
	protected void importNodeTypeDefinitions(final Repository repository) {
		Runnable action = new Runnable() {
			public void run() {
				Reader reader = null;
				Session session = null;
				try {
					session = repository.login();
					processNewSession(session);
					// Load cnds as resources
					for (String resUrl : cndFiles) {
						Resource res = resourceLoader.getResource(resUrl);
						byte[] arr = IOUtils.toByteArray(res.getInputStream());
						reader = new InputStreamReader(
								new ByteArrayInputStream(arr));
						CndImporter.registerNodeTypes(reader, session, true);
					}
					session.save();
				} catch (Exception e) {
					log.error(
							"Cannot import node type definitions " + cndFiles,
							e);
					JcrUtils.discardQuietly(session);
				} finally {
					IOUtils.closeQuietly(reader);
					JcrUtils.logoutQuietly(session);
				}
			}
		};

		if (systemExecutor != null)
			systemExecutor.execute(action);
		else
			action.run();
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
	public void setHomeDirectory(String homeDirectory) {
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

	public void setUri(String uri) {
		this.uri = uri;
	}

	public void setSystemExecutor(Executor systemExecutor) {
		this.systemExecutor = systemExecutor;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setDataModelMigrations(
			Set<JackrabbitDataModelMigration> dataModelMigrations) {
		this.dataModelMigrations = dataModelMigrations;
	}

}
