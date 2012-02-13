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

	protected abstract Node retrieve(Session session, String relativePath);

	void init() {
		try {
			jcrAdminSession = jcrRepository.login();
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

	public Node proxy(Session session, String path) {
		try {
			if (session.hasPendingChanges())
				throw new ArgeoException(
						"Cannot proxy based on a session with pending changes");
			String nodePath = getNodePath(path);
			if (!session.itemExists(nodePath)) {
				Node nodeT = retrieveAndSave(path);
				if (nodeT == null)
					return null;
			}
			return session.getNode(nodePath);
		} catch (RepositoryException e) {
			JcrUtils.discardQuietly(jcrAdminSession);
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
		}
	}

	/** Session is not saved */
	protected Node proxyUrl(Session session, String baseUrl, String path)
			throws RepositoryException {
		String nodePath = getNodePath(path);
		if (jcrAdminSession.itemExists(nodePath))
			throw new ArgeoException("Node " + nodePath + " already exists");
		Node node = null;
		String remoteUrl = baseUrl + path;
		InputStream in = null;
		try {
			URL u = new URL(remoteUrl);
			in = u.openStream();
			node = importFile(session, nodePath, in);
		} catch (IOException e) {
			if (log.isTraceEnabled()) {
				log.trace("Cannot read " + remoteUrl + ", skipping... "
						+ e.getMessage());
				// log.trace("Cannot read because of ", e);
			}
			JcrUtils.discardQuietly(session);
		} finally {
			IOUtils.closeQuietly(in);
		}
		return node;
	}

	protected Node importFile(Session session, String nodePath, InputStream in)
			throws RepositoryException {
		// FIXME allow parallel proxying
		Binary binary = null;
		try {
			Node node = JcrUtils.mkdirs(jcrAdminSession, nodePath,
					NodeType.NT_FILE, NodeType.NT_FOLDER, false);
			Node content = node.addNode(Node.JCR_CONTENT, NodeType.NT_RESOURCE);
			binary = session.getValueFactory().createBinary(in);
			content.setProperty(Property.JCR_DATA, binary);
			return node;
		} finally {
			JcrUtils.closeQuietly(binary);
		}
	}

	public void setJcrRepository(Repository jcrRepository) {
		this.jcrRepository = jcrRepository;
	}

}
