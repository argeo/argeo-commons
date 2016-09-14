/*
 * Copyright (C) 2007-2012 Argeo GmbH
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
package org.argeo.eclipse.ui.workbench.internal.jcr.parts;

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
	// private final static Log log = LogFactory.getLog(EmptyNodePage.class);

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
