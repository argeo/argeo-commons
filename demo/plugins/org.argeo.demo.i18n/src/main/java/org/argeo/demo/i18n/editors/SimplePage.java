/*
 * Copyright (C) 2007-2012 Mathieu Baudier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
