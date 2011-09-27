package org.argeo.demo.i18n.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * An editor input based the object name.
 * */

public class SimpleMultitabEditorInput implements IEditorInput {

	private final String name;

	public SimpleMultitabEditorInput(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
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

	public IPersistableElement getPersistable() {
		return null;
	}

	/**
	 * equals method based on the name
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		SimpleMultitabEditorInput other = (SimpleMultitabEditorInput) obj;
		if (!getName().equals(other.getName()))
			return false;
		return true;
	}

	@Override
	public String getToolTipText() {
		return name;
	}
}
