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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Executor;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.commons.NamespaceHelper;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.RepositoryConfigurationParser;
import org.apache.jackrabbit.jcr2dav.Jcr2davRepositoryFactory;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.SystemAuthentication;
import org.springframework.core.io.Resource;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.util.SystemPropertyUtils;
import org.xml.sax.InputSource;

/**
 * Wrapper around a Jackrabbit repository which allows to configure it in Spring
 * and expose it as a {@link Repository}.
 */
public class JackrabbitContainer implements Repository {
	private Log log = LogFactory.getLog(JackrabbitContainer.class);

	// remote
	private String uri = null;
	private Credentials remoteSystemCredentials = null;

	// local
	private Resource configuration;
	private RepositoryConfig repositoryConfig;
	private File homeDirectory;
	private Resource variables;
	private Boolean inMemory = false;

	// wrapped repository
	private Repository repository;

	// data model
	/** Node type definitions in CND format */
	private List<String> cndFiles = new ArrayList<String>();

	/** Migrations to execute (if not already done) */
	private Set<JackrabbitDataModelMigration> dataModelMigrations = new HashSet<JackrabbitDataModelMigration>();

	/** Namespaces to register: key is prefix, value namespace */
	private Map<String, String> namespaces = new HashMap<String, String>();

	private Boolean autocreateWorkspaces = false;

	private Executor systemExecutor;

	/**
	 * Empty constructor, {@link #init()} should be called after properties have
	 * been set
	 */
	public JackrabbitContainer() {
	}

	/**
	 * Convenience constructor for remote, {@link #init()} is called in the
	 * constructor.
	 */
	public JackrabbitContainer(String uri, Credentials remoteSystemCredentials) {
		setUri(uri);
		setRemoteSystemCredentials(remoteSystemCredentials);
		init();
	}

	/** Initializes */
	public void init() {
		if (repository != null) {
			// we are just wrapping another repository
			prepareDataModel();
			return;
		}

		createJackrabbitRepository();
		// migrate if needed
		migrate();

		// apply new CND files after migration
		if (cndFiles != null && cndFiles.size() > 0)
			prepareDataModel();
	}

	/** Actually creates the new repository. */
	protected void createJackrabbitRepository() {
		long begin = System.currentTimeMillis();
		InputStream configurationIn = null;
		try {
			if (uri != null && !uri.trim().equals("")) {// remote
				Map<String, String> params = new HashMap<String, String>();
				params.put(
						org.apache.jackrabbit.commons.JcrUtils.REPOSITORY_URI,
						uri);
				repository = new Jcr2davRepositoryFactory()
						.getRepository(params);
				if (repository == null)
					throw new ArgeoException("Remote Davex repository " + uri
							+ " not found");
				log.info("Initialized Jackrabbit repository " + repository
						+ " from URI " + uri);
				// we assume that the remote repository has been properly
				// configured
			} else {// local
				// reset uri to null in order to optimize isRemote()
				uri = null;

				// temporary
				if (inMemory && getHomeDirectory().exists()) {
					FileUtils.deleteDirectory(getHomeDirectory());
					log.warn("Deleted Jackrabbit home directory "
							+ getHomeDirectory());
				}

				// process configuration file
				Properties vars = getConfigurationProperties();
				configurationIn = configuration.getInputStream();
				vars.put(
						RepositoryConfigurationParser.REPOSITORY_HOME_VARIABLE,
						getHomeDirectory().getCanonicalPath());
				repositoryConfig = RepositoryConfig.create(new InputSource(
						configurationIn), vars);

				//
				// Actual repository creation
				//
				repository = RepositoryImpl.create(repositoryConfig);

				double duration = ((double) (System.currentTimeMillis() - begin)) / 1000;
				log.info("Initialized Jackrabbit repository in " + duration
						+ " s, home: " + getHomeDirectory() + ", config: "
						+ configuration);
			}
		} catch (Exception e) {
			throw new ArgeoException("Cannot create Jackrabbit repository "
					+ getHomeDirectory(), e);
		} finally {
			IOUtils.closeQuietly(configurationIn);
		}
	}

