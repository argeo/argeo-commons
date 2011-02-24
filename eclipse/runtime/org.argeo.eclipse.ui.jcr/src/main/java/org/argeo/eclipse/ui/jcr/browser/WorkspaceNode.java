package org.argeo.eclipse.ui.jcr.browser;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.eclipse.ui.jcr.JcrUiPlugin;
import org.eclipse.swt.graphics.Image;

public class WorkspaceNode extends TreeParent implements EventListener {
	private final String name;
	private final Repository repository;
	private Session session = null;
	public final static Image WORKSPACE_DISCONNECTED = JcrUiPlugin
	.getImageDescriptor("icons/workspace_disconnected.png")
	.createImage();
	public final static Image WORKSPACE_CONNECTED = JcrUiPlugin
	.getImageDescriptor("icons/workspace_connected.png").createImage();

	public WorkspaceNode(Repository repository, String name) {
		this(repository, name, null);
	}

	public WorkspaceNode(Repository repository, String name, Session session) {
		super(name);
		this.name = name;
		this.repository = repository;
		this.session = session;
		if (session != null)
			processNewSession(session);
	}

	public Session getSession() {
		return session;
	}

	public void login() {
		try {
			logout();
			session = repository.login(name);
			processNewSession(session);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot connect to repository " + name, e);
		}
	}

	public void logout() {
		try {
			if (session != null && session.isLive()) {
				session.getWorkspace().getObservationManager()
						.removeEventListener(this);
				session.logout();
			}
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot connect to repository " + name, e);
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
