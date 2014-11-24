package org.argeo.cms;

import javax.jcr.Node;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

public class CmsInstallPage implements CmsUiProvider {
	private Text text;

	@Override
	public Control createUi(Composite parent, Node context) {
		text = new Text(parent, SWT.MULTI);
		text.setData(RWT.CUSTOM_VARIANT, "cms_install");
		return text;
	}

}
