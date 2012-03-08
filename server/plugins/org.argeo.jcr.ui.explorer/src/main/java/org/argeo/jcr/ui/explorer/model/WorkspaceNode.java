package org.argeo.jcr.ui.explorer.model;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.TreeParent;

/**
 * UI Tree component. Wraps the root node of a JCR {@link Workspace}. It also
 * keeps a reference to its parent {@link RepositoryNode}, to be able to
 * retrieve alias of the current used repository
 */
public class WorkspaceNode extends TreeParent implements EventListener, UiNode {
	private Session session = null;

	public WorkspaceNode(RepositoryNode parent, String name) {
		this(parent, name, null);
	}

	public WorkspaceNode(RepositoryNode parent, String name, Session session) {
		super(name);
		this.session = session;
		if (session != null)
			processNewSession(session);
		setParent(parent);
	}

	public Session getSession() {
		return session;
	}

	public Node getRootNode() {
		try {
			if (session != null)
				return session.getRootNode();
			else
				return null;
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot get root node of workspace "
					+ getName(), e);
		}
	}

	public void login() {
		try {
			logout();
			session = ((RepositoryNode) getParent()).repositoryLogin(getName());
			processNewSession(session);

		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot connect to repository "
					+ getName(), e);
		}
	}

	@Override
	public synchronized void dispose() {
		logout();
		super.dispose();
	}

	/** Logouts the session, does not nothing if there is no live session. */
	public void logout() {
		try {
			if (session != null && session.isLive()) {
				session.getWorkspace().getObservationManager()
						.removeEventListener(this);
				session.logout();
			}
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot connect to repository "
					+ getName(), e);
		}
	}

	/** Returns the alias of the parent Repository */
	public String getAlias() {
		return ((UiNode) getParent()).getAlias();
	}

	@Override
	public boolean hasChildren() {
		try {
			if (session == null)
				return false;
			else
				return session.getRootNode().hasNodes();
		} catch (RepositoryException re) {
			throw new ArgeoException(
					"Unexpected error while checking children node existence",
					re);
		}
	}

	/** Override normal behaviour to initialize display of the workspace */
	@Override
	public synchronized Object[] getChildren() {
		if (isLoaded()) {
			return super.getChildren();
		} else {
			// initialize current object
			try {
				Node rootNode;
				if (session == null)
					return null;
				else
					rootNode = session.getRootNode();
				NodeIterator ni = rootNode.getNodes();
				while (ni.hasNext()) {
					Node node = ni.nextNode();
					addChild(new SingleJcrNode(this, node, node.getName()));
				}
				return super.getChildren();
			} catch (RepositoryException e) {
				throw new ArgeoException(
						"Cannot initialize WorkspaceNode UI object."
								+ getName(), e);
			}
		}
	}

	public void onEvent(final EventIterator events) {
		// if (session == null)
		// return;
		// Display.getDefault().syncExec(new Runnable() {
		// public void run() {
		// while (events.hasNext()) {
		// Event event = events.nextEvent();
		// try {
		// String path = event.getPath();
		// String parentPath = path.substring(0,
		// path.lastIndexOf('/'));
		// final Object parent;
		// if (parentPath.equals("/") || parentPath.equals(""))
		// parent = this;
		// else if (session.itemExists(parentPath)){
		// parent = session.getItem(parentPath);
		// ((Item)parent).refresh(false);
		// }
		// else
		// parent = null;
		// if (parent != null) {
		// nodesViewer.refresh(parent);
		// }
		//
		// } catch (RepositoryException e) {
		// log.warn("Error processing event " + event, e);
		// }
		// }
		// }
		// });
	}

	protected void processNewSession(Session session) {
		// try {
		// ObservationManager observationManager = session.getWorkspace()
		// .getObservationManager();
		// observationManager.addEventListener(this, Event.NODE_ADDED
		// | Event.NODE_REMOVED, "/", true, null, null, false);
		// } catch (RepositoryException e) {
		// throw new ArgeoException("Cannot process new session "
		// + session, e);
		// }
	}

}
