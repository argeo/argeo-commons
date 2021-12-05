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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.jcr.JcrException;
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
			jcrAdminSession = JcrUtils.loginOrCreateWorkspace(jcrRepository, proxyWorkspace);
			beforeInitSessionSave(jcrAdminSession);
			if (jcrAdminSession.hasPendingChanges())
				jcrAdminSession.save();
		} catch (RepositoryException e) {
			JcrUtils.discardQuietly(jcrAdminSession);
			throw new JcrException("Cannot initialize URL proxy", e);
		}
	}

	/**
	 * Called before the (admin) session is saved at the end of the initialization.
	 * Does nothing by default, to be overridden.
	 */
	protected void beforeInitSessionSave(Session session) throws RepositoryException {
	}

	void destroy() {
		JcrUtils.logoutQuietly(jcrAdminSession);
	}

	/**
	 * Called before the (admin) session is logged out when resources are released.
	 * Does nothing by default, to be overridden.
	 */
	protected void beforeDestroySessionLogout() throws RepositoryException {
	}

	public Node proxy(String path) {
		// we open a JCR session with client credentials in order not to use the
		// admin session in multiple thread or make it a bottleneck.
		Node nodeAdmin = null;
		Node nodeClient = null;
		Session clientSession = null;
		try {
			clientSession = jcrRepository.login(proxyWorkspace);
			if (!clientSession.itemExists(path) || shouldUpdate(clientSession, path)) {
				nodeAdmin = retrieveAndSave(path);
				if (nodeAdmin != null)
					nodeClient = clientSession.getNode(path);
			} else
				nodeClient = clientSession.getNode(path);
			return nodeClient;
		} catch (RepositoryException e) {
			throw new JcrException("Cannot proxy " + path, e);
		} finally {
			if (nodeClient == null)
				JcrUtils.logoutQuietly(clientSession);
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
			throw new JcrException("Cannot retrieve and save " + path, e);
		} finally {
			notifyAll();
		}
	}

	/** Session is not saved */
	protected synchronized Node proxyUrl(Session session, String remoteUrl, String path) throws RepositoryException {
		Node node = null;
		if (session.itemExists(path)) {
			// throw new ArgeoJcrException("Node " + path + " already exists");
		}
		try (InputStream in = new URL(remoteUrl).openStream()) {
			// URL u = new URL(remoteUrl);
			// in = u.openStream();
			node = importFile(session, path, in);
		} catch (IOException e) {
			if (log.isDebugEnabled()) {
				log.debug("Cannot read " + remoteUrl + ", skipping... " + e.getMessage());
				// log.trace("Cannot read because of ", e);
			}
			JcrUtils.discardQuietly(session);
			// } finally {
			// IOUtils.closeQuietly(in);
		}
		return node;
	}

	protected synchronized Node importFile(Session session, String path, InputStream in) throws RepositoryException {
		Binary binary = null;
		try {
			Node content = null;
			Node node = null;
			if (!session.itemExists(path)) {
				node = JcrUtils.mkdirs(session, path, NodeType.NT_FILE, NodeType.NT_FOLDER, false);
				content = node.addNode(Node.JCR_CONTENT, NodeType.NT_UNSTRUCTURED);
			} else {
				node = session.getNode(path);
				content = node.getNode(Node.JCR_CONTENT);
			}
			binary = session.getValueFactory().createBinary(in);
			content.setProperty(Property.JCR_DATA, binary);
			JcrUtils.updateLastModifiedAndParents(node, null, true);
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
