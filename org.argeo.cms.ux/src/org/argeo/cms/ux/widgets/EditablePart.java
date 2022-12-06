package org.argeo.cms.ux.widgets;

/** Manages whether an editable or non editable control is shown. */
public interface EditablePart {
	public void startEditing();

	public void stopEditing();
}
