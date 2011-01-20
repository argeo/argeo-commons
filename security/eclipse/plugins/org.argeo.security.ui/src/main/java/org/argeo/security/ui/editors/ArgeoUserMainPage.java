package org.argeo.security.ui.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.security.ArgeoUser;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ColumnLayout;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * 
 * @author bsinou
 * 
 *         This page display main info of a specified Ebi. Roles enable the
 */
public class ArgeoUserMainPage extends FormPage {
	private final static Log log = LogFactory.getLog(ArgeoUserMainPage.class);

	private ArgeoUser user;

	private Text text;
	private Combo combo;

	public ArgeoUserMainPage(FormEditor editor, ArgeoUser user) {
		super(editor, "argeoUserEditor.mainPage", "Main");
		this.user = user;
	}

	protected void createFormContent(IManagedForm managedForm) {
		try {
			ScrolledForm form = managedForm.getForm();

			// Set the title of the current form
			form.setText(user.toString());

			ColumnLayout mainLayout = new ColumnLayout();
			mainLayout.minNumColumns = 1;
			mainLayout.maxNumColumns = 4;

			mainLayout.topMargin = 0;
			mainLayout.bottomMargin = 5;
			mainLayout.leftMargin = mainLayout.rightMargin = mainLayout.horizontalSpacing = mainLayout.verticalSpacing = 10;
			form.getBody().setLayout(mainLayout);

			FormToolkit toolkit = managedForm.getToolkit();

			Composite body = toolkit.createComposite(form.getBody());
			GridLayout layout = new GridLayout();
			layout.marginWidth = layout.marginHeight = 0;
			layout.numColumns = 2;
			body.setLayout(layout);

			// Comments
			toolkit.createLabel(body, "Label1");
			text = toolkit.createText(body, user.getUsername(), SWT.WRAP
					| SWT.BORDER);

			// Project Status
			// A combo Box
			toolkit.createLabel(body, "Statut du Projet");
			// TIP : we have to create a composite to wrap the combo box that
			// cannot be handled directly by the toolkit.
			Composite subBody = toolkit.createComposite(body);

			GridLayout subLayout = new GridLayout();
			subLayout.marginWidth = 3;
			layout.numColumns = 2;
			subBody.setLayout(subLayout);

			// The subBody fills 2 columns and a row
			GridData gd;
			gd = new GridData(GridData.FILL_BOTH);
			gd.horizontalSpan = 2;
			subBody.setLayoutData(gd);

			toolkit.adapt(subBody, true, true);

			toolkit.createLabel(body, "Some more text");
			toolkit.createLabel(body, "And Again");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
