package org.argeo.sandbox.ui.rap.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

public class TestDownloadView extends ViewPart {
	public final static String ID = "org.argeo.sandbox.ui.testDownloadView";

	@Override
	public void createPartControl(Composite parent) {
		Label label = new Label(parent, SWT.None);
		label.setText("Test dowload");
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
	}

}
