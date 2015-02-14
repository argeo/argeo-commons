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
package org.argeo.security.ui.admin.wizards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.ArgeoMonitor;
import org.argeo.eclipse.ui.EclipseArgeoMonitor;
import org.argeo.eclipse.ui.parts.UsersTable;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.UserAdminService;
import org.argeo.security.jcr.JcrUserDetails;
import org.argeo.security.ui.PrivilegedJob;
import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/** Wizard to update users */
public class UserBatchUpdateWizard extends Wizard {
	private final static Log log = LogFactory
			.getLog(UserBatchUpdateWizard.class);
	private Session session;
	private UserAdminService userAdminService;

	// pages
	private ChooseCommandWizardPage chooseCommandPage;
	private ChooseUsersWizardPage userListPage;
	private ValidateAndLaunchWizardPage validatePage;

	// /////////////////////////////////////////////////
	// / Definition of the various implemented commands
	private final static String CMD_UPDATE_PASSWORD = "resetPassword";
	private final static String CMD_GROUP_MEMBERSHIP = "groupMembership";

	private final Map<String, String> commands = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;
		{
			put("Enable user(s)", ArgeoNames.ARGEO_ENABLED);
			put("Expire credentials", ArgeoNames.ARGEO_CREDENTIALS_NON_EXPIRED);
			put("Expire account(s)", ArgeoNames.ARGEO_ACCOUNT_NON_EXPIRED);
			put("Lock account(s)", ArgeoNames.ARGEO_ACCOUNT_NON_LOCKED);
			put("Reset password(s)", CMD_UPDATE_PASSWORD);
			// TODO implement role / group management
			// put("Add/Remove from group", CMD_GROUP_MEMBERSHIP);
		}
	};

	public UserBatchUpdateWizard(Session session,
			UserAdminService userAdminService) {
		this.session = session;
		this.userAdminService = userAdminService;
	}

	@Override
	public void addPages() {
		chooseCommandPage = new ChooseCommandWizardPage();
		addPage(chooseCommandPage);
		userListPage = new ChooseUsersWizardPage(session);
		addPage(userListPage);
		validatePage = new ValidateAndLaunchWizardPage(session);
		addPage(validatePage);
	}

	@Override
	public boolean performFinish() {
		if (!canFinish())
			return false;

		UpdateJob job = null;
		if (ArgeoNames.ARGEO_ENABLED.equals(chooseCommandPage.getCommand())) {
			job = new UpdateBoolean(session, userListPage.getSelectedUsers(),
					ArgeoNames.ARGEO_ENABLED,
					chooseCommandPage.getBoleanValue());
		} else if (ArgeoNames.ARGEO_CREDENTIALS_NON_EXPIRED
				.equals(chooseCommandPage.getCommand())) {
			job = new UpdateBoolean(session, userListPage.getSelectedUsers(),
					ArgeoNames.ARGEO_CREDENTIALS_NON_EXPIRED,
					chooseCommandPage.getBoleanValue());
		} else if (ArgeoNames.ARGEO_ACCOUNT_NON_EXPIRED
				.equals(chooseCommandPage.getCommand())) {
			job = new UpdateBoolean(session, userListPage.getSelectedUsers(),
					ArgeoNames.ARGEO_ACCOUNT_NON_EXPIRED,
					chooseCommandPage.getBoleanValue());
		} else if (ArgeoNames.ARGEO_ACCOUNT_NON_LOCKED.equals(chooseCommandPage
				.getCommand())) {
			job = new UpdateBoolean(session, userListPage.getSelectedUsers(),
					ArgeoNames.ARGEO_ACCOUNT_NON_LOCKED,
					chooseCommandPage.getBoleanValue());
		} else if (CMD_UPDATE_PASSWORD.equals(chooseCommandPage.getCommand())) {
			String newValue = chooseCommandPage.getPwdValue();
			if (newValue == null)
				throw new ArgeoException(
						"Password cannot be null or an empty string");
			job = new ResetPassword(session, userAdminService,
					userListPage.getSelectedUsers(), newValue);
		}

		if (job != null)
			job.schedule();
		return true;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public boolean canFinish() {
		if (this.getContainer().getCurrentPage() == validatePage)
			return true;
		return false;
	}

	// /////////////////////////
	// REEL UPDATE JOB
	private class UpdateBoolean extends UpdateJob {
		private String propertyName;
		private boolean value;

		public UpdateBoolean(Session session, List<Node> nodesToUpdate,
				String propertyName, boolean value) {
			super(session, nodesToUpdate);
			this.propertyName = propertyName;
			this.value = value;
		}

		protected void doUpdate(Node node) {
			try {
				node.setProperty(propertyName, value);
			} catch (RepositoryException re) {
				throw new ArgeoException(
						"Unable to update boolean value for node " + node, re);
			}
		}
	}

	private class ResetPassword extends UpdateJob {
		private String newValue;
		private UserAdminService userAdminService;

		public ResetPassword(Session session,
				UserAdminService userAdminService, List<Node> nodesToUpdate,
				String newValue) {
			super(session, nodesToUpdate);
			this.newValue = newValue;
			this.userAdminService = userAdminService;
		}

		protected void doUpdate(Node node) {
			try {
				String userId = node.getProperty(ArgeoNames.ARGEO_USER_ID)
						.getString();
				if (userAdminService.userExists(userId)) {
					JcrUserDetails userDetails = (JcrUserDetails) userAdminService
							.loadUserByUsername(userId);
					userAdminService.updateUser(userDetails
							.cloneWithNewPassword(newValue));
				}
			} catch (RepositoryException re) {
				throw new ArgeoException(
						"Unable to update boolean value for node " + node, re);
			}
		}
	}

	@SuppressWarnings("unused")
	private class AddToGroup extends UpdateJob {
		private String groupID;
		private Session session;

		public AddToGroup(Session session, List<Node> nodesToUpdate,
				String groupID) {
			super(session, nodesToUpdate);
			this.session = session;
			this.groupID = groupID;
		}

		protected void doUpdate(Node node) {
			log.info("Add/Remove to group actions are not yet implemented");
			// TODO implement this
			// try {
			// throw new ArgeoException("Not yet implemented");
			// } catch (RepositoryException re) {
			// throw new ArgeoException(
			// "Unable to update boolean value for node " + node, re);
			// }
		}
	}

	/**
	 * Base privileged job that will be run asynchronously to perform the batch
	 * update
	 */
	private abstract class UpdateJob extends PrivilegedJob {

		private final Session currSession;
		private final List<Node> nodesToUpdate;

		protected abstract void doUpdate(Node node);

		public UpdateJob(Session session, List<Node> nodesToUpdate) {
			super("Perform update");
			try {
				this.currSession = session.getRepository().login();
				// "move" nodes to update in the new session
				// the "old" session will be closed by the calling command
				// before the job has effectively ran
				// TODO there must be a cleaner way to do.
				List<Node> nodes = new ArrayList<Node>();
				for (Node node : nodesToUpdate) {
					nodes.add(currSession.getNode(node.getPath()));
				}
				this.nodesToUpdate = nodes;
			} catch (RepositoryException e) {
				throw new ArgeoException("Error while dupplicating "
						+ "session for job", e);
			}
		}

		@Override
		protected IStatus doRun(IProgressMonitor progressMonitor) {
			try {
				ArgeoMonitor monitor = new EclipseArgeoMonitor(progressMonitor);
				VersionManager vm = currSession.getWorkspace()
						.getVersionManager();
				int total = nodesToUpdate.size();
				monitor.beginTask("Performing change", total);
				for (Node node : nodesToUpdate) {
					String path = node.getPath();
					vm.checkout(path);
					doUpdate(node);
					currSession.save();
					vm.checkin(path);
					monitor.worked(1);
				}
			} catch (Exception e) {
				log.error("Cannot perform batch update on users", e);
				// e.printStackTrace();

				// Dig exception to find the root cause that will enable the
				// user to understand the problem
				Throwable cause = e;
				Throwable originalCause = e;
				while (cause != null) {
					if (log.isTraceEnabled())
						log.trace("Parent Cause message : "
								+ cause.getMessage());
					originalCause = cause;
					cause = cause.getCause();
				}
				return new Status(IStatus.ERROR, SecurityAdminPlugin.PLUGIN_ID,
						"Cannot perform updates.", originalCause);
			} finally {
				JcrUtils.logoutQuietly(currSession);
			}
			return Status.OK_STATUS;
		}
	}

	// //////////////////////
	// Pages definition
	/** Displays a combo box that enables user to choose which action to perform */
	private class ChooseCommandWizardPage extends WizardPage {
		private static final long serialVersionUID = 1L;

		private Combo chooseCommandCmb;
		private Button trueChk;
		private Text valueTxt;
		private Text pwdTxt;
		private Text pwd2Txt;

		public ChooseCommandWizardPage() {
			super("Choose a command to run.");
			setTitle("Choose a command to run.");
		}

		@Override
		public void createControl(Composite parent) {
			GridLayout gl = new GridLayout();
			Composite container = new Composite(parent, SWT.NO_FOCUS);
			container.setLayout(gl);

			chooseCommandCmb = new Combo(container, SWT.NO_FOCUS);
			String[] values = commands.keySet().toArray(
					new String[commands.size()]);
			chooseCommandCmb.setItems(values);
			chooseCommandCmb.setLayoutData(new GridData(SWT.FILL, SWT.TOP,
					true, false));

			final Composite bottomPart = new Composite(container, SWT.NO_FOCUS);
			gl = new GridLayout();
			gl.horizontalSpacing = gl.marginWidth = gl.verticalSpacing = 0;
			bottomPart.setLayout(gl);
			bottomPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
					true));

			chooseCommandCmb.addSelectionListener(new SelectionListener() {
				private static final long serialVersionUID = 1L;

				@Override
				public void widgetSelected(SelectionEvent e) {
					if (getCommand().equals(CMD_UPDATE_PASSWORD))
						populatePasswordCmp(bottomPart);
					else if (getCommand().equals(CMD_GROUP_MEMBERSHIP))
						populateGroupCmp(bottomPart);
					else
						populateBooleanFlagCmp(bottomPart);
					bottomPart.pack(true);
					bottomPart.layout();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});

			setControl(container);
		}

		private void cleanParent(Composite parent) {
			if (parent.getChildren().length > 0) {
				for (Control control : parent.getChildren())
					control.dispose();
			}
		}

		private void populateBooleanFlagCmp(Composite parent) {
			cleanParent(parent);
			trueChk = new Button(parent, SWT.CHECK);
			trueChk.setText("Do it. (It will to the contrary if unchecked)");
			trueChk.setSelection(true);
			trueChk.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		}

		private void populatePasswordCmp(Composite parent) {
			cleanParent(parent);
			Composite body = new Composite(parent, SWT.NO_FOCUS);
			body.setLayout(new GridLayout(2, false));
			body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			pwdTxt = createLP(body, "New password", "");
			pwd2Txt = createLP(body, "Repeat password", "");
		}

		/** Creates label and password. */
		protected Text createLP(Composite body, String label, String value) {
			Label lbl = new Label(body, SWT.NONE);
			lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
			lbl.setText(label);
			Text text = new Text(body, SWT.BORDER | SWT.PASSWORD);
			text.setText(value);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			return text;
		}

		private void populateGroupCmp(Composite parent) {
			if (parent.getChildren().length > 0) {
				for (Control control : parent.getChildren())
					control.dispose();
			}
			trueChk = new Button(parent, SWT.CHECK);
			trueChk.setText("Add to group. (It will remove user(s) from the "
					+ "corresponding group if unchecked)");
			trueChk.setSelection(true);
			trueChk.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		}

		protected String getCommand() {
			return commands.get(chooseCommandCmb.getItem(chooseCommandCmb
					.getSelectionIndex()));
		}

		protected String getCommandLbl() {
			return chooseCommandCmb.getItem(chooseCommandCmb
					.getSelectionIndex());
		}

		protected boolean getBoleanValue() {
			// FIXME this is not consistent and will lead to errors.
			if (ArgeoNames.ARGEO_ENABLED.equals(getCommand()))
				return trueChk.getSelection();
			else
				return !trueChk.getSelection();
		}

		@SuppressWarnings("unused")
		protected String getStringValue() {
			String value = null;
			if (valueTxt != null) {
				value = valueTxt.getText();
				if ("".equals(value.trim()))
					value = null;
			}
			return value;
		}

		protected String getPwdValue() {
			String newPwd = null;
			if (pwdTxt == null || pwd2Txt == null)
				return null;
			if (!pwdTxt.getText().equals("") || !pwd2Txt.getText().equals("")) {
				if (pwdTxt.getText().equals(pwd2Txt.getText())) {
					newPwd = pwdTxt.getText();
					pwdTxt.setText("");
					pwd2Txt.setText("");
				} else {
					pwdTxt.setText("");
					pwd2Txt.setText("");
					throw new ArgeoException("Passwords are not equals");
				}
			}
			return newPwd;
		}
	}

	/**
	 * Displays a list of users with a check box to be able to choose some of
	 * them
	 */
	private class ChooseUsersWizardPage extends WizardPage implements
			IPageChangedListener {
		private static final long serialVersionUID = 1L;
		private UsersTable userTableCmp;
		private Composite container;
		private Session session;

		public ChooseUsersWizardPage(Session session) {
			super("Choose Users");
			this.session = session;
			setTitle("Select users who will be impacted");
		}

		@Override
		public void createControl(Composite parent) {
			container = new Composite(parent, SWT.NONE);
			container.setLayout(new FillLayout());
			userTableCmp = new MyUserTableCmp(container, SWT.NO_FOCUS, session);
			userTableCmp.populate(true, true);
			setControl(container);

			// Add listener to update message when shown
			final IWizardContainer container = this.getContainer();
			if (container instanceof IPageChangeProvider) {
				((IPageChangeProvider) container).addPageChangedListener(this);
			}

		}

		@Override
		public void pageChanged(PageChangedEvent event) {
			if (event.getSelectedPage() == this) {
				String msg = "Chosen batch action: "
						+ chooseCommandPage.getCommandLbl();
				((WizardPage) event.getSelectedPage()).setMessage(msg);
			}
		}

		protected List<Node> getSelectedUsers() {
			return userTableCmp.getSelectedUsers();
		}

		private class MyUserTableCmp extends UsersTable {

			private static final long serialVersionUID = 1L;

			public MyUserTableCmp(Composite parent, int style, Session session) {
				super(parent, style, session);
			}

			@Override
			protected void refreshFilteredList() {
				List<Node> nodes = new ArrayList<Node>();
				try {
					NodeIterator ni = listFilteredElements(session,
							getFilterString());

					users: while (ni.hasNext()) {
						Node currNode = ni.nextNode();
						String username = currNode.hasProperty(ARGEO_USER_ID) ? currNode
								.getProperty(ARGEO_USER_ID).getString() : "";
						if (username.equals(session.getUserID()))
							continue users;
						else
							nodes.add(currNode);
					}
					getTableViewer().setInput(nodes.toArray());
				} catch (RepositoryException e) {
					throw new ArgeoException("Unable to list users", e);
				}
			}
		}
	}

	/**
	 * Recapitulation of input data before running real update
	 */
	private class ValidateAndLaunchWizardPage extends WizardPage implements
			IPageChangedListener {
		private static final long serialVersionUID = 1L;
		private UsersTable userTableCmp;
		private Session session;

		public ValidateAndLaunchWizardPage(Session session) {
			super("Validate and launch");
			this.session = session;
			setTitle("Validate and launch");
		}

		@Override
		public void createControl(Composite parent) {
			Composite mainCmp = new Composite(parent, SWT.NO_FOCUS);
			mainCmp.setLayout(new FillLayout());

			// Add listener to update user list when shown
			final IWizardContainer container = this.getContainer();
			if (container instanceof IPageChangeProvider) {
				((IPageChangeProvider) container).addPageChangedListener(this);
			}

			userTableCmp = new UsersTable(mainCmp, SWT.NO_FOCUS, session);
			userTableCmp.populate(false, false);
			setControl(mainCmp);
		}

		@Override
		public void pageChanged(PageChangedEvent event) {
			if (event.getSelectedPage() == this) {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				Object[] values = ((ArrayList) userListPage.getSelectedUsers())
						.toArray(new Object[userListPage.getSelectedUsers()
								.size()]);
				userTableCmp.getTableViewer().setInput(values);
				String msg = "Following batch action: ["
						+ chooseCommandPage.getCommandLbl()
						+ "] will be perfomed on the users listed below.\n"
						+ "Are you sure you want to proceed?";
				((WizardPage) event.getSelectedPage()).setMessage(msg);
			}
		}

		// private class MyUserTableCmp extends UserTableComposite {
		// public MyUserTableCmp(Composite parent, int style, Session session) {
		// super(parent, style, session);
		// }
		//
		// @Override
		// protected void refreshFilteredList() {
		// @SuppressWarnings({ "unchecked", "rawtypes" })
		//
		// setFilteredList(values);
		// }
		//
		// @Override
		// public void setVisible(boolean visible) {
		// super.setVisible(visible);
		// if (visible)
		// refreshFilteredList();
		// }
		// }
	}
}