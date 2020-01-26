package org.argeo.cms.internal.kernel;

import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitValue;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.argeo.jcr.JcrUtils;

class JackrabbitLocalRepository extends LocalRepository {
	private final static Log log = LogFactory.getLog(JackrabbitLocalRepository.class);
	private final static String MIX_ETAG = "mix:etag";
	private final static String JCR_ETAG = "jcr:etag";

	private Map<String, WorkspaceMonitor> workspaceMonitors = new TreeMap<>();

	public JackrabbitLocalRepository(RepositoryImpl repository, String cn) {
		super(repository, cn);
		Session session = KernelUtils.openAdminSession(repository);
		try {
			for (String workspaceName : session.getWorkspace().getAccessibleWorkspaceNames()) {
				addMonitor(workspaceName);
			}
		} catch (RepositoryException e) {
			throw new IllegalStateException(e);
		} finally {
			JcrUtils.logoutQuietly(session);
		}
	}

	protected RepositoryImpl getJackrabbitrepository(String workspaceName) {
		return (RepositoryImpl) getRepository(workspaceName);
	}

	@Override
	protected synchronized void processNewSession(Session session, String workspaceName) {
		String realWorkspaceName = session.getWorkspace().getName();
		addMonitor(realWorkspaceName);
	}

	private void addMonitor(String realWorkspaceName) {
		if (!workspaceMonitors.containsKey(realWorkspaceName)) {
			try {
				WorkspaceMonitor workspaceMonitor = new WorkspaceMonitor(getJackrabbitrepository(realWorkspaceName),
						getCn(), realWorkspaceName);
				workspaceMonitors.put(realWorkspaceName, workspaceMonitor);
				workspaceMonitor.start();
				if (log.isDebugEnabled())
					log.debug("Registered " + workspaceMonitor.getName());
			} catch (RepositoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	static String toEtag(Value v) {
		JackrabbitValue value = (JackrabbitValue) v;
		return '\"' + value.getContentIdentity() + '\"';

	}

	/** recursive */
	static void setLastModified(Node node, Event event) throws RepositoryException {
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTimeInMillis(event.getDate());
		if (node.isNodeType(NodeType.NT_FOLDER)) {
			node.addMixin(NodeType.MIX_LAST_MODIFIED);
		}
		if (node.isNodeType(NodeType.MIX_LAST_MODIFIED)) {
			node.setProperty(Property.JCR_LAST_MODIFIED, calendar);
			node.setProperty(Property.JCR_LAST_MODIFIED_BY, event.getUserID());
		}
		if (node.getDepth() == 0)
			return;
		Node parent = node.getParent();
		setLastModified(parent, event);
	}

	static class WorkspaceMonitor extends Thread implements EventListener {
		String workspaceName;
		RepositoryImpl repositoryImpl;
		Session session;

		public WorkspaceMonitor(RepositoryImpl repositoryImpl, String cn, String workspaceName)
				throws RepositoryException {
			super("Monitor workspace " + workspaceName + " of repository " + cn);
			this.workspaceName = workspaceName;
			this.repositoryImpl = repositoryImpl;
		}

		public void run() {

			session = KernelUtils.openAdminSession(repositoryImpl, workspaceName);
			try {
				String[] nodeTypes = { NodeType.NT_FILE, NodeType.MIX_LAST_MODIFIED };
				session.getWorkspace().getObservationManager().addEventListener(this,
						Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_CHANGED, "/", true, null, nodeTypes,
						true);
				while (save()) {

				}

			} catch (RepositoryException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} finally {
				JcrUtils.logoutQuietly(session);
			}
		}

		protected synchronized boolean save() {
			try {
				wait(100);
			} catch (InterruptedException e) {
				// silent
			}
			if (!session.isLive())
				return false;
			try {
				if (session.hasPendingChanges())
					session.save();
			} catch (RepositoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		}

		@Override
		public synchronized void onEvent(EventIterator events) {
			events: while (events.hasNext()) {
				Event event = events.nextEvent();
//				if (log.isDebugEnabled())
//					log.debug(event);
				try {
					if (event.getType() == Event.NODE_ADDED) {
						Node node = session.getNode(event.getPath());
						if (node.getParent().isNodeType(NodeType.NT_FILE)) {
//							Node contentNode = node.getNode(Node.JCR_CONTENT);
							node.addMixin(NodeType.MIX_LAST_MODIFIED);
							Property property = node.getProperty(Property.JCR_DATA);
							String etag = toEtag(property.getValue());
							node.setProperty(JCR_ETAG, etag);
							setLastModified(node, event);
//						node.getSession().save();
							if (log.isDebugEnabled())
								log.debug("Node " + node.getPath() + ": " + event);
						}
					} else if (event.getType() == Event.NODE_REMOVED) {
						String parentPath = JcrUtils.parentPath(event.getPath());
						try {
							Node parent = session.getNode(parentPath);
							setLastModified(parent, event);

							if (log.isDebugEnabled())
								log.debug("Node removed from " + parent.getPath() + ": " + event);
						} catch (ItemNotFoundException | PathNotFoundException e) {
							continue events;
						}
					} else if (event.getType() == Event.PROPERTY_CHANGED) {
						Property property = session.getProperty(event.getPath());
						if (property.getName().equals("jcr:lastModified"))
							continue events;
						if (property.getType() == PropertyType.BINARY && property.getName().equals("jcr:data")
								&& property.getParent().isNodeType(NodeType.NT_UNSTRUCTURED)) {
							String etag = toEtag(property.getValue());
							property.getParent().setProperty(JCR_ETAG, etag);
						}
						setLastModified(property.getParent(), event);
//						property.getParent().getSession().save();
						if (log.isDebugEnabled())
							log.debug("Property " + property.getPath() + ": " + event);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			notifyAll();

		}

	}
}
