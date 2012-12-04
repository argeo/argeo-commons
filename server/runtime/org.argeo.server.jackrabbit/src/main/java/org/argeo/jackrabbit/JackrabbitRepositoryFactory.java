/*
 * Copyright (C) 2007-2012 Argeo GmbH
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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.RepositoryConfigurationParser;
import org.apache.jackrabbit.jcr2dav.Jcr2davRepositoryFactory;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.DefaultRepositoryFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.xml.sax.InputSource;

/**
 * Repository factory which can create new repositories and access remote
 * Jackrabbit repositories
 */
public class JackrabbitRepositoryFactory extends DefaultRepositoryFactory
		implements RepositoryFactory, ArgeoJcrConstants {

	private final static Log log = LogFactory
			.getLog(JackrabbitRepositoryFactory.class);

	private Resource fileRepositoryConfiguration = new ClassPathResource(
			"/org/argeo/jackrabbit/repository-h2.xml");

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Repository getRepository(Map parameters) throws RepositoryException {
		// check if can be found by alias
		Repository repository = super.getRepository(parameters);
		if (repository != null)
			return repository;

		// check if remote
		String uri = null;
		if (parameters.containsKey(JCR_REPOSITORY_URI))
			uri = parameters.get(JCR_REPOSITORY_URI).toString();
		else if (parameters.containsKey(JcrUtils.REPOSITORY_URI))
			uri = parameters.get(JcrUtils.REPOSITORY_URI).toString();

		if (uri != null) {
			if (uri.startsWith("http"))// http, https
				repository = createRemoteRepository(uri);
			else if (uri.startsWith("file"))// http, https
				repository = createFileRepository(uri, parameters);
			else if (uri.startsWith("vm")) {
				log.warn("URI "
						+ uri
						+ " should have been managed by generic JCR repository factory");
				repository = getRepositoryByAlias(getAliasFromURI(uri));
			}
		}

		// publish under alias
		if (parameters.containsKey(JCR_REPOSITORY_ALIAS)) {
			Properties properties = new Properties();
			properties.putAll(parameters);
			String alias = parameters.get(JCR_REPOSITORY_ALIAS).toString();
			publish(alias, repository, properties);
			log.info("Registered JCR repository under alias '" + alias
					+ "' with properties " + properties);
		}

		return repository;
	}

	protected Repository createRemoteRepository(String uri)
			throws RepositoryException {
		Map<String, String> params = new HashMap<String, String>();
		params.put(JcrUtils.REPOSITORY_URI, uri);
		Repository repository = new Jcr2davRepositoryFactory()
				.getRepository(params);
		if (repository == null)
			throw new ArgeoException("Remote Davex repository " + uri
					+ " not found");
		log.info("Initialized remote Jackrabbit repository from uri " + uri);
		return repository;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Repository createFileRepository(final String uri, Map parameters)
			throws RepositoryException {
		InputStream configurationIn = null;
		try {
			Properties vars = new Properties();
			vars.putAll(parameters);
			String dirPath = uri.substring("file:".length());
			File homeDir = new File(dirPath);
			if (homeDir.exists() && !homeDir.isDirectory())
				throw new ArgeoException("Repository home " + dirPath
						+ " is not a directory");
			if (!homeDir.exists())
				homeDir.mkdirs();
			configurationIn = fileRepositoryConfiguration.getInputStream();
			vars.put(RepositoryConfigurationParser.REPOSITORY_HOME_VARIABLE,
					homeDir.getCanonicalPath());
			RepositoryConfig repositoryConfig = RepositoryConfig.create(
					new InputSource(configurationIn), vars);

			// TransientRepository repository = new
			// TransientRepository(repositoryConfig);
			final RepositoryImpl repository = RepositoryImpl
					.create(repositoryConfig);
			Session session = repository.login();
			// FIXME make it generic
			org.argeo.jcr.JcrUtils.addPrivilege(session, "/", "ROLE_ADMIN",
					"jcr:all");
			org.argeo.jcr.JcrUtils.logoutQuietly(session);
			Runtime.getRuntime().addShutdownHook(
					new Thread("Clean JCR repository " + uri) {
						public void run() {
							repository.shutdown();
							log.info("Destroyed repository " + uri);
						}
					});
			log.info("Initialized file Jackrabbit repository from uri " + uri);
			return repository;
		} catch (Exception e) {
			throw new ArgeoException("Cannot create repository " + uri, e);
		} finally {
			IOUtils.closeQuietly(configurationIn);
		}
	}

	/**
	 * Called after the repository has been initialised. Does nothing by
	 * default.
	 */
	@SuppressWarnings("rawtypes")
	protected void postInitialization(Repository repository, Map parameters) {

	}

	public void setFileRepositoryConfiguration(
			Resource fileRepositoryConfiguration) {
		this.fileRepositoryConfiguration = fileRepositoryConfiguration;
	}

}
