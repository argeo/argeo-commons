package org.argeo.eclipse.ui.specific;

import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

public class CmsFileDialog extends FileDialog {
	public CmsFileDialog(Shell parent, int style) {
		super(parent, style);
	}

	public CmsFileDialog(Shell parent) {
		super(parent);
	}

}
