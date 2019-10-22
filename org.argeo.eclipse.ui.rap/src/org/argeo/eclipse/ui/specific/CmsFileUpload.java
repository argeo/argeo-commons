package org.argeo.eclipse.ui.specific;

import org.eclipse.rap.rwt.widgets.FileUpload;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;

public class CmsFileUpload extends FileUpload {
	private static final long serialVersionUID = 8027963992680980549L;

	public CmsFileUpload(Composite parent, int style) {
		super(parent, style);
	}

	@Override
	public void setText(String text) {
		super.setText(text);
	}

	@Override
	public String getFileName() {
		return super.getFileName();
	}

	@Override
	public String[] getFileNames() {
		return super.getFileNames();
	}

	@Override
	public void addSelectionListener(SelectionListener listener) {
		super.addSelectionListener(listener);
	}

}
