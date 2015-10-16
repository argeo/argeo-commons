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
package org.argeo.eclipse.ui.workbench.jcr.internal.parts;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.workbench.users.PickUpGroupDialog;
import org.argeo.jcr.JcrUtils;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.useradmin.UserAdmin;

/** Add Jcr privileges to the chosen user group on a given node */
public class ChangeRightsWizard extends Wizard {

	private UserAdmin userAdmin;
	private Session currentSession;
	private String path;

	// This page widget
	private DefinePrivilegePage page;

	// USABLE SHORTCUTS
	protected final static String[] validAuthType = { Privilege.JCR_READ,
			Privilege.JCR_WRITE, Privilege.JCR_ALL };

	public ChangeRightsWizard(Session currentSession, String path,
			UserAdmin userAdmin) {
		super();
		this.userAdmin = userAdmin;
		this.currentSession = currentSession;
		this.path = path;
	}

	@Override
	public void addPages() {
		try {
			page = new DefinePrivilegePage(userAdmin, path);
			addPage(page);
		} catch (Exception e) {
			throw new ArgeoException("Cannot add page to wizard ", e);
		}
	}

	@Override
	public boolean performFinish() {
		if (!canFinish())
			return false;
		try {
			JcrUtils.addPrivilege(currentSession, path, page.getGroupName(),
					page.getAuthTypeStr());
		} catch (RepositoryException re) {
			throw new ArgeoException(
					"Unexpected error while setting privileges", re);
		}
		return true;
	}

	private class DefinePrivilegePage extends WizardPage implements ModifyListener {
		private static final long serialVersionUID = 8084431378762283920L;

		// Context
		final private UserAdmin userAdmin;

		// This page widget
		private Text groupNameTxt;
		private Combo authorizationCmb;

		public DefinePrivilegePage(UserAdmin userAdmin, String path) {
			super("Main");
			this.userAdmin = userAdmin;
			setTitle("Define the privilege to apply to " + path);
		}

		public void createControl(Composite parent) {
			// specify subject
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout(3, false));
			Label lbl = new Label(composite, SWT.LEAD);
			lbl.setText("Group name");
			lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			groupNameTxt = new Text(composite, SWT.LEAD | SWT.BORDER);
			groupNameTxt.setLayoutData(EclipseUiUtils.fillWidth());
			if (groupNameTxt != null)
				groupNameTxt.addModifyListener(this);

			Link pickUpLk = new Link(composite, SWT.LEFT);
			pickUpLk.setText(" <a>Pick up</a> ");
			pickUpLk.addSelectionListener(new SelectionAdapter() {
				private static final long serialVersionUID = 1L;

				@Override
				public void widgetSelected(SelectionEvent e) {
					PickUpGroupDialog dialog = new PickUpGroupDialog(
							getShell(), "Choose a group", userAdmin);
					if (dialog.open() == Window.OK)
						groupNameTxt.setText(dialog.getSelected());
				}

			});

			// Choose rigths
			new Label(composite, SWT.NONE)
					.setText("Choose corresponding rights");
			authorizationCmb = new Combo(composite, SWT.BORDER | SWT.V_SCROLL);
			authorizationCmb.setItems(validAuthType);
			authorizationCmb.setLayoutData(EclipseUiUtils.fillWidth(2));
			authorizationCmb.select(0);

			// Compulsory
			setControl(composite);
		}

		protected String getGroupName() {
			return groupNameTxt.getText();
		}

		protected String getAuthTypeStr() {
			return authorizationCmb.getItem(authorizationCmb
					.getSelectionIndex());
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
			return null;
		}
	}
}