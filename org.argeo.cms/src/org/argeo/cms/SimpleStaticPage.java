package org.argeo.cms;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class SimpleStaticPage implements CmsUiProvider {
	private String text;

	@Override
	public Control createUi(Composite parent, Node context)
			throws RepositoryException {
		Label textC = new Label(parent,  SWT.WRAP);
		textC.setData(RWT.CUSTOM_VARIANT, CmsStyles.CMS_STATIC_TEXT);
		textC.setData(RWT.MARKUP_ENABLED, Boolean.TRUE);
		textC.setText(text);
		
		return textC;
	}

	public void setText(String text) {
		this.text = text;
	}

}
