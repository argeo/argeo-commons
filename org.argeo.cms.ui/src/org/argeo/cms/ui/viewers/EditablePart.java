package org.argeo.cms.ui.viewers;

import org.eclipse.swt.widgets.Control;

public interface EditablePart {
	public void startEditing();

	public void stopEditing();

	public Control getControl();
}
