package org.argeo.cms.users;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.cms.CmsUiProvider;
import org.argeo.cms.CmsUtils;
import org.argeo.cms.maintenance.NonAdminPage;
import org.argeo.eclipse.ui.dialogs.UserCreationWizard;
import org.argeo.eclipse.ui.parts.UsersTable;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.UserAdminService;
import org.argeo.security.jcr.JcrSecurityModel;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.springframework.security.userdetails.UserDetailsManager;

/**
 * Simple page to manage users of a given repository. We still rely on Argeo
 * model; with user stored in the main workspace
 */
public class Users implements CmsUiProvider {

	private final static Log log = LogFactory.getLog(Users.class);

	// Enable user CRUD // INJECTED
	private UserAdminService userAdminService;
	private JcrSecurityModel jcrSecurityModel;
	// private UserDetailsManager userDetailsManager;
	private String userWkspName;

	// Local UI Providers
	NonAdminPage nap = new NonAdminPage();
	UserPage userPage = new UserPage();

	// Manage authorization
	@Override
	public Control createUi(Composite parent, Node context)
			throws RepositoryException {
		if (isAdmin(context)) {
			Session session = context.getSession().getRepository()
					.login(userWkspName);
			return createMainLayout(parent, session);
		} else
			nap.createUi(parent, context);
		return null;
	}

