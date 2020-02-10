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
package org.argeo.cms.e4.jcr.handlers;

import java.net.URI;
import java.util.Hashtable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.argeo.cms.ArgeoNames;
import org.argeo.cms.ArgeoTypes;
import org.argeo.cms.e4.jcr.JcrBrowserView;
import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.dialogs.ErrorFeedback;
import org.argeo.jcr.JcrUtils;
import org.argeo.node.NodeConstants;
import org.argeo.node.NodeUtils;
import org.argeo.node.security.Keyring;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Connect to a remote repository and, if successful publish it as an OSGi
 * service.
 */
public class AddRemoteRepository {

	@Inject
	private RepositoryFactory repositoryFactory;
	@Inject
	private Repository nodeRepository;
	@Inject
	@Optional
	private Keyring keyring;

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part) {
		JcrBrowserView view = (JcrBrowserView) part.getObject();
		RemoteRepositoryLoginDialog dlg = new RemoteRepositoryLoginDialog(Display.getDefault().getActiveShell());
		if (dlg.open() == Dialog.OK) {
			view.refresh(null);
		}
	}

	// public void setRepositoryFactory(RepositoryFactory repositoryFactory) {
	// this.repositoryFactory = repositoryFactory;
	// }
	//
	// public void setKeyring(Keyring keyring) {
	// this.keyring = keyring;
	// }
	//
	// public void setNodeRepository(Repository nodeRepository) {
	// this.nodeRepository = nodeRepository;
	// }

	class RemoteRepositoryLoginDialog extends TitleAreaDialog {
		private static final long serialVersionUID = 2234006887750103399L;
		private Text name;
		private Text uri;
		private Text username;
		private Text password;
		private Button saveInKeyring;

		public RemoteRepositoryLoginDialog(Shell parentShell) {
			super(parentShell);
		}

		protected Point getInitialSize() {
			return new Point(600, 400);
		}

		protected Control createDialogArea(Composite parent) {
			Composite dialogarea = (Composite) super.createDialogArea(parent);
			dialogarea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			Composite composite = new Composite(dialogarea, SWT.NONE);
			composite.setLayout(new GridLayout(2, false));
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			setMessage("Login to remote repository", IMessageProvider.NONE);
			name = createLT(composite, "Name", "remoteRepository");
			uri = createLT(composite, "URI", "http://localhost:7070/jcr/node");
			username = createLT(composite, "User", "");
			password = createLP(composite, "Password");

			saveInKeyring = createLC(composite, "Remember password", false);
			parent.pack();
			return composite;
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			super.createButtonsForButtonBar(parent);
			Button test = createButton(parent, 2, "Test", false);
			test.addSelectionListener(new SelectionAdapter() {
				private static final long serialVersionUID = -1829962269440419560L;

				public void widgetSelected(SelectionEvent arg0) {
					testConnection();
				}
			});
		}

		void testConnection() {
			Session session = null;
			try {
				URI checkedUri = new URI(uri.getText());
				String checkedUriStr = checkedUri.toString();

				Hashtable<String, String> params = new Hashtable<String, String>();
				params.put(NodeConstants.LABELED_URI, checkedUriStr);
				Repository repository = repositoryFactory.getRepository(params);
				if (username.getText().trim().equals("")) {// anonymous
					// FIXME make it more generic
					session = repository.login("main");
				} else {
					// FIXME use getTextChars() when upgrading to 3.7
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=297412
					char[] pwd = password.getText().toCharArray();
					SimpleCredentials sc = new SimpleCredentials(username.getText(), pwd);
					session = repository.login(sc, "main");
					MessageDialog.openInformation(getParentShell(), "Success",
							"Connection to '" + uri.getText() + "' successful");
				}
			} catch (Exception e) {
				ErrorFeedback.show("Connection test failed for " + uri.getText(), e);
			} finally {
				JcrUtils.logoutQuietly(session);
			}
		}

		@Override
		protected void okPressed() {
			Session nodeSession = null;
			try {
				nodeSession = nodeRepository.login();
				Node home = NodeUtils.getUserHome(nodeSession);

				Node remote = home.hasNode(ArgeoNames.ARGEO_REMOTE) ? home.getNode(ArgeoNames.ARGEO_REMOTE)
						: home.addNode(ArgeoNames.ARGEO_REMOTE);
				if (remote.hasNode(name.getText()))
					throw new EclipseUiException("There is already a remote repository named " + name.getText());
				Node remoteRepository = remote.addNode(name.getText(), ArgeoTypes.ARGEO_REMOTE_REPOSITORY);
				remoteRepository.setProperty(ArgeoNames.ARGEO_URI, uri.getText());
				remoteRepository.setProperty(ArgeoNames.ARGEO_USER_ID, username.getText());
				nodeSession.save();
				if (saveInKeyring.getSelection()) {
					String pwdPath = remoteRepository.getPath() + '/' + ArgeoNames.ARGEO_PASSWORD;
					keyring.set(pwdPath, password.getText().toCharArray());
				}
				nodeSession.save();
				MessageDialog.openInformation(getParentShell(), "Repository Added",
						"Remote repository '" + username.getText() + "@" + uri.getText() + "' added");

				super.okPressed();
			} catch (Exception e) {
				ErrorFeedback.show("Cannot add remote repository", e);
			} finally {
				JcrUtils.logoutQuietly(nodeSession);
			}
		}

		/** Creates label and text. */
		protected Text createLT(Composite parent, String label, String initial) {
			new Label(parent, SWT.NONE).setText(label);
			Text text = new Text(parent, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			text.setText(initial);
			return text;
		}

		/** Creates label and check. */
		protected Button createLC(Composite parent, String label, Boolean initial) {
			new Label(parent, SWT.NONE).setText(label);
			Button check = new Button(parent, SWT.CHECK);
			check.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			check.setSelection(initial);
			return check;
		}

		protected Text createLP(Composite parent, String label) {
			new Label(parent, SWT.NONE).setText(label);
			Text text = new Text(parent, SWT.SINGLE | SWT.LEAD | SWT.BORDER | SWT.PASSWORD);
			text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			return text;
		}
	}
}
