package org.argeo.security.ui.admin.views;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.specific.EclipseUiSpecificUtils;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.ArgeoTypes;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.argeo.security.ui.admin.commands.OpenArgeoUserEditor;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ViewPart;

/** List all users. */
public class UsersView extends ViewPart implements ArgeoNames, ArgeoTypes {
	public final static String ID = "org.argeo.security.ui.admin.adminUsersView";

	private TableViewer viewer;
	private Session session;

	private UserStructureListener userStructureListener;

	private Font italic;
	private Font bold;

	@Override
	public void createPartControl(Composite parent) {
		italic = EclipseUiUtils.getItalicFont(parent);
		bold = EclipseUiUtils.getBoldFont(parent);

		// viewer = new TableViewer(createTable(parent));
		viewer = createTableViewer(parent);
		EclipseUiSpecificUtils.enableToolTipSupport(viewer);
		viewer.setContentProvider(new UsersContentProvider());
		// viewer.setLabelProvider(new UsersLabelProvider());
		viewer.addDoubleClickListener(new ViewDoubleClickListener());
		getViewSite().setSelectionProvider(viewer);
		viewer.setInput(getViewSite());

		userStructureListener = new UserStructureListener();
		JcrUtils.addListener(session, userStructureListener, Event.NODE_ADDED
				| Event.NODE_REMOVED, JcrUtils.DEFAULT_HOME_BASE_PATH,
				ArgeoTypes.ARGEO_USER_HOME);
	}

