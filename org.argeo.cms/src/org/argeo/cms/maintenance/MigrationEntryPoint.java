package org.argeo.cms.maintenance;

import org.eclipse.rap.rwt.application.AbstractEntryPoint;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class MigrationEntryPoint extends AbstractEntryPoint {

	@Override
	protected void createContents(Composite parent) {
		new Label(parent, SWT.NONE).setText("Migration");
	}

	

}
