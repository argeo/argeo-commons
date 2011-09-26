package org.argeo.jcr.proxy;

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

	protected abstract String retrieve(String relativePath);

	void init() {
		try {
			jcrAdminSession = jcrRepository.login();
			beforeInitSessionSave();
			if (jcrAdminSession.hasPendingChanges())
				jcrAdminSession.save();
		} catch (RepositoryException e) {
			JcrUtils.discardQuietly(jcrAdminSession);
			throw new ArgeoException("Cannot initialize Maven proxy", e);
		}
	}

	/**
	 * Called before the (admin) session is saved at the end of the
	 * initialization. Does nothing by default, to be overridden.
	 */
	protected void beforeInitSessionSave() throws RepositoryException {
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

	public Node proxy(Session jcrSession, String path) {
		Node node;
		try {
			String nodePath = getNodePath(path);
			if (!jcrSession.itemExists(nodePath)) {
				String nodeIdentifier = retrieve(path);
				if (nodeIdentifier == null) {
					// log.warn("Could not proxy " + path);
					return null;
				} else {
					node = jcrSession.getNodeByIdentifier(nodeIdentifier);
				}
			} else {
				node = jcrSession.getNode(nodePath);
			}
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot proxy " + path, e);
		}
		return node;
	}

	protected Node proxyUrl(String baseUrl, String path) {
		Node node = null;
		String remoteUrl = baseUrl + path;
		if (log.isTraceEnabled())
			log.trace("baseUrl=" + remoteUrl);
		InputStream in = null;
		try {
			URL u = new URL(remoteUrl);
			in = u.openStream();
			node = importFile(getNodePath(path), in);
			if (log.isDebugEnabled())
				log.debug("Imported " + remoteUrl + " to " + node);
		} catch (Exception e) {
			if (log.isTraceEnabled())
				log.trace("Cannot read " + remoteUrl + ", skipping... "
						+ e.getMessage());
			if (log.isTraceEnabled()) {
				log.trace("Cannot read because of ", e);
			}
		} finally {
			IOUtils.closeQuietly(in);
		}

		return node;
	}

	protected synchronized Node importFile(String nodePath, InputStream in) {
		// FIXME allow parallel proxying
		Binary binary = null;
		try {
			Node node = JcrUtils.mkdirs(jcrAdminSession, nodePath,
					NodeType.NT_FILE, NodeType.NT_FOLDER, false);
			Node content = node.addNode(Node.JCR_CONTENT, NodeType.NT_RESOURCE);
			binary = jcrAdminSession.getValueFactory().createBinary(in);
			content.setProperty(Property.JCR_DATA, binary);
			jcrAdminSession.save();
			return node;
		} catch (RepositoryException e) {
			JcrUtils.discardQuietly(jcrAdminSession);
			throw new ArgeoException("Cannot initialize Maven proxy", e);
		} finally {
			JcrUtils.closeQuietly(binary);
		}
	}

	protected Session getJcrAdminSession() {
		return jcrAdminSession;
	}

	public void setJcrRepository(Repository jcrRepository) {
		this.jcrRepository = jcrRepository;
	}

}