	protected TableViewer createTableViewer(final Composite parent) {

		Table table = new Table(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		TableViewer viewer = new TableViewer(table);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		// User ID
		TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText("User ID");
		column.getColumn().setWidth(100);
		column.setLabelProvider(new CLProvider() {
			public String getText(Object elem) {
				return getProperty(elem, ARGEO_USER_ID);
				// if (username.equals(session.getUserID()))
				// return "[" + username + "]";
				// else
				// return username;
			}
		});

		// Displayed name
		column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText("Name");
		column.getColumn().setWidth(150);
		column.setLabelProvider(new CLProvider() {
			public String getText(Object elem) {
				return getProperty(elem, Property.JCR_TITLE);
			}
		});

		// E-mail
		column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText("E-mail");
		column.getColumn().setWidth(150);
		column.setLabelProvider(new CLProvider() {
			public String getText(Object elem) {
				return getProperty(elem, ARGEO_PRIMARY_EMAIL);
			}
		});

		// E-mail
		column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText("Description");
		column.getColumn().setWidth(200);
		column.setLabelProvider(new CLProvider() {
			public String getText(Object elem) {
				return getProperty(elem, Property.JCR_DESCRIPTION);
			}
		});

		return viewer;
	}

	private class CLProvider extends ColumnLabelProvider {

		public String getToolTipText(Object element) {
			return getText(element);
		}

		@Override
		public Font getFont(Object elem) {
			// self
			String username = getProperty(elem, ARGEO_USER_ID);
			if (username.equals(session.getUserID()))
				return bold;

			// disabled
			try {
				Node userHome = (Node) elem;
				Node userProfile = userHome.getNode(ARGEO_PROFILE);
				if (!userProfile.getProperty(ARGEO_ENABLED).getBoolean())
					return italic;
				else
					return null;
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot get font for " + username, e);
			}
		}

	}

	// protected Table createTable(Composite parent) {
	// // TODO use a more flexible API
	// Table table = new Table(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
	// table.setLinesVisible(true);
	// table.setHeaderVisible(true);
	// TableColumn column = new TableColumn(table, SWT.LEFT, 0);
	// column.setText("Username");
	// column.setWidth(100);
	// column = new TableColumn(table, SWT.LEFT, 1);
	// column.setText("Displayed name");
	// column.setWidth(150);
	// column = new TableColumn(table, SWT.LEFT, 2);
	// column.setText("E-mail");
	// column.setWidth(100);
	// column = new TableColumn(table, SWT.LEFT, 3);
	// column.setText("First Name");
	// column.setWidth(100);
	// column = new TableColumn(table, SWT.LEFT, 4);
	// column.setText("Last Name");
	// column.setWidth(100);
	// column = new TableColumn(table, SWT.LEFT, 5);
	// column.setText("Status");
	// column.setWidth(50);
	// column = new TableColumn(table, SWT.LEFT, 6);
	// column.setText("Description");
	// column.setWidth(200);
	// return table;
	// }

	// private class UsersLabelProvider extends LabelProvider implements
	// ITableLabelProvider {
	// public String getColumnText(Object element, int columnIndex) {
	// try {
	// Node userHome = (Node) element;
	// Node userProfile = userHome.getNode(ARGEO_PROFILE);
	// switch (columnIndex) {
	// case 0:
	// String username = userHome.getProperty(ARGEO_USER_ID)
	// .getString();
	// if (username.equals(session.getUserID()))
	// return "[" + username + "]";
	// else
	// return username;
	// case 1:
	// return getProperty(userProfile, Property.JCR_TITLE);
	// case 2:
	// return getProperty(userProfile, ARGEO_PRIMARY_EMAIL);
	// case 3:
	// return getProperty(userProfile, ARGEO_FIRST_NAME);
	// case 4:
	// return getProperty(userProfile, ARGEO_LAST_NAME);
	// case 5:
	// return userProfile.getProperty(ARGEO_ENABLED).getBoolean() ? ""
	// : "disabled";
	// case 6:
	// return getProperty(userProfile, Property.JCR_DESCRIPTION);
	// default:
	// throw new ArgeoException("Unmanaged column " + columnIndex);
	// }
	// } catch (RepositoryException e) {
	// throw new ArgeoException("Cannot get text", e);
	// }
	// }
	//
	// public Image getColumnImage(Object element, int columnIndex) {
	// return null;
	// }
	//
	// }

	@Override
	public void setFocus() {
		viewer.getTable().setFocus();
	}

	@Override
	public void dispose() {
		JcrUtils.removeListenerQuietly(session, userStructureListener);
		super.dispose();
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public void refresh() {
		viewer.refresh();
	}

	protected String getProperty(Node userProfile, String name)
			throws RepositoryException {
		return userProfile.hasProperty(name) ? userProfile.getProperty(name)
				.getString() : "";
	}

	protected String getProperty(Object element, String name) {
		try {
			Node userHome = (Node) element;
			Node userProfile = userHome.getNode(ARGEO_PROFILE);
			return userProfile.hasProperty(name) ? userProfile
					.getProperty(name).getString() : "";
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot get property " + name, e);
		}
	}

	private class UserStructureListener implements EventListener {

		@Override
		public void onEvent(EventIterator events) {
			viewer.refresh();
		}
	}

	private class UsersContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object inputElement) {
			try {
				Query query = session
						.getWorkspace()
						.getQueryManager()
						.createQuery(
								"select [" + ARGEO_PROFILE + "] from ["
										+ ARGEO_USER_HOME + "]", Query.JCR_SQL2);
				NodeIterator nit = query.execute().getNodes();
				List<Node> userProfiles = new ArrayList<Node>();
				while (nit.hasNext()) {
					userProfiles.add(nit.nextNode());
				}
				return userProfiles.toArray();
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot list users", e);
			}
			// return userAdminService.listUsers().toArray();
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

	class ViewDoubleClickListener implements IDoubleClickListener {
		public void doubleClick(DoubleClickEvent evt) {
			if (evt.getSelection().isEmpty())
				return;

			Object obj = ((IStructuredSelection) evt.getSelection())
					.getFirstElement();
			if (obj instanceof Node) {
				IWorkbench iw = SecurityAdminPlugin.getDefault().getWorkbench();
				IHandlerService handlerService = (IHandlerService) iw
						.getService(IHandlerService.class);
				try {
					String username = ((Node) obj).getProperty(ARGEO_USER_ID)
							.getString();
					String commandId = OpenArgeoUserEditor.COMMAND_ID;
					String paramName = OpenArgeoUserEditor.PARAM_USERNAME;

					// TODO: factorize this
					// execute related command
					IWorkbenchWindow window = iw.getActiveWorkbenchWindow();
					ICommandService cmdService = (ICommandService) window
							.getService(ICommandService.class);
					Command cmd = cmdService.getCommand(commandId);
					ArrayList<Parameterization> parameters = new ArrayList<Parameterization>();
					IParameter iparam = cmd.getParameter(paramName);
					Parameterization param = new Parameterization(iparam,
							username);
					parameters.add(param);
					ParameterizedCommand pc = new ParameterizedCommand(cmd,
							parameters.toArray(new Parameterization[parameters
									.size()]));
					handlerService = (IHandlerService) window
							.getService(IHandlerService.class);
					handlerService.executeCommand(pc, null);
				} catch (Exception e) {
					throw new ArgeoException("Cannot open editor", e);
				}

			}
		}
	}

}
