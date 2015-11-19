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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.workbench.internal.users.UsersUtils;
import org.argeo.eclipse.ui.workbench.users.PickUpUserDialog;
import org.argeo.jcr.JcrUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
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
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/** Add JCR privileges to the chosen user group on a given node */
public class AddPrivilegeWizard extends Wizard {

	// Context
	private UserAdmin userAdmin;
	private Session currentSession;
	private String targetPath;
	// Chosen parameters
	private User chosenUser;
	private String jcrPrivilege;

	// UI Object
	private DefinePrivilegePage page;

	// TODO enable external definition of possible values and corresponding
	// description
	protected static final Map<String, String> AUTH_TYPE_LABELS;
	static {
		Map<String, String> tmpMap = new HashMap<String, String>();
		tmpMap.put(Privilege.JCR_READ, "jcr:read");
		tmpMap.put(Privilege.JCR_WRITE, "jcr:write");
		tmpMap.put(Privilege.JCR_ALL, "jcr:all");
		AUTH_TYPE_LABELS = Collections.unmodifiableMap(tmpMap);
	}

	protected static final Map<String, String> AUTH_TYPE_DESC;
	static {
		Map<String, String> tmpMap = new HashMap<String, String>();
		tmpMap.put(Privilege.JCR_READ,
				"The privilege to retrieve a node and get its properties and their values.");
		tmpMap.put(Privilege.JCR_WRITE, "An aggregate privilege that "
				+ "contains: jcr:modifyProperties, jcr:addChildNodes, "
				+ "jcr:removeNode, jcr:removeChildNodes");
		tmpMap.put(Privilege.JCR_ALL, "An aggregate privilege that "
				+ "contains all JCR predefined privileges, "
				+ "plus all implementation-defined privileges. ");
		AUTH_TYPE_DESC = Collections.unmodifiableMap(tmpMap);
	}

	public AddPrivilegeWizard(Session currentSession, String path,
			UserAdmin userAdmin) {
		super();
		this.userAdmin = userAdmin;
		this.currentSession = currentSession;
		this.targetPath = path;
	}

	@Override
	public void addPages() {
		try {
			setWindowTitle("Add privilege on " + targetPath);
			page = new DefinePrivilegePage(userAdmin, targetPath);
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
			JcrUtils.addPrivilege(currentSession, targetPath,
					chosenUser.getName(), jcrPrivilege);
		} catch (RepositoryException re) {
			throw new ArgeoException("Cannot set " + jcrPrivilege + " for "
					+ chosenUser.getName() + " on " + targetPath, re);
		}
		return true;
	}

	private class DefinePrivilegePage extends WizardPage implements
			ModifyListener {
		private static final long serialVersionUID = 8084431378762283920L;

		// Context
		final private UserAdmin userAdmin;

		public DefinePrivilegePage(UserAdmin userAdmin, String path) {
			super("Main");
			this.userAdmin = userAdmin;
			setTitle("Define the privilege to apply to " + path);
			setMessage("Please choose a user or a group and relevant JCR Privilege.");
		}

		public void createControl(Composite parent) {
			final Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout(3, false));

			// specify subject
			createBoldLabel(composite, "User or group name");
			final Label groupNameLbl = new Label(composite, SWT.LEAD);
			groupNameLbl.setLayoutData(EclipseUiUtils.fillWidth());

			Link pickUpLk = new Link(composite, SWT.LEFT);
			pickUpLk.setText(" <a>Change</a> ");

			createBoldLabel(composite, "User or group DN");
			final Text groupNameTxt = new Text(composite, SWT.LEAD | SWT.BORDER);
			groupNameTxt.setLayoutData(EclipseUiUtils.fillWidth(2));

			pickUpLk.addSelectionListener(new SelectionAdapter() {
				private static final long serialVersionUID = 1L;

				@Override
				public void widgetSelected(SelectionEvent e) {
					PickUpUserDialog dialog = new PickUpUserDialog(
							getShell(), "Choose a group or a user", userAdmin);
					if (dialog.open() == Window.OK) {
						chosenUser = dialog.getSelected();
						groupNameLbl.setText(UsersUtils
								.getCommonName(chosenUser));
						groupNameTxt.setText(chosenUser.getName());
					}
				}
			});

			groupNameTxt.addFocusListener(new FocusListener() {
				private static final long serialVersionUID = 1965498600105667738L;

				@Override
				public void focusLost(FocusEvent event) {
					String dn = groupNameTxt.getText();
					User newChosen = null;
					try {
						newChosen = (User) userAdmin.getRole(dn);
					} catch (Exception e) {
						boolean tryAgain = MessageDialog.openQuestion(
								getShell(), "Unvalid DN",
								"DN " + dn + " is not valid.\nError message: "
										+ e.getMessage()
										+ "\n\t\tDo you want to try again?");
						if (tryAgain)
							groupNameTxt.setFocus();
						else
							resetOnFail();
					}

					if (userAdmin.getRole(dn) == null) {
						boolean tryAgain = MessageDialog.openQuestion(
								getShell(), "Unexisting role", "User/group "
										+ dn + " does not exist. "
										+ "Do you want to try again?");
						if (tryAgain)
							groupNameTxt.setFocus();
						else
							resetOnFail();
					} else {
						chosenUser = newChosen;
						groupNameLbl.setText(UsersUtils
								.getCommonName(chosenUser));
					}
				}

				private void resetOnFail() {
					String oldDn = chosenUser == null ? "" : chosenUser
							.getName();
					groupNameTxt.setText(oldDn);
				}

				@Override
				public void focusGained(FocusEvent event) {
				}
			});

			// JCR Privileges
			createBoldLabel(composite, "Privilege type");
			Combo authorizationCmb = new Combo(composite, SWT.BORDER
					| SWT.READ_ONLY | SWT.V_SCROLL);
			authorizationCmb.setItems(AUTH_TYPE_LABELS.values().toArray(
					new String[0]));
			authorizationCmb.setLayoutData(EclipseUiUtils.fillWidth(2));
			createBoldLabel(composite, ""); // empty cell
			final Label descLbl = new Label(composite, SWT.WRAP);
			descLbl.setLayoutData(EclipseUiUtils.fillWidth(2));

			authorizationCmb.addSelectionListener(new SelectionAdapter() {
				private static final long serialVersionUID = 1L;

				@Override
				public void widgetSelected(SelectionEvent e) {
					String chosenPrivStr = ((Combo) e.getSource()).getText();
					if (AUTH_TYPE_LABELS.containsValue(chosenPrivStr)) {
						loop: for (String key : AUTH_TYPE_LABELS.keySet()) {
							if (AUTH_TYPE_LABELS.get(key).equals(chosenPrivStr)) {
								jcrPrivilege = key;
								break loop;
							}
						}
					}

					if (jcrPrivilege != null) {
						descLbl.setText(AUTH_TYPE_DESC.get(jcrPrivilege));
						composite.layout(true, true);
					}
				}
			});

			// Compulsory
			setControl(composite);
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
			if (chosenUser == null)
				return "Please choose a relevant group or user";
			else if (userAdmin.getRole(chosenUser.getName()) == null)
				return "Please choose a relevant group or user";
			else if (jcrPrivilege == null)
				return "Please choose a relevant JCR privilege";
			return null;
		}
	}

	private Label createBoldLabel(Composite parent, String value) {
		Label label = new Label(parent, SWT.RIGHT);
		label.setText(" " + value);
		label.setFont(EclipseUiUtils.getBoldFont(parent));
		label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		return label;
	}
}