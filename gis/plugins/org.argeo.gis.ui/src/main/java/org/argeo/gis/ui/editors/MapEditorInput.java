package org.argeo.gis.ui.editors;

import javax.jcr.Node;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public class MapEditorInput implements IEditorInput {
	private final Node context;

	public MapEditorInput(Node mapContext) {
		this.context = mapContext;
	}

	public Node getContext() {
		return context;
	}

	public String getName() {
		return context.toString();
	}

	public String getToolTipText() {
		return context.toString();
	}

	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		if (Node.class.isAssignableFrom(adapter))
			return context;
		return null;
	}

	public boolean exists() {
		return false;
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public IPersistableElement getPersistable() {
		return null;
	}

}
