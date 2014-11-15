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
package org.argeo.security.jcr;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoJcrUtils;
import org.argeo.jcr.JcrRepositoryWrapper;
import org.argeo.security.NodeAuthenticationToken;
import org.argeo.security.SystemAuthentication;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;

/**
 * Wrapper around a remote Jackrabbit repository which allows to simplify
 * configuration and intercept some actions. It exposes itself as a
 * {@link Repository}.
 */
public class RemoteJcrRepositoryWrapper extends JcrRepositoryWrapper {
	private final static Log log = LogFactory
			.getLog(RemoteJcrRepositoryWrapper.class);

	private String uri = null;

	private RepositoryFactory repositoryFactory;

	// remote
	private Credentials remoteSystemCredentials = null;

	/**
	 * Empty constructor, {@link #init()} should be called after properties have
	 * been set
	 */
	public RemoteJcrRepositoryWrapper() {
	}

	/**
	 * Embedded constructor, calling the {@link #init()} method.
	 * 
	 * @param alias
	 *            if not null the repository will be published under this alias
	 */
	public RemoteJcrRepositoryWrapper(RepositoryFactory repositoryFactory,
			String uri, Credentials remoteSystemCredentials) {
		this.repositoryFactory = repositoryFactory;
		this.uri = uri;
		this.remoteSystemCredentials = remoteSystemCredentials;
		init();
	}

	public void init() {
		Repository repository = createJackrabbitRepository();
		setRepository(repository);
	}

	/** Actually creates the new repository. */
	protected Repository createJackrabbitRepository() {
		long begin = System.currentTimeMillis();
		try {
			if (uri == null || uri.trim().equals(""))
				throw new ArgeoException("Remote URI not set");

			Repository repository = ArgeoJcrUtils.getRepositoryByUri(
					repositoryFactory, uri);
			if (repository == null)
				throw new ArgeoException("Remote JCR repository " + uri
						+ " not found");
			double duration = ((double) (System.currentTimeMillis() - begin)) / 1000;
			log.info("Created remote JCR repository in " + duration
					+ " s from URI " + uri);
			// we assume that the data model of the remote repository has
			// been properly initialized
			return repository;
		} catch (Exception e) {
			throw new ArgeoException("Cannot create remote JCR repository "
					+ uri, e);
		}
	}

	/** Shutdown the repository */
	public void destroy() throws Exception {
		super.destroy();
	}

	/** Central login method */
	public Session login(Credentials credentials, String workspaceName)
			throws LoginException, NoSuchWorkspaceException,
			RepositoryException {

		// retrieve credentials for remote
		if (credentials == null) {
			Authentication authentication = SecurityContextHolder.getContext()
					.getAuthentication();
			if (authentication != null) {
				if (authentication instanceof UsernamePasswordAuthenticationToken) {
					UsernamePasswordAuthenticationToken upat = (UsernamePasswordAuthenticationToken) authentication;
					credentials = new SimpleCredentials(upat.getName(), upat
							.getCredentials().toString().toCharArray());
				} else if ((authentication instanceof SystemAuthentication)
						|| (authentication instanceof NodeAuthenticationToken)) {
					credentials = remoteSystemCredentials;
				}
			}
		}

		return super.login(credentials, workspaceName);
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public void setRepositoryFactory(RepositoryFactory repositoryFactory) {
		this.repositoryFactory = repositoryFactory;
	}

	public void setRemoteSystemCredentials(Credentials remoteSystemCredentials) {
		this.remoteSystemCredentials = remoteSystemCredentials;
	}

}
