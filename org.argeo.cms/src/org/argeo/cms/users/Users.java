package org.argeo.cms.users;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.argeo.cms.CmsUiProvider;
import org.argeo.cms.CmsUtils;
import org.argeo.cms.maintenance.NonAdminPage;
import org.argeo.eclipse.ui.parts.UsersTable;
import org.argeo.jcr.JcrUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class Users implements CmsUiProvider {

	NonAdminPage nap = new NonAdminPage();

	@Override
	public Control createUi(Composite parent, Node context)
			throws RepositoryException {

		if (isAdmin(context)) {
			Session session = context.getSession().getRepository()
					.login("main");
			return createUsersTable(parent, session);
		} else
			nap.createUi(parent, context);
		return null;

	}

	private Control createUsersTable(Composite parent, final Session session)
			throws RepositoryException {
		// Create the composite that displays the list and a filter
		UsersTable userTableCmp = new UsersTable(parent, SWT.NO_FOCUS, session);
		userTableCmp.populate(true, false);

		userTableCmp.setLayoutData(CmsUtils.fillAll());

		userTableCmp.addDisposeListener(new DisposeListener() {
			private static final long serialVersionUID = -8854052549807709846L;

			@Override
			public void widgetDisposed(DisposeEvent event) {
				JcrUtils.logoutQuietly(session);
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
		// FIXME clean this check one new user management policy has been
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

}
