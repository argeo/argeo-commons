package org.argeo.cms.swt;

import org.argeo.cms.ux.widgets.EditablePart;
import org.eclipse.swt.widgets.Control;

/** Manages whether an editable or non editable control is shown. */
public interface SwtEditablePart extends EditablePart {
	public Control getControl();
}
