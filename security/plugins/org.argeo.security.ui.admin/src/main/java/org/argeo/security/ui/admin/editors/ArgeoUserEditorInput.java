package org.argeo.security.ui.admin.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/** Editor input for an Argeo user. */
public class ArgeoUserEditorInput implements IEditorInput {
	private final String username;

	public ArgeoUserEditorInput(String username) {
		this.username = username;
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

}
