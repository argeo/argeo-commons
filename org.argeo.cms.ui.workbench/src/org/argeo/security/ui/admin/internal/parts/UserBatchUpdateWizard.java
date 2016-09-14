package org.argeo.security.ui.admin.internal.parts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.cms.auth.AuthConstants;
import org.argeo.cms.util.useradmin.UserAdminUtils;
import org.argeo.eclipse.ui.ColumnDefinition;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.parts.LdifUsersTable;
import org.argeo.jcr.ArgeoNames;
import org.argeo.osgi.useradmin.LdifName;
import org.argeo.security.ui.admin.internal.UserAdminWrapper;
import org.argeo.security.ui.admin.internal.providers.CommonNameLP;
import org.argeo.security.ui.admin.internal.providers.DomainNameLP;
import org.argeo.security.ui.admin.internal.providers.MailLP;
import org.argeo.security.ui.admin.internal.providers.UserNameLP;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/** Wizard to update users */
public class UserBatchUpdateWizard extends Wizard {

	private final static Log log = LogFactory
			.getLog(UserBatchUpdateWizard.class);
	private UserAdminWrapper userAdminWrapper;

	// pages
	private ChooseCommandWizardPage chooseCommandPage;
	private ChooseUsersWizardPage userListPage;
	private ValidateAndLaunchWizardPage validatePage;

	// Various implemented commands keys
	private final static String CMD_UPDATE_PASSWORD = "resetPassword";
	private final static String CMD_GROUP_MEMBERSHIP = "groupMembership";

