package org.argeo.security.ui.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.security.ArgeoUser;
import org.argeo.security.UserNature;
import org.argeo.security.nature.SimpleUserNature;
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
public class DefaultUserMainPage extends FormPage {
	private final static Log log = LogFactory.getLog(DefaultUserMainPage.class);

	private ArgeoUser user;
	private SimpleUserNature simpleNature;

	private String simpleNatureType;

	private Text email;
	private Text description;

	public DefaultUserMainPage(FormEditor editor, ArgeoUser user) {
		super(editor, "argeoUserEditor.mainPage", "Main");
		this.user = user;

		if (simpleNatureType != null)
			simpleNature = (SimpleUserNature) user.getUserNatures().get(
					simpleNatureType);
		else
			for (UserNature userNature : user.getUserNatures().values())
				if (userNature instanceof SimpleUserNature)
					simpleNature = (SimpleUserNature) userNature;

		if (simpleNature == null)
			throw new ArgeoException("No simple user nature in user " + user);
	}

	protected void createFormContent(IManagedForm managedForm) {
		try {
			ScrolledForm form = managedForm.getForm();

			// Set the title of the current form
			form.setText(simpleNature.getFirstName() + " "
					+ simpleNature.getLastName());

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
			toolkit.createLabel(body, "Username");
			toolkit.createLabel(body, user.getUsername());
			toolkit.createLabel(body, "Email");
			email = toolkit.createText(body, simpleNature.getEmail(), SWT.WRAP
					| SWT.BORDER);
			toolkit.createLabel(body, "Description");
			description = toolkit.createText(body,
					simpleNature.getDescription(), SWT.MULTI | SWT.WRAP
							| SWT.BORDER);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setSimpleNatureType(String simpleNatureType) {
		this.simpleNatureType = simpleNatureType;
	}

}
