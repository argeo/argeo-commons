/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.UUID;

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
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.RepositoryConfigurationParser;
import org.apache.jackrabbit.jcr2dav.Jcr2davRepositoryFactory;
import org.argeo.ArgeoException;
import org.xml.sax.InputSource;

/**
 * Wrapper around a Jackrabbit repository which allows to simplify configuration
 * and intercept some actions. It exposes itself as a {@link Repository}.
 */
public abstract class JackrabbitWrapper implements Repository {
	private Log log = LogFactory.getLog(JackrabbitWrapper.class);

	// remote
	private String uri = null;

	// local
	private RepositoryConfig repositoryConfig;
	private File homeDirectory;
	private Boolean inMemory = false;

	// wrapped repository
	private Repository repository;

	private Boolean autocreateWorkspaces = false;

	/**
	 * Empty constructor, {@link #init()} should be called after properties have
	 * been set
	 */
	public JackrabbitWrapper() {
	}

	/**
	 * Reads the configuration which will initialize a {@link RepositoryConfig}.
	 */
	protected abstract InputStream readConfiguration();

	/**
	 * Reads the variables which will initialize a {@link Properties}. Returns
	 * null by default, to be overridden.
	 * 
	 * @return a new stream or null if no variables available
	 */
	protected InputStream readVariables() {
		return null;
	}

	/**
	 * Resolves ${} placeholders in the provided string. Based on system
	 * properties if no map is provided.
	 */
	protected abstract String resolvePlaceholders(String string,
			Map<String, String> variables);

	/** Initializes */
	public void init() {
		long begin = System.currentTimeMillis();

		if (repository != null) {
			// we are just wrapping another repository
			postInitWrapped();
		} else {
			createJackrabbitRepository();
			postInitNew();
		}

		double duration = ((double) (System.currentTimeMillis() - begin)) / 1000;
		if (log.isTraceEnabled())
			log.trace("Initialized Jackrabbit wrapper in " + duration + " s");
	}

	/**
	 * Called after initialization of an already existing {@link Repository}
	 * which is being wrapped (e.g. in order to impact its data model). To be
	 * overridden, does nothing by default.
	 */
	protected void postInitWrapped() {

	}

	/**
	 * Called after initialization of a new {@link Repository} either local or
	 * remote. To be overridden, does nothing by default.
	 */
	protected void postInitNew() {

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
				double duration = ((double) (System.currentTimeMillis() - begin)) / 1000;
				log.info("Created Jackrabbit repository in " + duration
						+ " s from URI " + uri);
				// we assume that the data model of the remote repository has
				// been properly initialized
			} else {// local
				// force uri to null in order to optimize isRemote()
				uri = null;

				// temporary
				if (inMemory && getHomeDirectory().exists()) {
					FileUtils.deleteDirectory(getHomeDirectory());
					log.warn("Deleted Jackrabbit home directory "
							+ getHomeDirectory());
				}

				// process configuration file
				Properties vars = getConfigurationProperties();
				configurationIn = readConfiguration();
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
				if (log.isTraceEnabled())
					log.trace("Created Jackrabbit repository in " + duration
							+ " s, home: " + getHomeDirectory());
			}
		} catch (Exception e) {
			throw new ArgeoException("Cannot create Jackrabbit repository "
					+ getHomeDirectory(), e);
		} finally {
			IOUtils.closeQuietly(configurationIn);
		}
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
					+ " s, home: " + getHomeDirectory());
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
			propsIn = readVariables();
			if (propsIn != null) {
				vars.load(propsIn);
			}
			// resolve system properties
			for (Object key : vars.keySet()) {
				// TODO: implement a smarter mechanism to resolve nested ${}
				String newValue = resolvePlaceholders(
						vars.getProperty(key.toString()), null);
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
			throw new ArgeoException("No repository initialized."
					+ " Was the init() method called?"
					+ " The destroy() method should also"
					+ " be called on shutdown.");
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

	public void setInMemory(Boolean inMemory) {
		this.inMemory = inMemory;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

}
