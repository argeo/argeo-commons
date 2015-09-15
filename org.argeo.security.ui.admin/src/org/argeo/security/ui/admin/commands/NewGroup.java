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
package org.argeo.security.ui.admin.commands;

import java.util.Dictionary;

import javax.transaction.UserTransaction;

import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.dialogs.ErrorFeedback;
import org.argeo.jcr.ArgeoNames;
import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.argeo.security.ui.admin.internal.UiAdminUtils;
import org.argeo.security.ui.admin.internal.UserAdminConstants;
import org.argeo.security.ui.admin.views.GroupsView;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;

/** Create a new group. */
public class NewGroup extends AbstractHandler {
	public final static String ID = SecurityAdminPlugin.PLUGIN_ID + ".newGroup";

	private UserAdmin userAdmin;
	private UserTransaction userTransaction;

	// TODO implement a dynamic choice of the base dn
	private String getDn(String cn) {
		return "cn=" + cn + ",dc=example,dc=com";
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		NewGroupWizard newGroupWizard = new NewGroupWizard();
		WizardDialog dialog = new WizardDialog(
				HandlerUtil.getActiveShell(event), newGroupWizard);
		dialog.setTitle("Create a new group");

		// Force refresh until the listener are implemented
		if (Window.OK == dialog.open())
			forceRefresh(event);
		return null;
	}

	private void forceRefresh(ExecutionEvent event) {
		IWorkbenchWindow iww = HandlerUtil.getActiveWorkbenchWindow(event);
		if (iww == null)
			return;
		IWorkbenchPage activePage = iww.getActivePage();
		IWorkbenchPart part = activePage.getActivePart();
		if (part instanceof GroupsView)
			((GroupsView) part).refresh();
	}

	private class NewGroupWizard extends Wizard {

		// pages
		private MainGroupInfoWizardPage mainGroupInfo;

		// End user fields
		private Text dNameTxt, commonNameTxt, descriptionTxt;

		public NewGroupWizard() {
		}

		@Override
		public void addPages() {
			mainGroupInfo = new MainGroupInfoWizardPage();
			addPage(mainGroupInfo);

			setWindowTitle("Create a new group");
			// mainGroupInfo.setMessage(message, WizardPage.WARNING);
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public boolean performFinish() {
			if (!canFinish())
				return false;
			String commonName = commonNameTxt.getText();
			try {
				UiAdminUtils.beginTransactionIfNeeded(userTransaction);
				Group user = (Group) userAdmin.createRole(getDn(commonName),
						Role.GROUP);
				Dictionary props = user.getProperties();
				String descStr = descriptionTxt.getText();
				if (UiAdminUtils.notNull(descStr))
					props.put(UserAdminConstants.KEY_DESC, descStr);
				return true;
			} catch (Exception e) {
				ErrorFeedback.show("Cannot create new group " + commonName, e);
				return false;
			}
		}

		private class MainGroupInfoWizardPage extends WizardPage implements
				ModifyListener, ArgeoNames {
			private static final long serialVersionUID = -3150193365151601807L;

			public MainGroupInfoWizardPage() {
				super("Main");
				setTitle("General information");
				setMessage("Please provide a common name and a free description");
			}

			@Override
			public void createControl(Composite parent) {
				Composite bodyCmp = new Composite(parent, SWT.NONE);
				bodyCmp.setLayout(new GridLayout(2, false));
				dNameTxt = EclipseUiUtils.createGridLT(bodyCmp,
						"Distinguished name", this);
				dNameTxt.setEnabled(false);
				commonNameTxt = EclipseUiUtils.createGridLT(bodyCmp,
						"Common name", this);
				commonNameTxt.addModifyListener(new ModifyListener() {
					private static final long serialVersionUID = -1435351236582736843L;

					@Override
					public void modifyText(ModifyEvent event) {
						String name = commonNameTxt.getText();
						if (name.trim().equals("")) {
							dNameTxt.setText("");
						} else {
							dNameTxt.setText(getDn(name));
						}
					}
				});

				Label descLbl = new Label(bodyCmp, SWT.LEAD);
				descLbl.setText("Description");
				descLbl.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false,
						false));
				descriptionTxt = new Text(bodyCmp, SWT.LEAD | SWT.MULTI
						| SWT.WRAP | SWT.BORDER);
				descriptionTxt.setLayoutData(EclipseUiUtils.fillAll());
				descriptionTxt.addModifyListener(this);

				setControl(bodyCmp);

				// Initialize buttons
				setPageComplete(false);
				getContainer().updateButtons();
			}

			@Override
			public void modifyText(ModifyEvent event) {
				String message = checkComplete();
				if (message != null) {
					setMessage(message, WizardPage.ERROR);
					setPageComplete(false);
				} else {
					setMessage("Complete", WizardPage.INFORMATION);
					setPageComplete(true);
				}
				getContainer().updateButtons();
			}

			/** @return error message or null if complete */
			protected String checkComplete() {
				String name = commonNameTxt.getText();

				if (name.trim().equals(""))
					return "Common name must not be empty";
				Role role = userAdmin.getRole(getDn(name));
				if (role != null)
					return "Group " + name + " already exists";
				return null;
			}

			@Override
			public void setVisible(boolean visible) {
				super.setVisible(visible);
				if (visible)
					commonNameTxt.setFocus();
			}
		}
	}

	/* DEPENDENCY INJECTION */
	public void setUserAdmin(UserAdmin userAdmin) {
		this.userAdmin = userAdmin;
	}

	public void setUserTransaction(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}
}