package org.argeo.eclipse.ui.specific;

import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

public class CmsFileDialog extends FileDialog {
	private static final long serialVersionUID = -7540791204102318801L;

	public CmsFileDialog(Shell parent, int style) {
		super(parent, style);
	}

	public CmsFileDialog(Shell parent) {
		super(parent);
	}

}
