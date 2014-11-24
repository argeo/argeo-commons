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
package org.argeo.jcr.ui.explorer.wizards;

import javax.jcr.security.Privilege;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ChooseRightsPage extends WizardPage implements ModifyListener {

	// This page widget
	private Text groupNameTxt;
	private Combo authorizationCmb;

	// USABLE SHORTCUTS
	protected final static String[] validAuthType = { Privilege.JCR_READ,
			Privilege.JCR_WRITE, Privilege.JCR_ALL };

	public ChooseRightsPage(String path) {
		super("Main");
		setTitle("Add privilege to " + path);
	}

	public void createControl(Composite parent) {
		// specify subject
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		Label lbl = new Label(composite, SWT.LEAD);
		lbl.setText("Group or user name (no blank, no special chars)");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		groupNameTxt = new Text(composite, SWT.LEAD | SWT.BORDER);
		groupNameTxt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false));
		if (groupNameTxt != null)
			groupNameTxt.addModifyListener(this);

		// Choose rigths
		new Label(composite, SWT.NONE).setText("Choose corresponding rights");
		authorizationCmb = new Combo(composite, SWT.BORDER | SWT.V_SCROLL);
		authorizationCmb.setItems(validAuthType);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		authorizationCmb.setLayoutData(gd);

		authorizationCmb.select(0);

		// Compulsory
		setControl(composite);
	}

	protected String getGroupName() {
		return groupNameTxt.getText();
	}

	protected String getAuthTypeStr() {
		return authorizationCmb.getItem(authorizationCmb.getSelectionIndex());
	}

	public void modifyText(ModifyEvent event) {
		String message = checkComplete();
		if (message != null)
			setMessage(message, WizardPage.ERROR);
		else {
			setMessage("Complete", WizardPage.INFORMATION);
			setPageComplete(true);
		}
	}

	/** @return error message or null if complete */
	protected String checkComplete() {
		String groupStr = groupNameTxt.getText();
		if (groupStr == null || "".equals(groupStr))
			return "Please enter the name of the corresponding group.";
		// Remove regexp check for the time being.
		// else if (!match(groupStr))
		// return
		// "Please use only alphanumerical chars for the short technical name.";
		return null;
	}
}
