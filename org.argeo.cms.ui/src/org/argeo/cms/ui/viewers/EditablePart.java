package org.argeo.cms.ui.viewers;

import org.eclipse.swt.widgets.Control;

/** Manages whether an editable or non editable control is shown. */
public interface EditablePart {
	public void startEditing();

	public void stopEditing();

	public Control getControl();
}
