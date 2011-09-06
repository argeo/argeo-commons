package org.argeo.eclipse.ui.jcr.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * A canonical editor input based on a path to a node. In a multirepository
 * environment, path can be enriched with Repository Alias and workspace
 */

public class NodeEditorInput implements IEditorInput {
	private final String path;

	public NodeEditorInput(String path) {
		this.path = path;
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
		return path;
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public String getToolTipText() {
		return path;
	}

	public String getPath() {
		return path;
	}
}