	/** Executes migrations, if needed. */
	protected void migrate() {
		// Remote migration not supported
		if (isRemote())
			return;

		// No migration to perform
		if (dataModelMigrations.size() == 0)
			return;

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
			JackrabbitDataModelMigration
					.clearRepositoryCaches(repositoryConfig);
			((JackrabbitRepository) repository).shutdown();
			createJackrabbitRepository();
		}

		// set data model version
		try {
			session = login();
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot login to migrated repository", e);
		}

		for (JackrabbitDataModelMigration dataModelMigration : new TreeSet<JackrabbitDataModelMigration>(
				dataModelMigrations)) {
			try {
				if (session.itemExists(dataModelMigration
						.getDataModelNodePath())) {
					Node dataModelNode = session.getNode(dataModelMigration
							.getDataModelNodePath());
					dataModelNode.setProperty(
							ArgeoNames.ARGEO_DATA_MODEL_VERSION,
							dataModelMigration.getTargetVersion());
					session.save();
				}
			} catch (Exception e) {
				log.error("Cannot set model version", e);
			}
		}
		JcrUtils.logoutQuietly(session);

	}

	/** Lazy init. */
	protected File getHomeDirectory() {
		try {
			if (homeDirectory == null) {
				if (inMemory) {
					homeDirectory = new File(
							System.getProperty("java.io.tmpdir")
									+ File.separator
									+ System.getProperty("user.name")
									+ File.separator + "jackrabbit-"
									+ UUID.randomUUID());
					homeDirectory.mkdirs();
					// will it work if directory is not empty??
					homeDirectory.deleteOnExit();
				}
			}

			return homeDirectory.getCanonicalFile();
		} catch (IOException e) {
			throw new ArgeoException("Cannot get canonical file for "
					+ homeDirectory, e);
		}
	}

	/** Shutdown the repository */
	public void destroy() throws Exception {
		if (repository != null && repository instanceof RepositoryImpl) {
			long begin = System.currentTimeMillis();
			((RepositoryImpl) repository).shutdown();
			if (inMemory)
				if (getHomeDirectory().exists()) {
					FileUtils.deleteDirectory(getHomeDirectory());
					if (log.isDebugEnabled())
						log.debug("Deleted Jackrabbit home directory "
								+ getHomeDirectory());
				}
			double duration = ((double) (System.currentTimeMillis() - begin)) / 1000;
			log.info("Destroyed Jackrabbit repository in " + duration
					+ " s, home: " + getHomeDirectory() + ", config "
					+ configuration);
		}
	}

	/**
	 * @deprecated explicitly declare {@link #destroy()} as destroy-method
	 *             instead.
	 */
	public void dispose() throws Exception {
		log.error("## Declare destroy-method=\"destroy\". in the Jackrabbit container bean");
		destroy();
	}

	/*
	 * UTILITIES
	 */

	/** Generates the properties to use in the configuration. */
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

			if (log.isTraceEnabled()) {
				log.trace("Jackrabbit config variables:");
				for (Object key : new TreeSet<Object>(vars.keySet()))
					log.trace(key + "=" + vars.getProperty(key.toString()));
			}

		} catch (IOException e) {
			throw new ArgeoException("Cannot read configuration properties", e);
		} finally {
			IOUtils.closeQuietly(propsIn);
		}
		return vars;
	}

	/**
	 * Import declared node type definitions and register namespaces. Tries to
	 * update the node definitions if they have changed. In case of failures an
	 * error will be logged but no exception will be thrown.
	 */
	protected void prepareDataModel() {
		// importing node def on remote si currently not supported
		if (isRemote())
			return;

		Runnable action = new Runnable() {
			public void run() {
				Session session = null;
				try {
					session = login();
					// register namespaces
					if (namespaces.size() > 0) {
						NamespaceHelper namespaceHelper = new NamespaceHelper(
								session);
						namespaceHelper.registerNamespaces(namespaces);
					}
					// load CND files from classpath or as URL
					for (String resUrl : cndFiles) {
						boolean classpath;
						if (resUrl.startsWith("classpath:")) {
							resUrl = resUrl.substring("classpath:".length());
							classpath = true;
						} else if (resUrl.indexOf(':') < 0) {
							classpath = true;
						} else {
							classpath = false;
						}

						URL url = classpath ? getClass().getClassLoader()
								.getResource(resUrl) : new URL(resUrl);

						Reader reader = null;
						try {
							reader = new InputStreamReader(url.openStream());
							CndImporter
									.registerNodeTypes(reader, session, true);
						} finally {
							IOUtils.closeQuietly(reader);
						}
					}
				} catch (Exception e) {
					log.error(
							"Cannot import node type definitions " + cndFiles,
							e);
					JcrUtils.discardQuietly(session);
				} finally {
					JcrUtils.logoutQuietly(session);
				}
			}
		};

		if (systemExecutor != null)
			systemExecutor.execute(action);
		else
			action.run();
	}

	/*
	 * DELEGATED JCR REPOSITORY METHODS
	 */

	public String getDescriptor(String key) {
		return getRepository().getDescriptor(key);
	}

	public String[] getDescriptorKeys() {
		return getRepository().getDescriptorKeys();
	}

	/** Central login method */
	public Session login(Credentials credentials, String workspaceName)
			throws LoginException, NoSuchWorkspaceException,
			RepositoryException {

		// retrieve credentials for remote
		if (credentials == null && isRemote()) {
			Authentication authentication = SecurityContextHolder.getContext()
					.getAuthentication();
			if (authentication != null) {
				if (authentication instanceof UsernamePasswordAuthenticationToken) {
					UsernamePasswordAuthenticationToken upat = (UsernamePasswordAuthenticationToken) authentication;
					credentials = new SimpleCredentials(upat.getName(), upat
							.getCredentials().toString().toCharArray());
				} else if ((authentication instanceof SystemAuthentication)
						&& remoteSystemCredentials != null) {
					credentials = remoteSystemCredentials;
				}
			}
		}

		Session session;
		try {
			session = getRepository().login(credentials, workspaceName);
		} catch (NoSuchWorkspaceException e) {
			if (autocreateWorkspaces && workspaceName != null)
				session = createWorkspaceAndLogsIn(credentials, workspaceName);
			else
				throw e;
		}
		processNewSession(session);
		return session;
	}

	public Session login() throws LoginException, RepositoryException {
		return login(null, null);
	}

	public Session login(Credentials credentials) throws LoginException,
			RepositoryException {
		return login(credentials, null);
	}

	public Session login(String workspaceName) throws LoginException,
			NoSuchWorkspaceException, RepositoryException {
		return login(null, workspaceName);
	}

	/** Called after a session has been created, does nothing by default. */
	protected void processNewSession(Session session) {
	}

	public Boolean isRemote() {
		return uri != null;
	}

	/** Wraps access to the repository, making sure it is available. */
	protected Repository getRepository() {
		if (repository == null) {
			throw new ArgeoException(
					"No repository initialized."
							+ " Was the init() method called?"
							+ " The dispose() method should also be called on shutdown.");
		}
		return repository;
	}

	/**
	 * Logs in to the default workspace, creates the required workspace, logs
	 * out, logs in to the required workspace.
	 */
	protected Session createWorkspaceAndLogsIn(Credentials credentials,
			String workspaceName) throws RepositoryException {
		if (workspaceName == null)
			throw new ArgeoException("No workspace specified.");
		Session session = getRepository().login(credentials);
		session.getWorkspace().createWorkspace(workspaceName);
		session.logout();
		return getRepository().login(credentials, workspaceName);
	}

	public boolean isStandardDescriptor(String key) {
		return getRepository().isStandardDescriptor(key);
	}

	public boolean isSingleValueDescriptor(String key) {
		return getRepository().isSingleValueDescriptor(key);
	}

	public Value getDescriptorValue(String key) {
		return getRepository().getDescriptorValue(key);
	}

	public Value[] getDescriptorValues(String key) {
		return getRepository().getDescriptorValues(key);
	}

	/*
	 * FIELDS ACCESS
	 */

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

	public void setUri(String uri) {
		this.uri = uri;
	}

	public void setRemoteSystemCredentials(Credentials remoteSystemCredentials) {
		this.remoteSystemCredentials = remoteSystemCredentials;
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