	private final Map<String, String> commands = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;
		{
			put("Reset password(s)", CMD_UPDATE_PASSWORD);
			// TODO implement role / group management
			// put("Add/Remove from group", CMD_GROUP_MEMBERSHIP);
		}
	};

	public UserBatchUpdateWizard(UserAdminWrapper userAdminWrapper) {
		this.userAdminWrapper = userAdminWrapper;
	}

	@Override
	public void addPages() {
		chooseCommandPage = new ChooseCommandWizardPage();
		addPage(chooseCommandPage);
		userListPage = new ChooseUsersWizardPage();
		addPage(userListPage);
		validatePage = new ValidateAndLaunchWizardPage();
		addPage(validatePage);
	}

	@Override
	public boolean performFinish() {
		if (!canFinish())
			return false;
		UserTransaction ut = userAdminWrapper.getUserTransaction();
		try {
			if (ut.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION
					&& !MessageDialog.openConfirm(getShell(),
							"Existing Transaction",
							"A user transaction is already existing, "
									+ "are you sure you want to proceed ?"))
				return false;
		} catch (SystemException e) {
			throw new CmsException("Cannot get user transaction state "
					+ "before user batch update", e);
		}

		// We cannot use jobs, user modifications are still meant to be done in
		// the UIThread
		// UpdateJob job = null;
		// if (job != null)
		// job.schedule();

		if (CMD_UPDATE_PASSWORD.equals(chooseCommandPage.getCommand())) {
			char[] newValue = chooseCommandPage.getPwdValue();
			if (newValue == null)
				throw new CmsException(
						"Password cannot be null or an empty string");
			ResetPassword job = new ResetPassword(userAdminWrapper,
					userListPage.getSelectedUsers(), newValue);
			job.doUpdate();
		}
		return true;
	}

	public boolean canFinish() {
		if (this.getContainer().getCurrentPage() == validatePage)
			return true;
		return false;
	}

	private class ResetPassword {
		private char[] newPwd;
		private UserAdminWrapper userAdminWrapper;
		private List<User> usersToUpdate;

		public ResetPassword(UserAdminWrapper userAdminWrapper,
				List<User> usersToUpdate, char[] newPwd) {
			this.newPwd = newPwd;
			this.usersToUpdate = usersToUpdate;
			this.userAdminWrapper = userAdminWrapper;
		}

		@SuppressWarnings("unchecked")
		protected void doUpdate() {
			userAdminWrapper.beginTransactionIfNeeded();
			try {
				for (User user : usersToUpdate) {
					// the char array is emptied after being used.
					user.getCredentials().put(null, newPwd.clone());
				}
				userAdminWrapper.commitOrNotifyTransactionStateChange();
			} catch (Exception e) {
				throw new CmsException("Cannot perform batch update on users",
						e);
			} finally {
				UserTransaction ut = userAdminWrapper.getUserTransaction();
				try {
					if (ut.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION)
						ut.rollback();
				} catch (IllegalStateException | SecurityException
						| SystemException e) {
					log.error("Unable to rollback session in 'finally', "
							+ "the system might be in a dirty state");
					e.printStackTrace();
				}
			}
		}
	}

	// @SuppressWarnings("unused")
	// private class AddToGroup extends UpdateJob {
	// private String groupID;
	// private Session session;
	//
	// public AddToGroup(Session session, List<Node> nodesToUpdate,
	// String groupID) {
	// super(session, nodesToUpdate);
	// this.session = session;
	// this.groupID = groupID;
	// }
	//
	// protected void doUpdate(Node node) {
	// log.info("Add/Remove to group actions are not yet implemented");
	// // TODO implement this
	// // try {
	// // throw new CmsException("Not yet implemented");
	// // } catch (RepositoryException re) {
	// // throw new CmsException(
	// // "Unable to update boolean value for node " + node, re);
	// // }
	// }
	// }

	// /**
	// * Base privileged job that will be run asynchronously to perform the
	// batch
	// * update
	// */
	// private abstract class UpdateJob extends PrivilegedJob {
	//
	// private final UserAdminWrapper userAdminWrapper;
	// private final List<User> usersToUpdate;
	//
	// protected abstract void doUpdate(User user);
	//
	// public UpdateJob(UserAdminWrapper userAdminWrapper,
	// List<User> usersToUpdate) {
	// super("Perform update");
	// this.usersToUpdate = usersToUpdate;
	// this.userAdminWrapper = userAdminWrapper;
	// }
	//
	// @Override
	// protected IStatus doRun(IProgressMonitor progressMonitor) {
	// try {
	// ArgeoMonitor monitor = new EclipseArgeoMonitor(progressMonitor);
	// int total = usersToUpdate.size();
	// monitor.beginTask("Performing change", total);
	// userAdminWrapper.beginTransactionIfNeeded();
	// for (User user : usersToUpdate) {
	// doUpdate(user);
	// monitor.worked(1);
	// }
	// userAdminWrapper.getUserTransaction().commit();
	// } catch (Exception e) {
	// throw new CmsException(
	// "Cannot perform batch update on users", e);
	// } finally {
	// UserTransaction ut = userAdminWrapper.getUserTransaction();
	// try {
	// if (ut.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION)
	// ut.rollback();
	// } catch (IllegalStateException | SecurityException
	// | SystemException e) {
	// log.error("Unable to rollback session in 'finally', "
	// + "the system might be in a dirty state");
	// e.printStackTrace();
	// }
	// }
	// return Status.OK_STATUS;
	// }
	// }

	// PAGES
	/** Displays a combo box that enables user to choose which action to perform */
	private class ChooseCommandWizardPage extends WizardPage {
		private static final long serialVersionUID = -8069434295293996633L;
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

			chooseCommandCmb = new Combo(container, SWT.READ_ONLY);
			chooseCommandCmb.setLayoutData(EclipseUiUtils.fillWidth());
			String[] values = commands.keySet().toArray(new String[0]);
			chooseCommandCmb.setItems(values);

			final Composite bottomPart = new Composite(container, SWT.NO_FOCUS);
			bottomPart.setLayoutData(EclipseUiUtils.fillAll());
			bottomPart.setLayout(EclipseUiUtils.noSpaceGridLayout());

			chooseCommandCmb.addSelectionListener(new SelectionAdapter() {
				private static final long serialVersionUID = 1L;

				@Override
				public void widgetSelected(SelectionEvent e) {
					if (getCommand().equals(CMD_UPDATE_PASSWORD))
						populatePasswordCmp(bottomPart);
					else if (getCommand().equals(CMD_GROUP_MEMBERSHIP))
						populateGroupCmp(bottomPart);
					else
						populateBooleanFlagCmp(bottomPart);
					checkPageComplete();
					bottomPart.layout(true, true);
				}
			});
			setControl(container);
		}

		private void populateBooleanFlagCmp(Composite parent) {
			EclipseUiUtils.clear(parent);
			trueChk = new Button(parent, SWT.CHECK);
			trueChk.setText("Do it. (It will to the contrary if unchecked)");
			trueChk.setSelection(true);
			trueChk.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		}

		private void populatePasswordCmp(Composite parent) {
			EclipseUiUtils.clear(parent);
			Composite body = new Composite(parent, SWT.NO_FOCUS);

			ModifyListener ml = new ModifyListener() {
				private static final long serialVersionUID = -1558726363536729634L;

				@Override
				public void modifyText(ModifyEvent event) {
					checkPageComplete();
				}
			};

			body.setLayout(new GridLayout(2, false));
			body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			pwdTxt = EclipseUiUtils.createGridLP(body, "New password", ml);
			pwd2Txt = EclipseUiUtils.createGridLP(body, "Repeat password", ml);
		}

		private void checkPageComplete() {
			String errorMsg = null;
			if (chooseCommandCmb.getSelectionIndex() < 0)
				errorMsg = "Please select an action";
			else if (CMD_UPDATE_PASSWORD.equals(getCommand())) {
				if (EclipseUiUtils.isEmpty(pwdTxt.getText())
						|| pwdTxt.getText().length() < 4)
					errorMsg = "Please enter a password that is at least 4 character long";
				else if (!pwdTxt.getText().equals(pwd2Txt.getText()))
					errorMsg = "Passwords are different";
			}
			if (EclipseUiUtils.notEmpty(errorMsg)) {
				setMessage(errorMsg, WizardPage.ERROR);
				setPageComplete(false);
			} else {
				setMessage("Page complete, you can proceed to user choice",
						WizardPage.INFORMATION);
				setPageComplete(true);
			}

			getContainer().updateButtons();
		}

		private void populateGroupCmp(Composite parent) {
			EclipseUiUtils.clear(parent);
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

		@SuppressWarnings("unused")
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

		protected char[] getPwdValue() {
			// We do not directly reset the password text fields: There is no
			// need to over secure this process: setting a pwd to multi users
			// at the same time is anyhow a bad practice and should be used only
			// in test environment or for temporary access
			if (pwdTxt == null || pwdTxt.isDisposed())
				return null;
			else
				return pwdTxt.getText().toCharArray();
		}
	}

	/**
	 * Displays a list of users with a check box to be able to choose some of
	 * them
	 */
	private class ChooseUsersWizardPage extends WizardPage implements
			IPageChangedListener {
		private static final long serialVersionUID = 7651807402211214274L;
		private ChooseUserTableViewer userTableCmp;

		public ChooseUsersWizardPage() {
			super("Choose Users");
			setTitle("Select users who will be impacted");
		}

		@Override
		public void createControl(Composite parent) {
			Composite pageCmp = new Composite(parent, SWT.NONE);
			pageCmp.setLayout(EclipseUiUtils.noSpaceGridLayout());

			// Define the displayed columns
			List<ColumnDefinition> columnDefs = new ArrayList<ColumnDefinition>();
			columnDefs.add(new ColumnDefinition(new CommonNameLP(),
					"Common Name", 150));
			columnDefs.add(new ColumnDefinition(new MailLP(), "E-mail", 150));
			columnDefs.add(new ColumnDefinition(new DomainNameLP(), "Domain",
					200));

			// Only show technical DN to admin
			if (UserAdminUtils.isUserInRole(AuthConstants.ROLE_ADMIN))
				columnDefs.add(new ColumnDefinition(new UserNameLP(),
						"Distinguished Name", 300));

			userTableCmp = new ChooseUserTableViewer(pageCmp, SWT.MULTI
					| SWT.H_SCROLL | SWT.V_SCROLL);
			userTableCmp.setLayoutData(EclipseUiUtils.fillAll());
			userTableCmp.setColumnDefinitions(columnDefs);
			userTableCmp.populate(true, true);
			userTableCmp.refresh();

			setControl(pageCmp);

			// Add listener to update message when shown
			final IWizardContainer wContainer = this.getContainer();
			if (wContainer instanceof IPageChangeProvider) {
				((IPageChangeProvider) wContainer).addPageChangedListener(this);
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

		protected List<User> getSelectedUsers() {
			return userTableCmp.getSelectedUsers();
		}

		private class ChooseUserTableViewer extends LdifUsersTable {
			private static final long serialVersionUID = 5080437561015853124L;
			private final String[] knownProps = { LdifName.uid.name(),
					LdifName.dn.name(), LdifName.cn.name(),
					LdifName.givenName.name(), LdifName.sn.name(),
					LdifName.mail.name() };

			public ChooseUserTableViewer(Composite parent, int style) {
				super(parent, style);
			}

			@Override
			protected List<User> listFilteredElements(String filter) {
				Role[] roles;

				try {
					StringBuilder builder = new StringBuilder();

					StringBuilder tmpBuilder = new StringBuilder();
					if (EclipseUiUtils.notEmpty(filter))
						for (String prop : knownProps) {
							tmpBuilder.append("(");
							tmpBuilder.append(prop);
							tmpBuilder.append("=*");
							tmpBuilder.append(filter);
							tmpBuilder.append("*)");
						}
					if (tmpBuilder.length() > 1) {
						builder.append("(&(")
								.append(LdifName.objectClass.name())
								.append("=")
								.append(LdifName.inetOrgPerson.name())
								.append(")(|");
						builder.append(tmpBuilder.toString());
						builder.append("))");
					} else
						builder.append("(").append(LdifName.objectClass.name())
								.append("=")
								.append(LdifName.inetOrgPerson.name())
								.append(")");
					roles = userAdminWrapper.getUserAdmin().getRoles(
							builder.toString());
				} catch (InvalidSyntaxException e) {
					throw new CmsException("Unable to get roles with filter: "
							+ filter, e);
				}
				List<User> users = new ArrayList<User>();
				for (Role role : roles)
					// Prevent current logged in user to perform batch on
					// himself
					if (!UserAdminUtils.isCurrentUser((User) role))
						users.add((User) role);
				return users;
			}
		}
	}

	/** Summary of input data before launching the process */
	private class ValidateAndLaunchWizardPage extends WizardPage implements
			IPageChangedListener {
		private static final long serialVersionUID = 7098918351451743853L;
		private ChosenUsersTableViewer userTableCmp;

		public ValidateAndLaunchWizardPage() {
			super("Validate and launch");
			setTitle("Validate and launch");
		}

		@Override
		public void createControl(Composite parent) {
			Composite pageCmp = new Composite(parent, SWT.NO_FOCUS);
			pageCmp.setLayout(EclipseUiUtils.noSpaceGridLayout());

			List<ColumnDefinition> columnDefs = new ArrayList<ColumnDefinition>();
			columnDefs.add(new ColumnDefinition(new CommonNameLP(),
					"Common Name", 150));
			columnDefs.add(new ColumnDefinition(new MailLP(), "E-mail", 150));
			columnDefs.add(new ColumnDefinition(new DomainNameLP(), "Domain",
					200));
			// Only show technical DN to admin
			if (UserAdminUtils.isUserInRole(AuthConstants.ROLE_ADMIN))
				columnDefs.add(new ColumnDefinition(new UserNameLP(),
						"Distinguished Name", 300));
			userTableCmp = new ChosenUsersTableViewer(pageCmp, SWT.MULTI
					| SWT.H_SCROLL | SWT.V_SCROLL);
			userTableCmp.setLayoutData(EclipseUiUtils.fillAll());
			userTableCmp.setColumnDefinitions(columnDefs);
			userTableCmp.populate(false, false);
			userTableCmp.refresh();
			setControl(pageCmp);
			// Add listener to update message when shown
			final IWizardContainer wContainer = this.getContainer();
			if (wContainer instanceof IPageChangeProvider) {
				((IPageChangeProvider) wContainer).addPageChangedListener(this);
			}
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
						+ "] will be perfomed on the users listed below.\n";
				// + "Are you sure you want to proceed?";
				setMessage(msg);
			}
		}

		private class ChosenUsersTableViewer extends LdifUsersTable {
			private static final long serialVersionUID = 7814764735794270541L;

			public ChosenUsersTableViewer(Composite parent, int style) {
				super(parent, style);
			}

			@Override
			protected List<User> listFilteredElements(String filter) {
				return userListPage.getSelectedUsers();
			}
		}
	}
}