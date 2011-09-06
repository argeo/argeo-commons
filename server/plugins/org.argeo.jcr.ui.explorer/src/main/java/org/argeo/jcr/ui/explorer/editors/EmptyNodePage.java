package org.argeo.jcr.ui.explorer.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * This page is only used at editor's creation time when current node has not
 * yet been set
 */
public class EmptyNodePage extends FormPage {
	private final static Log log = LogFactory.getLog(EmptyNodePage.class);

	public EmptyNodePage(FormEditor editor, String title) {
		super(editor, "Empty Page", title);
	}

	protected void createFormContent(IManagedForm managedForm) {
		try {
			ScrolledForm form = managedForm.getForm();
			GridLayout twt = new GridLayout(1, false);
			twt.marginWidth = twt.marginHeight = 0;
			form.getBody().setLayout(twt);
			Label lbl = new Label(form.getBody(), SWT.NONE);
			lbl.setText("Empty page");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
