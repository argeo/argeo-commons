package org.argeo.jcr.ui.explorer.editors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * An editor input based the JCR node object.
 * */

public class GenericNodeEditorInput implements IEditorInput {
	private final Node currentNode;

	public GenericNodeEditorInput(Node currentNode) {
		this.currentNode = currentNode;
	}

	public Node getCurrentNode() {
		return currentNode;
	}

	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return null;
	}

	public boolean exists() {
		return true;
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getName() {
		try {
			return currentNode.getName();
		} catch (RepositoryException re) {
			throw new ArgeoException(
					"unexpected error while getting node name", re);
		}
	}

	public String getUid() {
		try {
			return currentNode.getIdentifier();
		} catch (RepositoryException re) {
			throw new ArgeoException("unexpected error while getting node uid",
					re);
		}
	}

	public String getToolTipText() {
		try {
			return currentNode.getPath();
		} catch (RepositoryException re) {
			throw new ArgeoException(
					"unexpected error while getting node path", re);
		}
	}

	public String getPath() {
		try {
			return currentNode.getPath();
		} catch (RepositoryException re) {
			throw new ArgeoException(
					"unexpected error while getting node path", re);
		}
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	/**
	 * equals method based on UID that is unique within a workspace and path of
	 * the node, thus 2 shared node that have same UID as defined in the spec
	 * but 2 different pathes will open two distinct editors.
	 * 
	 * TODO enhance this method to support multirepository and multiworkspace
	 * environments
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		GenericNodeEditorInput other = (GenericNodeEditorInput) obj;
		if (!getUid().equals(other.getUid()))
			return false;
		if (!getPath().equals(other.getPath()))
			return false;
		return true;
	}
}
