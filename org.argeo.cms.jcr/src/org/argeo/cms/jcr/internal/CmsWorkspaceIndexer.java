package org.argeo.cms.jcr.internal;

import java.util.GregorianCalendar;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

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
//	private final static String JCR_LAST_MODIFIED = "jcr:lastModified";
//	private final static String JCR_LAST_MODIFIED_BY = "jcr:lastModifiedBy";
//	private final static String JCR_MIXIN_TYPES = "jcr:mixinTypes";
	private final static String JCR_DATA = "jcr:data";
	private final static String JCR_CONTENT = "jcr:data";

	private String cn;
	private String workspaceName;
	private RepositoryImpl repositoryImpl;
	private Session session;
	private VersionManager versionManager;

	private LinkedBlockingDeque<Event> toProcess = new LinkedBlockingDeque<>();
	private IndexingThread indexingThread;
	private AtomicBoolean stopping = new AtomicBoolean(false);

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
					Event.NODE_ADDED | Event.PROPERTY_CHANGED, "/", true, null, nodeTypes, true);
			versionManager = session.getWorkspace().getVersionManager();

			indexingThread = new IndexingThread();
			indexingThread.start();
		} catch (RepositoryException e1) {
			throw new IllegalStateException(e1);
		}
	}

	public void destroy() {
		stopping.set(true);
		indexingThread.interrupt();
		// TODO make it configurable
		try {
			indexingThread.join(10 * 60 * 1000);
		} catch (InterruptedException e1) {
			log.warn("Indexing thread interrupted. Will log out session.");
		}

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
		long begin = System.currentTimeMillis();
		long count = 0;
		while (events.hasNext()) {
			Event event = events.nextEvent();
			try {
				toProcess.put(event);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
//			processEvent(event);
			count++;
		}
		long duration = System.currentTimeMillis() - begin;
		if (log.isTraceEnabled())
			log.trace("Processed " + count + " events in " + duration + " ms");
		notifyAll();
	}

	protected synchronized void processEvent(Event event) {
		try {
			String eventPath = event.getPath();
			if (event.getType() == Event.NODE_ADDED) {
				if (!versionManager.isCheckedOut(eventPath))
					return;// ignore checked-in nodes
				if (log.isTraceEnabled())
					log.trace("NODE_ADDED " + eventPath);
//				session.refresh(true);
				session.refresh(false);
				Node node = session.getNode(eventPath);
				Node parentNode = node.getParent();
				if (parentNode.isNodeType(NodeType.NT_FILE)) {
					if (node.isNodeType(NodeType.NT_UNSTRUCTURED)) {
						if (!node.isNodeType(NodeType.MIX_LAST_MODIFIED))
							node.addMixin(NodeType.MIX_LAST_MODIFIED);
						Property property = node.getProperty(Property.JCR_DATA);
						String etag = toEtag(property.getValue());
						session.save();
						node.setProperty(JCR_ETAG, etag);
						if (log.isTraceEnabled())
							log.trace("ETag and last modified added to new " + node);
					} else if (node.isNodeType(NodeType.NT_RESOURCE)) {
//						if (!node.isNodeType(MIX_ETAG))
//							node.addMixin(MIX_ETAG);
//						session.save();
//						Property property = node.getProperty(Property.JCR_DATA);
//						String etag = toEtag(property.getValue());
//						node.setProperty(JCR_ETAG, etag);
//						session.save();
					}
//					setLastModifiedRecursive(parentNode, event);
//					session.save();
//					if (log.isTraceEnabled())
//						log.trace("ETag and last modified added to new " + node);
				}

//				if (node.isNodeType(NodeType.NT_FOLDER)) {
//					setLastModifiedRecursive(node, event);
//					session.save();
//					if (log.isTraceEnabled())
//						log.trace("Last modified added to new " + node);
//				}
			} else if (event.getType() == Event.PROPERTY_CHANGED) {
				String propertyName = extractItemName(eventPath);
				// skip if last modified properties are explicitly set
				if (!propertyName.equals(JCR_DATA))
					return;
//				if (propertyName.equals(JCR_LAST_MODIFIED))
//					return;
//				if (propertyName.equals(JCR_LAST_MODIFIED_BY))
//					return;
//				if (propertyName.equals(JCR_MIXIN_TYPES))
//					return;
//				if (propertyName.equals(JCR_ETAG))
//					return;

				if (log.isTraceEnabled())
					log.trace("PROPERTY_CHANGED " + eventPath);

				if (!session.propertyExists(eventPath))
					return;
				session.refresh(false);
				Property property = session.getProperty(eventPath);
				Node node = property.getParent();
				if (property.getType() == PropertyType.BINARY && propertyName.equals(JCR_DATA)
						&& node.isNodeType(NodeType.NT_UNSTRUCTURED)) {
					String etag = toEtag(property.getValue());
					node.setProperty(JCR_ETAG, etag);
					Node parentNode = node.getParent();
					if (parentNode.isNodeType(NodeType.MIX_LAST_MODIFIED)) {
						setLastModified(parentNode, event);
					}
					if (log.isTraceEnabled())
						log.trace("ETag and last modified updated for " + node);
				}
//				setLastModified(node, event);
//				session.save();
//				if (log.isTraceEnabled())
//					log.trace("ETag and last modified updated for " + node);
			} else if (event.getType() == Event.NODE_REMOVED) {
				String removeNodePath = eventPath;
				String nodeName = extractItemName(eventPath);
				if (JCR_CONTENT.equals(nodeName)) // parent is a file, deleted anyhow
					return;
				if (log.isTraceEnabled())
					log.trace("NODE_REMOVED " + eventPath);
//				String parentPath = JcrUtils.parentPath(removeNodePath);
//				session.refresh(true);
//				setLastModified(parentPath, event);
//				session.save();
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

	private String extractItemName(String path) {
		if (path == null || path.length() <= 1)
			return null;
		int lastIndex = path.lastIndexOf('/');
		if (lastIndex >= 0) {
			return path.substring(lastIndex + 1);
		} else {
			return path;
		}
	}

	@Override
	public void onEvent(EventIterator events) {
		processEvents(events);
//		Runnable toRun = new Runnable() {
//
//			@Override
//			public void run() {
//				processEvents(events);
//			}
//		};
//		Future<?> future = Activator.getInternalExecutorService().submit(toRun);
//		try {
//			// make the call synchronous
//			future.get(60, TimeUnit.SECONDS);
//		} catch (TimeoutException | ExecutionException | InterruptedException e) {
//			// silent
//		}
	}

	static String toEtag(Value v) {
		if (v instanceof JackrabbitValue) {
			JackrabbitValue value = (JackrabbitValue) v;
			return '\"' + value.getContentIdentity() + '\"';
		} else {
			return null;
		}

	}

	protected synchronized void setLastModified(Node node, Event event) throws RepositoryException {
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTimeInMillis(event.getDate());
		node.setProperty(Property.JCR_LAST_MODIFIED, calendar);
		node.setProperty(Property.JCR_LAST_MODIFIED_BY, event.getUserID());
		if (log.isTraceEnabled())
			log.trace("Last modified set on " + node);
	}

	/** Recursively set the last updated time on parents. */
	protected synchronized void setLastModifiedRecursive(Node node, Event event) throws RepositoryException {
		if (versionManager.isCheckedOut(node.getPath())) {
			if (node.isNodeType(NodeType.MIX_LAST_MODIFIED)) {
				setLastModified(node, event);
			}
			if (node.isNodeType(NodeType.NT_FOLDER) && !node.isNodeType(NodeType.MIX_LAST_MODIFIED)) {
				node.addMixin(NodeType.MIX_LAST_MODIFIED);
				if (log.isTraceEnabled())
					log.trace("Last modified mix-in added to " + node);
			}

		}

		// end condition
		if (node.getDepth() == 0) {
//			try {
//				node.getSession().save();
//			} catch (RepositoryException e) {
//				log.warn("Cannot index workspace", e);
//			}
			return;
		} else {
			Node parent = node.getParent();
			setLastModifiedRecursive(parent, event);
		}
	}

	/**
	 * Recursively set the last updated time on parents. Useful to use paths when
	 * dealing with deletions.
	 */
	protected synchronized void setLastModifiedRecursive(String path, Event event) throws RepositoryException {
		// root node will always exist, so end condition is delegated to the other
		// recursive setLastModified method
		if (session.nodeExists(path)) {
			setLastModifiedRecursive(session.getNode(path), event);
		} else {
			setLastModifiedRecursive(JcrUtils.parentPath(path), event);
		}
	}

	@Override
	public String toString() {
		return "Indexer for workspace " + workspaceName + " of repository " + cn;
	}

	class IndexingThread extends Thread {

		public IndexingThread() {
			super(CmsWorkspaceIndexer.this.toString());
			// TODO Auto-generated constructor stub
		}

		@Override
		public void run() {
			life: while (session != null && session.isLive()) {
				try {
					Event nextEvent = toProcess.take();
					processEvent(nextEvent);
				} catch (InterruptedException e) {
					// silent
					interrupted();
				}

				if (stopping.get() && toProcess.isEmpty()) {
					break life;
				}
			}
			if (log.isDebugEnabled())
				log.debug(CmsWorkspaceIndexer.this.toString() + " has shut down.");
		}

	}

}