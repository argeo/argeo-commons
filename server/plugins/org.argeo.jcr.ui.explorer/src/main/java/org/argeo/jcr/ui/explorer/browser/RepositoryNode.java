package org.argeo.jcr.ui.explorer.browser;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.eclipse.ui.jcr.JcrUiPlugin;
import org.eclipse.swt.graphics.Image;

public class RepositoryNode extends TreeParent {
	private final String name;
	private final Repository repository;
	private Session defaultSession = null;
	public final static Image REPOSITORY_DISCONNECTED = JcrUiPlugin
			.getImageDescriptor("icons/repository_disconnected.gif")
			.createImage();
	public final static Image REPOSITORY_CONNECTED = JcrUiPlugin
			.getImageDescriptor("icons/repository_connected.gif").createImage();

	public RepositoryNode(String name, Repository repository) {
		super(name);
		this.name = name;
		this.repository = repository;
	}

	public void login() {
		try {
//			SimpleCredentials sc = new SimpleCredentials("root",
//					"demo".toCharArray());
//			defaultSession = repository.login(sc);
			defaultSession = repository.login();
			String[] wkpNames = defaultSession.getWorkspace()
					.getAccessibleWorkspaceNames();
			for (String wkpName : wkpNames) {
				if (wkpName.equals(defaultSession.getWorkspace().getName()))
					addChild(new WorkspaceNode(repository, wkpName,
							defaultSession));
				else
					addChild(new WorkspaceNode(repository, wkpName));
			}
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot connect to repository " + name, e);
		}
	}

	public Session getDefaultSession() {
		return defaultSession;
	}

}
