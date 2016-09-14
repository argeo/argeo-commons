package org.argeo.cms.viewers;

import org.eclipse.swt.widgets.Control;

public interface EditablePart {
	public void startEditing();

	public void stopEditing();

	public Control getControl();
}
