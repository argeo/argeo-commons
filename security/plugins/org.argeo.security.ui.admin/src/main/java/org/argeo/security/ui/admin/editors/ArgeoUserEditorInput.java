package org.argeo.security.ui.admin.editors;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoNames;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/** Editor input for an Argeo user. */
public class ArgeoUserEditorInput implements IEditorInput {
	private final String username;
	private final Node userHome;

	@Deprecated
	public ArgeoUserEditorInput(String username) {
		this.username = username;
		this.userHome = null;
	}

	public ArgeoUserEditorInput(Node userHome) {
		try {
			this.username = userHome.getProperty(ArgeoNames.ARGEO_USER_ID)
					.getString();
			this.userHome = userHome;
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot initialize editor input for "
					+ userHome, e);
		}
	}

	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return null;
	}

	public boolean exists() {
		return username != null;
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getName() {
		return username != null ? username : "<new user>";
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public String getToolTipText() {
		return username != null ? username : "<new user>";
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof ArgeoUserEditorInput))
			return false;
		if (((ArgeoUserEditorInput) obj).getUsername() == null)
			return false;
		return ((ArgeoUserEditorInput) obj).getUsername().equals(username);
	}

	public String getUsername() {
		return username;
	}

	public Node getUserHome() {
		return userHome;
	}

}
