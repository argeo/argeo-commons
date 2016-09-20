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
package org.argeo.cms.ui.workbench.internal.jcr.commands;

import java.security.Principal;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.argeo.cms.ui.workbench.WorkbenchUiPlugin;
import org.argeo.cms.ui.workbench.internal.jcr.model.SingleJcrNodeElem;
import org.argeo.cms.ui.workbench.internal.jcr.model.WorkspaceElem;
import org.argeo.cms.ui.workbench.jcr.JcrImages;
import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.eclipse.ui.dialogs.ErrorFeedback;
import org.argeo.jcr.JcrUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/** Open a dialog to remove privileges from the selected node */
public class RemovePrivileges extends AbstractHandler {
	public final static String ID = WorkbenchUiPlugin.PLUGIN_ID
			+ ".removePrivileges";

	public Object execute(ExecutionEvent event) throws ExecutionException {

		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage().getSelection();
		if (selection != null && !selection.isEmpty()
				&& selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			TreeParent uiNode = null;
			Node jcrNode = null;

			if (obj instanceof SingleJcrNodeElem) {
				uiNode = (TreeParent) obj;
				jcrNode = ((SingleJcrNodeElem) uiNode).getNode();
			} else if (obj instanceof WorkspaceElem) {
				uiNode = (TreeParent) obj;
				jcrNode = ((WorkspaceElem) uiNode).getRootNode();
			} else
				return null;

			try {
				String targetPath = jcrNode.getPath();
				Dialog dialog = new RemovePrivDialog(
						HandlerUtil.getActiveShell(event),
						jcrNode.getSession(), targetPath);
				dialog.open();
				return null;
			} catch (RepositoryException re) {
				throw new EclipseUiException("Unable to retrieve "
						+ "path or JCR session to add privilege on " + jcrNode,
						re);
			}
		} else {
			ErrorFeedback.show("Cannot add privileges");
		}
		return null;
	}

	private class RemovePrivDialog extends TitleAreaDialog {
		private static final long serialVersionUID = 280139710002698692L;

		private Composite body;

		private final String path;
		private final Session session;

		public RemovePrivDialog(Shell parentShell, Session session, String path) {
			super(parentShell);
			this.session = session;
			this.path = path;
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Remove privileges");
		}

		protected Control createDialogArea(Composite parent) {
			Composite dialogarea = (Composite) super.createDialogArea(parent);
			dialogarea.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, true,
					true));
			body = new Composite(dialogarea, SWT.NONE);
			body.setLayoutData(EclipseUiUtils.fillAll());
			refreshContent();
			parent.pack();
			return body;
		}

		private void refreshContent() {
			EclipseUiUtils.clear(body);
			try {
				AccessControlManager acm = session.getAccessControlManager();
				AccessControlList acl = JcrUtils
						.getAccessControlList(acm, path);
				if (acl == null || acl.getAccessControlEntries().length <= 0)
					setMessage("No privilege are defined on this node",
							IMessageProvider.INFORMATION);
				else {
					body.setLayout(new GridLayout(3, false));
					for (AccessControlEntry ace : acl.getAccessControlEntries()) {
						addOnePrivRow(body, ace);
					}
					setMessage("Remove some of the defined privileges",
							IMessageProvider.INFORMATION);
				}
			} catch (RepositoryException e) {
				throw new EclipseUiException("Unable to list privileges on "
						+ path, e);
			}
			body.layout(true, true);
		}

		private void addOnePrivRow(Composite parent, AccessControlEntry ace) {
			Principal currentPrincipal = ace.getPrincipal();
			final String currPrincipalName = currentPrincipal.getName();
			new Label(parent, SWT.WRAP).setText(currPrincipalName);
			new Label(parent, SWT.WRAP).setText(privAsString(ace
					.getPrivileges()));
			final Button rmBtn = new Button(parent, SWT.FLAT);
			rmBtn.setImage(JcrImages.REMOVE);

			rmBtn.addSelectionListener(new SelectionAdapter() {
				private static final long serialVersionUID = 7566938841363890730L;

				@Override
				public void widgetSelected(SelectionEvent e) {

					if (MessageDialog.openConfirm(rmBtn.getShell(),
							"Confirm deletion",
							"Are you sure you want to remove this privilege?")) {
						try {
							session.save();
							JcrUtils.clearAccessControList(session, path,
									currPrincipalName);
							session.save();
							refreshContent();
						} catch (RepositoryException re) {
							throw new EclipseUiException("Unable to "
									+ "remove privilege for "
									+ currPrincipalName + " on " + path, re);
						}
					}

					super.widgetSelected(e);
				}
			});

		}

		private String privAsString(Privilege[] currentPrivileges) {

			StringBuilder builder = new StringBuilder();
			builder.append("[ ");
			for (Privilege priv : currentPrivileges) {
				builder.append(priv.getName()).append(", ");
			}
			if (builder.length() > 3)
				return builder.substring(0, builder.length() - 2) + " ]";
			else
				return "[]";

		}
	}
}