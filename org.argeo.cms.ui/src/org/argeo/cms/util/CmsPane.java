package org.argeo.cms.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;

/** The main pane of a CMS display, with QA and support areas. */
public class CmsPane {

	private Composite mainArea;
	private Composite qaArea;
	private Composite supportArea;

	public CmsPane(Composite parent, int style) {
		parent.setLayout(CmsUtils.noSpaceGridLayout());

		qaArea = new Composite(parent, SWT.NONE);
		qaArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		RowLayout qaLayout = new RowLayout();
		qaLayout.spacing = 0;
		qaArea.setLayout(qaLayout);

		mainArea = new Composite(parent, SWT.NONE);
		mainArea.setLayout(new GridLayout());
		mainArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		supportArea = new Composite(parent, SWT.NONE);
		supportArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		RowLayout supportLayout = new RowLayout();
		supportLayout.spacing = 0;
		supportArea.setLayout(supportLayout);
	}

	public Composite getMainArea() {
		return mainArea;
	}

	public Composite getQaArea() {
		return qaArea;
	}

	public Composite getSupportArea() {
		return supportArea;
	}

}
