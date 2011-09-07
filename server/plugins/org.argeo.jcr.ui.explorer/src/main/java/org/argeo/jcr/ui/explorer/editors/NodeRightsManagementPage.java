package org.argeo.jcr.ui.explorer.editors;

import javax.jcr.Node;

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
 * This comments will be nicely fill by mbaudier in. 
 */
public class NodeRightsManagementPage extends FormPage {
	private final static Log log = LogFactory.getLog(NodeRightsManagementPage.class);


	private Node currentNode;
	public NodeRightsManagementPage(FormEditor editor, String title, Node currentNode) {
		super(editor, "NodeRightsManagementPage", title);
		this.currentNode = currentNode;
	}

	protected void createFormContent(IManagedForm managedForm) {
		try {
			ScrolledForm form = managedForm.getForm();
			GridLayout twt = new GridLayout(1, false);
			twt.marginWidth = twt.marginHeight = 0;
			form.getBody().setLayout(twt);
			Label lbl = new Label(form.getBody(), SWT.NONE);
			lbl.setText("Implement this");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
