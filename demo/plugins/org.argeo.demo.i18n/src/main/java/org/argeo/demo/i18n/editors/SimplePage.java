package org.argeo.demo.i18n.editors;

import org.argeo.demo.i18n.I18nDemoMessages;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * Main node editor page. Lists all properties of the current node and enable
 * access and editing for some of them.
 */

public class SimplePage extends FormPage {
	// private final static Log log = LogFactory.getLog(SimplePage.class);

	// Utils
	// protected DateFormat timeFormatter = new
	// SimpleDateFormat(DATE_TIME_FORMAT);

	// This page widgets
	private FormToolkit tk;

	// private List<Control> modifyableProperties = new ArrayList<Control>();

	public SimplePage(FormEditor editor, String title) {
		super(editor, "id", title);
	}

	protected void createFormContent(IManagedForm managedForm) {
		tk = managedForm.getToolkit();
		ScrolledForm form = managedForm.getForm();
		GridLayout twt = new GridLayout(3, false);
		twt.marginWidth = twt.marginHeight = 5;

		form.getBody().setLayout(twt);
		createPropertiesPart(form.getBody());
	}

	private void createPropertiesPart(Composite parent) {
		// Initializes form part
		tk.createLabel(parent, I18nDemoMessages.get().SimplePage_DescriptionTxt);
		AbstractFormPart part = new AbstractFormPart() {
			public void commit(boolean onSave) {
				if (onSave) {

					// We only commit when onSave = true,
					// thus it is still possible to save after a tab
					// change.
					super.commit(onSave);
				}
			}
		};

		getManagedForm().addPart(part);

	}

	//
	// LISTENERS
	//
	/*
	 * UNUSED FOR THE TIME BEING private class ModifiedFieldListener implements
	 * ModifyListener {
	 * 
	 * private AbstractFormPart formPart;
	 * 
	 * public ModifiedFieldListener(AbstractFormPart generalPart) {
	 * this.formPart = generalPart; }
	 * 
	 * public void modifyText(ModifyEvent e) { formPart.markDirty(); } }
	 */
}
