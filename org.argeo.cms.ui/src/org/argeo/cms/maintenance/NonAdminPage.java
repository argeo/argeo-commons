package org.argeo.cms.maintenance;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.cms.CmsUiProvider;
import org.argeo.cms.util.CmsUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class NonAdminPage implements CmsUiProvider{

	@Override
	public Control createUi(Composite parent, Node context)
			throws RepositoryException {
		Composite body = new Composite(parent, SWT.NO_FOCUS);
		body.setLayoutData(CmsUtils.fillAll());
		body.setLayout(new GridLayout());
		Label label = new Label(body, SWT.NONE);
		label.setText("You should be an admin to perform maintenance operations. "
				+ "Are you sure you are logged in?");
		label.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		return null;
	}
	
}
