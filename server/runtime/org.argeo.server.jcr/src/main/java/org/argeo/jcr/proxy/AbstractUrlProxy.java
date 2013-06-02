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
package org.argeo.jcr.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.jcr.JcrUtils;

/** Base class for URL based proxys. */
public abstract class AbstractUrlProxy implements ResourceProxy {
	private final static Log log = LogFactory.getLog(AbstractUrlProxy.class);

	private Repository jcrRepository;
	private Session jcrAdminSession;
	private String proxyWorkspace = "proxy";

	protected abstract Node retrieve(Session session, String path);

	void init() {
		try {
			jcrAdminSession = JcrUtils.loginOrCreateWorkspace(jcrRepository,
					proxyWorkspace);
			beforeInitSessionSave(jcrAdminSession);
			if (jcrAdminSession.hasPendingChanges())
				jcrAdminSession.save();
		} catch (Exception e) {
			JcrUtils.discardQuietly(jcrAdminSession);
			throw new ArgeoException("Cannot initialize Maven proxy", e);
		}
	}

	/**
	 * Called before the (admin) session is saved at the end of the
	 * initialization. Does nothing by default, to be overridden.
	 */
	protected void beforeInitSessionSave(Session session)
			throws RepositoryException {
	}

	void destroy() {
		JcrUtils.logoutQuietly(jcrAdminSession);
	}

	/**
	 * Called before the (admin) session is logged out when resources are
	 * released. Does nothing by default, to be overridden.
	 */
	protected void beforeDestroySessionLogout() throws RepositoryException {
	}

	public Node proxy(String path) {
		// we open a JCR session with client credentials in order not to use the
		// admin session in multiple thread or make it a bottleneck.
		Session clientSession = null;
		try {
			clientSession = jcrRepository.login(proxyWorkspace);
			if (!clientSession.itemExists(path)
					|| shouldUpdate(clientSession, path)) {
				Node nodeT = retrieveAndSave(path);
				if (nodeT == null)
					return null;
			}
			return clientSession.getNode(path);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot proxy " + path, e);
		}
	}

	protected synchronized Node retrieveAndSave(String path) {
		try {
			Node node = retrieve(jcrAdminSession, path);
			if (node == null)
				return null;
			jcrAdminSession.save();
			return node;
		} catch (RepositoryException e) {
			JcrUtils.discardQuietly(jcrAdminSession);
			throw new ArgeoException("Cannot retrieve and save " + path, e);
		} finally {
			notifyAll();
		}
	}

	/** Session is not saved */
	protected synchronized Node proxyUrl(Session session, String remoteUrl,
			String path) throws RepositoryException {
		Node node = null;
		if (session.itemExists(path)) {
			// throw new ArgeoException("Node " + path + " already exists");
		}
		InputStream in = null;
		try {
			URL u = new URL(remoteUrl);
			in = u.openStream();
			node = importFile(session, path, in);
		} catch (IOException e) {
			if (log.isDebugEnabled()) {
				log.debug("Cannot read " + remoteUrl + ", skipping... "
						+ e.getMessage());
				// log.trace("Cannot read because of ", e);
			}
			JcrUtils.discardQuietly(session);
		} finally {
			IOUtils.closeQuietly(in);
		}
		return node;
	}

	protected synchronized Node importFile(Session session, String path,
			InputStream in) throws RepositoryException {
		Binary binary = null;
		try {
			Node content = null;
			Node node = null;
			if (!session.itemExists(path)) {
				node = JcrUtils.mkdirs(session, path, NodeType.NT_FILE,
						NodeType.NT_FOLDER, false);
				content = node.addNode(Node.JCR_CONTENT, NodeType.NT_RESOURCE);
			} else {
				node = session.getNode(path);
				content = node.getNode(Node.JCR_CONTENT);
			}
			binary = session.getValueFactory().createBinary(in);
			content.setProperty(Property.JCR_DATA, binary);
			JcrUtils.updateLastModifiedAndParents(node, null);
			return node;
		} finally {
			JcrUtils.closeQuietly(binary);
		}
	}

	/** Whether the file should be updated. */
	protected Boolean shouldUpdate(Session clientSession, String nodePath) {
		return false;
	}

	public void setJcrRepository(Repository jcrRepository) {
		this.jcrRepository = jcrRepository;
	}

	public void setProxyWorkspace(String localWorkspace) {
		this.proxyWorkspace = localWorkspace;
	}

}
