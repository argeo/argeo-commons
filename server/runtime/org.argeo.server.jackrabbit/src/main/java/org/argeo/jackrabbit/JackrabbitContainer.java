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
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.RepositoryConfigurationParser;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.JcrUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.SystemPropertyUtils;
import org.xml.sax.InputSource;

/**
 * Wrapper around a Jackrabbit repository which allows to configure it in Spring
 * and expose it as a {@link Repository}.
 */
public class JackrabbitContainer extends JackrabbitWrapper {
	private Log log = LogFactory.getLog(JackrabbitContainer.class);

	// local
	private Resource configuration;
	private Resource variables;
	private RepositoryConfig repositoryConfig;
	private File homeDirectory;
	private Boolean inMemory = false;

	/** Migrations to execute (if not already done) */
	private Set<JackrabbitDataModelMigration> dataModelMigrations = new HashSet<JackrabbitDataModelMigration>();

	/**
	 * Empty constructor, {@link #init()} should be called after properties have
	 * been set
	 */
	public JackrabbitContainer() {
	}

	public void init() {
		long begin = System.currentTimeMillis();

		if (getRepository() != null)
			throw new ArgeoException(
					"Cannot be used to wrap another repository");
		Repository repository = createJackrabbitRepository();
		super.setRepository(repository);

		// migrate if needed
		migrate();

		// apply new CND files after migration
		prepareDataModel();

		double duration = ((double) (System.currentTimeMillis() - begin)) / 1000;
		if (log.isDebugEnabled())
			log.debug("Initialized JCR repository wrapper in " + duration
					+ " s");
	}

	/** Actually creates the new repository. */
	protected Repository createJackrabbitRepository() {
		long begin = System.currentTimeMillis();
		InputStream configurationIn = null;
		Repository repository;
		try {
			// temporary
			if (inMemory && getHomeDirectory().exists()) {
				FileUtils.deleteDirectory(getHomeDirectory());
				log.warn("Deleted Jackrabbit home directory "
						+ getHomeDirectory());
			}

			// process configuration file
			Properties vars = getConfigurationProperties();
			configurationIn = readConfiguration();
			vars.put(RepositoryConfigurationParser.REPOSITORY_HOME_VARIABLE,
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

			return repository;
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

	/** Executes migrations, if needed. */
	protected void migrate() {
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
			Repository repository = getRepository();
			if (repository instanceof RepositoryImpl) {
				JackrabbitDataModelMigration
						.clearRepositoryCaches(((RepositoryImpl) repository)
								.getConfig());
			}
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

	/** Shutdown the repository */
	public void destroy() throws Exception {
		Repository repository = getRepository();
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
		repository = null;
	}

	public void dispose() {
		throw new IllegalArgumentException(
				"Call destroy() method instead of dispose()");
	}

	/*
	 * UTILITIES
	 */
	/**
	 * Reads the configuration which will initialize a {@link RepositoryConfig}.
	 */
	protected InputStream readConfiguration() {
		try {
			return configuration != null ? configuration.getInputStream()
					: null;
		} catch (IOException e) {
			throw new ArgeoException("Cannot read Jackrabbit configuration "
					+ configuration, e);
		}
	}

	/**
	 * Reads the variables which will initialize a {@link Properties}. Returns
	 * null by default, to be overridden.
	 * 
	 * @return a new stream or null if no variables available
	 */
	protected InputStream readVariables() {
		try {
			return variables != null ? variables.getInputStream() : null;
		} catch (IOException e) {
			throw new ArgeoException("Cannot read Jackrabbit variables "
					+ variables, e);
		}
	}

	/**
	 * Resolves ${} placeholders in the provided string. Based on system
	 * properties if no map is provided.
	 */
	protected String resolvePlaceholders(String string,
			Map<String, String> variables) {
		return SystemPropertyUtils.resolvePlaceholders(string);
	}

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
	 * FIELDS ACCESS
	 */

	public void setHomeDirectory(File homeDirectory) {
		this.homeDirectory = homeDirectory;
	}

	public void setInMemory(Boolean inMemory) {
		this.inMemory = inMemory;
	}

	public void setRepository(Repository repository) {
		throw new ArgeoException("Cannot be used to wrap another repository");
	}

	public void setDataModelMigrations(
			Set<JackrabbitDataModelMigration> dataModelMigrations) {
		this.dataModelMigrations = dataModelMigrations;
	}

	public void setVariables(Resource variables) {
		this.variables = variables;
	}

	public void setConfiguration(Resource configuration) {
		this.configuration = configuration;
	}

}
