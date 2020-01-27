package org.argeo.cms.internal.kernel;

import java.util.GregorianCalendar;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.version.VersionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitValue;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.argeo.jcr.JcrUtils;

/** Ensure consistency of files, folder and last modified nodes. */
class CmsWorkspaceIndexer implements EventListener {
	private final static Log log = LogFactory.getLog(CmsWorkspaceIndexer.class);

//	private final static String MIX_ETAG = "mix:etag";
	private final static String JCR_ETAG = "jcr:etag";
	private final static String JCR_LAST_MODIFIED = "jcr:lastModified";
	private final static String JCR_LAST_MODIFIED_BY = "jcr:lastModifiedBy";
	private final static String JCR_DATA = "jcr:data";

	private String cn;
	private String workspaceName;
	private RepositoryImpl repositoryImpl;
	private Session session;
	private VersionManager versionManager;

	public CmsWorkspaceIndexer(RepositoryImpl repositoryImpl, String cn, String workspaceName)
			throws RepositoryException {
		this.cn = cn;
		this.workspaceName = workspaceName;
		this.repositoryImpl = repositoryImpl;
	}

	public void init() {
		session = KernelUtils.openAdminSession(repositoryImpl, workspaceName);
		try {
			String[] nodeTypes = { NodeType.NT_FILE, NodeType.MIX_LAST_MODIFIED };
			session.getWorkspace().getObservationManager().addEventListener(this,
					Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_CHANGED, "/", true, null, nodeTypes, true);
			versionManager = session.getWorkspace().getVersionManager();
		} catch (RepositoryException e1) {
			throw new IllegalStateException(e1);
		}
	}

	public void destroy() {
		try {
			session.getWorkspace().getObservationManager().removeEventListener(this);
		} catch (RepositoryException e) {
			if (log.isTraceEnabled())
				log.warn("Cannot unregistered JCR event listener", e);
		} finally {
			JcrUtils.logoutQuietly(session);
		}
	}

	private synchronized void processEvents(EventIterator events) {
		while (events.hasNext()) {
			Event event = events.nextEvent();
			processEvent(event);
		}
		notifyAll();
	}

	protected synchronized void processEvent(Event event) {
		try {
			if (event.getType() == Event.NODE_ADDED) {
				if (!versionManager.isCheckedOut(event.getPath()))
					return;// ignore checked-in nodes
				session.refresh(true);
				Node node = session.getNode(event.getPath());
				if (node.getParent().isNodeType(NodeType.NT_FILE)) {
					if (node.isNodeType(NodeType.NT_UNSTRUCTURED)) {
						if (!node.isNodeType(NodeType.MIX_LAST_MODIFIED))
							node.addMixin(NodeType.MIX_LAST_MODIFIED);
						Property property = node.getProperty(Property.JCR_DATA);
						String etag = toEtag(property.getValue());
						node.setProperty(JCR_ETAG, etag);
					} else if (node.isNodeType(NodeType.NT_RESOURCE)) {
//						if (!node.isNodeType(MIX_ETAG))
//							node.addMixin(MIX_ETAG);
//						session.save();
//						Property property = node.getProperty(Property.JCR_DATA);
//						String etag = toEtag(property.getValue());
//						node.setProperty(JCR_ETAG, etag);
//						session.save();
					}
					setLastModified(node.getParent(), event);
					session.save();
					if (log.isTraceEnabled())
						log.trace("ETag and last modified added to new " + node);
				}
			} else if (event.getType() == Event.PROPERTY_CHANGED) {
				if (!session.propertyExists(event.getPath()))
					return;
				session.refresh(true);
				Property property = session.getProperty(event.getPath());
				String propertyName = property.getName();
				// skip if last modified properties are explicitly set
				if (propertyName.equals(JCR_LAST_MODIFIED))
					return;
				if (propertyName.equals(JCR_LAST_MODIFIED_BY))
					return;
				Node node = property.getParent();
				if (property.getType() == PropertyType.BINARY && propertyName.equals(JCR_DATA)
						&& node.isNodeType(NodeType.NT_UNSTRUCTURED)) {
					String etag = toEtag(property.getValue());
					node.setProperty(JCR_ETAG, etag);
				}
				setLastModified(node, event);
				session.save();
				if (log.isTraceEnabled())
					log.trace("ETag and last modified updated for " + node);
			} else if (event.getType() == Event.NODE_REMOVED) {
				String removeNodePath = event.getPath();
				String parentPath = JcrUtils.parentPath(removeNodePath);
				session.refresh(true);
				setLastModified(parentPath, event);
				session.save();
				if (log.isTraceEnabled())
					log.trace("Last modified updated for parents of removed " + removeNodePath);
			}
		} catch (Exception e) {
			if (log.isTraceEnabled())
				log.warn("Cannot process event " + event, e);
		} finally {
//			try {
//				session.refresh(true);
//				if (session.hasPendingChanges())
//					session.save();
////				session.refresh(false);
//			} catch (RepositoryException e) {
//				if (log.isTraceEnabled())
//					log.warn("Cannot refresh JCR session", e);
//			}
		}

	}

	@Override
	public void onEvent(EventIterator events) {
		Runnable toRun = new Runnable() {

			@Override
			public void run() {
				processEvents(events);
			}
		};
		Future<?> future = Activator.getInternalExecutorService().submit(toRun);
		try {
			// make the call synchronous
			future.get(60, TimeUnit.SECONDS);
		} catch (TimeoutException | ExecutionException | InterruptedException e) {
			// silent
		}
	}

	static String toEtag(Value v) {
		if (v instanceof JackrabbitValue) {
			JackrabbitValue value = (JackrabbitValue) v;
			return '\"' + value.getContentIdentity() + '\"';
		} else {
			return null;
		}

	}

	/** Recursively set the last updated time on parents. */
	protected synchronized void setLastModified(Node node, Event event) throws RepositoryException {
		if (versionManager.isCheckedOut(node.getPath())) {
			GregorianCalendar calendar = new GregorianCalendar();
			calendar.setTimeInMillis(event.getDate());
			if (node.isNodeType(NodeType.MIX_LAST_MODIFIED)) {
				node.setProperty(Property.JCR_LAST_MODIFIED, calendar);
				node.setProperty(Property.JCR_LAST_MODIFIED_BY, event.getUserID());
			}
			if (node.isNodeType(NodeType.NT_FOLDER) && !node.isNodeType(NodeType.MIX_LAST_MODIFIED)) {
				node.addMixin(NodeType.MIX_LAST_MODIFIED);
			}

			try {
				node.getSession().save();
			} catch (RepositoryException e) {
				// fail silently and keep recursing
			}
		}
		if (node.getDepth() == 0)
			return;
		Node parent = node.getParent();
		setLastModified(parent, event);
	}

	/**
	 * Recursively set the last updated time on parents. Useful to use paths when
	 * dealing with deletions.
	 */
	protected synchronized void setLastModified(String path, Event event) throws RepositoryException {
		// root node will always exist, so end condition is delegated to the other
		// recursive setLastModified method
		if (session.nodeExists(path)) {
			setLastModified(session.getNode(path), event);
		} else {
			setLastModified(JcrUtils.parentPath(path), event);
		}
	}

	@Override
	public String toString() {
		return "Indexer for workspace " + workspaceName + " of repository " + cn;
	}

}