package org.argeo.cms.users;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.cms.CmsUiProvider;
import org.argeo.cms.maintenance.NonAdminPage;
import org.argeo.cms.util.CmsUtils;
import org.argeo.eclipse.ui.parts.UsersTable;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.UserAdminService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Simple page to manage users of a given repository. It relies on Argeo user
 * model: user profile nodes are stored in the main workspace
 */
public class UsersPage implements CmsUiProvider {
	private final static Log log = LogFactory.getLog(UsersPage.class);

	/* DEPENDENCY INJECTION */
	private UserAdminService userAdminService;
	private String userWkspName;

	// TODO use a constant
	private final static String ROLE_USER_ADMIN = "ROLE_USER_ADMIN";

	// Local UI Providers
	private NonAdminPage nap = new NonAdminPage();
	private UserPage userPage = new UserPage();

	@Override
	public Control createUi(Composite parent, Node context)
			throws RepositoryException {
		// This page is only visible to user with role USER_ADMIN
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

		final TableViewer viewer = table.getTableViewer();
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) viewer
						.getSelection();
				if (selection.isEmpty()) {
					// Should we clean the right column?
					CmsUtils.clear(right);
					right.layout();
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
		final UsersTable userTableCmp = new UsersTable(parent, SWT.BORDER,
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
				NewUserWizard newUserWizard = new NewUserWizard(session,
						userAdminService);
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

	private boolean isAdmin(Node node) throws RepositoryException {
		return isUserInRole(ROLE_USER_ADMIN);
	}

	/**
	 * Returns true if the current user is in the specified role TODO factoize
	 * in the user admin service
	 */
	private boolean isUserInRole(String role) {
		Authentication authen = SecurityContextHolder.getContext()
				.getAuthentication();
		for (GrantedAuthority ga : authen.getAuthorities()) {
			if (ga.getAuthority().equals(role))
				return true;
		}
		return false;
	}

	/* DEPENDENCY INJECTION */
	public void setWorkspaceName(String workspaceName) {
		this.userWkspName = workspaceName;
	}

	public void setUserAdminService(UserAdminService userAdminService) {
		this.userAdminService = userAdminService;
		userPage.setUserAdminService(userAdminService);
	}
}