	// Main layout
	// Left: User Table - Right User Details Edition
	private Control createMainLayout(Composite parent, final Session session)
			throws RepositoryException {

		Composite layoutCmp = new Composite(parent, SWT.NO_FOCUS);
		layoutCmp.setLayoutData(CmsUtils.fillAll());
		layoutCmp
				.setLayout(CmsUtils.noSpaceGridLayout(new GridLayout(2, true)));

		Composite left = new Composite(layoutCmp, SWT.NO_FOCUS);
		left.setLayoutData(CmsUtils.fillAll());
		UsersTable table = createUsersTable(left, session);

		final Composite right = new Composite(layoutCmp, SWT.NO_FOCUS);
		right.setLayoutData(CmsUtils.fillAll());
		// Composite innerPage = createUserPage(right);
		// final UserViewerOld userViewer = new UserViewerOld(innerPage);

		final TableViewer viewer = table.getTableViewer();
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) viewer
						.getSelection();
				if (selection.isEmpty()) {
					// Should we clean the right column?
					return;
				} else {
					Node context = (Node) selection.getFirstElement();
					try {
						CmsUtils.clear(right);
						userPage.createUi(right, context);
						right.layout();
						right.getParent().layout();
					} catch (RepositoryException e) {
						e.printStackTrace();
						throw new ArgeoException("unable to create "
								+ "editor for user " + context, e);
					}
				}
			}
		});
		return left;
	}

	private UsersTable createUsersTable(Composite parent, final Session session)
			throws RepositoryException {
		parent.setLayout(CmsUtils.noSpaceGridLayout());

		// Add user CRUD buttons
		Composite buttonCmp = new Composite(parent, SWT.NO_FOCUS);
		buttonCmp.setLayoutData(CmsUtils.fillWidth());
		buttonCmp.setLayout(new GridLayout(2, false));
		// Delete
		final Button deleteBtn = new Button(buttonCmp, SWT.PUSH);
		deleteBtn.setText("Delete selected");
		deleteBtn
				.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

		// Add
		final Button addBtn = new Button(buttonCmp, SWT.PUSH);
		addBtn.setText("Create");

		// Create the composite that displays the list and a filter
		final UsersTable userTableCmp = new UsersTable(parent, SWT.NO_FOCUS,
				session);
		userTableCmp.populate(true, false);
		userTableCmp.setLayoutData(CmsUtils.fillAll());

		// The various listeners
		userTableCmp.addDisposeListener(new DisposeListener() {
			private static final long serialVersionUID = -8854052549807709846L;

			@Override
			public void widgetDisposed(DisposeEvent event) {
				JcrUtils.logoutQuietly(session);
			}
		});

		deleteBtn.addSelectionListener(new SelectionAdapter() {
			private static final long serialVersionUID = -7340611909297995666L;

			@Override
			public void widgetSelected(SelectionEvent e) {

				TableViewer viewer = userTableCmp.getTableViewer();
				ISelection selection = viewer.getSelection();
				if (selection.isEmpty())
					return;

				Map<String, Node> toDelete = new TreeMap<String, Node>();
				@SuppressWarnings("unchecked")
				Iterator<Node> it = ((IStructuredSelection) selection)
						.iterator();
				nodes: while (it.hasNext()) {
					Node profileNode = it.next();
					try {
						String userName = profileNode.getProperty(
								ArgeoNames.ARGEO_USER_ID).getString();
						if (userName.equals(profileNode.getSession()
								.getUserID())) {
							log.warn("Cannot delete its own user: " + userName);
							continue nodes;
						}
						toDelete.put(userName, profileNode);
					} catch (RepositoryException re) {
						log.warn("Cannot interpred user " + profileNode);
					}
				}

				if (!MessageDialog.openQuestion(
						userTableCmp.getShell(),
						"Delete User",
						"Are you sure that you want to delete users "
								+ toDelete.keySet()
								+ "?\n"
								+ "This may lead to inconsistencies in the application."))
					return;

				for (String username : toDelete.keySet()) {
					Session session = null;
					try {
						Node profileNode = toDelete.get(username);
						userAdminService.deleteUser(username);
						profileNode.getParent().remove();
						session = profileNode.getSession();
						session.save();
					} catch (RepositoryException re) {
						JcrUtils.discardQuietly(session);
						throw new ArgeoException("Cannot list users", re);
					}
				}
				userTableCmp.refresh();
			}
		});

		addBtn.addSelectionListener(new SelectionAdapter() {
			private static final long serialVersionUID = 9214984636836267786L;

			@Override
			public void widgetSelected(SelectionEvent e) {
				UserCreationWizard newUserWizard = new UserCreationWizard(
						session, userAdminService, jcrSecurityModel);
				WizardDialog dialog = new WizardDialog(addBtn.getShell(),
						newUserWizard);
				if (dialog.open() == Dialog.OK)
					userTableCmp.refresh();
			}
		});

		// Configure
		// userTableCmp.getTableViewer().addDoubleClickListener(
		// new ViewDoubleClickListener());
		// getViewSite().setSelectionProvider(userTableCmp.getTableViewer());

		// Add listener to refresh the list when something changes
		// userStructureListener = new JcrUserListener(getSite().getShell()
		// .getDisplay());
		// JcrUtils.addListener(session, userStructureListener, Event.NODE_ADDED
		// | Event.NODE_REMOVED, ArgeoJcrConstants.PEOPLE_BASE_PATH, null);
		// userPropertiesListener = new JcrUserListener(getSite().getShell()
		// .getDisplay());
		// JcrUtils.addListener(session, userStructureListener,
		// Event.PROPERTY_CHANGED | Event.PROPERTY_ADDED
		// | Event.PROPERTY_REMOVED,
		// ArgeoJcrConstants.PEOPLE_BASE_PATH,
		// ArgeoTypes.ARGEO_USER_PROFILE);

		return userTableCmp;
	}

	// protected Composite createUserPage(Composite parent) {
	// parent.setLayout(CmsUtils.noSpaceGridLayout());
	// ScrolledPage scrolled = new ScrolledPage(parent, SWT.NONE);
	// scrolled.setLayoutData(CmsUtils.fillAll());
	// scrolled.setLayout(CmsUtils.noSpaceGridLayout());
	// // TODO manage style
	// // CmsUtils.style(scrolled, "maintenance_user_form");
	//
	// Composite page = new Composite(scrolled, SWT.NONE);
	// page.setLayout(CmsUtils.noSpaceGridLayout());
	// page.setBackgroundMode(SWT.INHERIT_NONE);
	//
	// return page;
	// }

	private boolean isAdmin(Node node) throws RepositoryException {
		// FIXME clean this once new user management policy has been
		// implemented.
		AccessControlManager acm = node.getSession().getAccessControlManager();
		Privilege[] privs = new Privilege[1];
		privs[0] = acm.privilegeFromName(Privilege.JCR_ALL);
		return acm.hasPrivileges("/", privs);
	}

	// @Override
	// public void dispose() {
	// JcrUtils.removeListenerQuietly(session, userStructureListener);
	// JcrUtils.removeListenerQuietly(session, userPropertiesListener);
	// JcrUtils.logoutQuietly(session);
	// super.dispose();
	// }
	//
	// // public void setSession(Session session) {
	// // this.session = session;
	// // }
	//
	// public void refresh() {
	// this.getSite().getShell().getDisplay().asyncExec(new Runnable() {
	// @Override
	// public void run() {
	// userTableCmp.refresh();
	// }
	// });
	// }
	//
	// private class JcrUserListener implements EventListener {
	// private final Display display;
	//
	// public JcrUserListener(Display display) {
	// super();
	// this.display = display;
	// }
	//
	// @Override
	// public void onEvent(EventIterator events) {
	// display.asyncExec(new Runnable() {
	// @Override
	// public void run() {
	// userTableCmp.refresh();
	// }
	// });
	// }
	// }
	//
	// class ViewDoubleClickListener implements IDoubleClickListener {
	// public void doubleClick(DoubleClickEvent evt) {
	// if (evt.getSelection().isEmpty())
	// return;
	//
	// Object obj = ((IStructuredSelection) evt.getSelection())
	// .getFirstElement();
	// if (obj instanceof Node) {
	// try {
	// String username = ((Node) obj).getProperty(ARGEO_USER_ID)
	// .getString();
	// String commandId = OpenArgeoUserEditor.COMMAND_ID;
	// String paramName = OpenArgeoUserEditor.PARAM_USERNAME;
	// CommandUtils.callCommand(commandId, paramName, username);
	// } catch (RepositoryException e) {
	// throw new ArgeoException("Cannot open user editor", e);
	// }
	// }
	// }

	/* DEPENDENCY INJECTION */
	public void setWorkspaceName(String workspaceName) {
		this.userWkspName = workspaceName;
	}

	public void setUserAdminService(UserAdminService userAdminService) {
		this.userAdminService = userAdminService;
		userPage.setUserAdminService(userAdminService);
	}

	public void setJcrSecurityModel(JcrSecurityModel jcrSecurityModel) {
		this.jcrSecurityModel = jcrSecurityModel;
		// userPage.setJcrSecurityModel(jcrSecurityModel);
	}

	public void setUserDetailsManager(UserDetailsManager userDetailsManager) {
		// this.userDetailsManager = userDetailsManager;
		// userPage.setUserDetailsManager(userDetailsManager);
	}
}