package org.argeo.security.ui.admin.views;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;

import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.ArgeoTypes;
import org.argeo.security.ui.admin.SecurityAdminPlugin;
import org.argeo.security.ui.admin.commands.OpenArgeoUserEditor;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ViewPart;

/** List all users. */
public class UsersView extends ViewPart implements ArgeoNames, ArgeoTypes,
		EventListener {
	public final static String ID = "org.argeo.security.ui.admin.adminUsersView";

	private TableViewer viewer;
	private Session session;

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(createTable(parent));
		viewer.setContentProvider(new UsersContentProvider());
		viewer.setLabelProvider(new UsersLabelProvider());
		viewer.addDoubleClickListener(new ViewDoubleClickListener());
		getViewSite().setSelectionProvider(viewer);
		viewer.setInput(getViewSite());
	}

	protected Table createTable(Composite parent) {
		Table table = new Table(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		TableColumn column = new TableColumn(table, SWT.LEFT, 0);
		column.setText("User");
		column.setWidth(50);
		column = new TableColumn(table, SWT.LEFT, 1);
		column.setText("First Name");
		column.setWidth(100);
		column = new TableColumn(table, SWT.LEFT, 2);
		column.setText("Last Name");
		column.setWidth(100);
		column = new TableColumn(table, SWT.LEFT, 3);
		column.setText("E-mail");
		column.setWidth(100);
		return table;
	}

	@Override
	public void setFocus() {
		viewer.getTable().setFocus();
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public void refresh() {
		viewer.refresh();
	}

	@Override
	public void onEvent(EventIterator events) {
		viewer.refresh();
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

	private class UsersLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		public String getColumnText(Object element, int columnIndex) {
			try {
				Node userHome = (Node) element;
				Node userProfile = userHome.getNode(ARGEO_PROFILE);
				switch (columnIndex) {
				case 0:
					String userName = userHome.getProperty(ARGEO_USER_ID)
							.getString();
					if (userName.equals(session.getUserID()))
						return "[" + userName + "]";
					else
						return userName;
				case 1:
					return userProfile.hasProperty(ARGEO_FIRST_NAME) ? userProfile
							.getProperty(ARGEO_FIRST_NAME).getString() : "";
				case 2:
					return userProfile.hasProperty(ARGEO_LAST_NAME) ? userProfile
							.getProperty(ARGEO_LAST_NAME).getString() : "";
				case 3:
					return userProfile.hasProperty(ARGEO_PRIMARY_EMAIL) ? userProfile
							.getProperty(ARGEO_PRIMARY_EMAIL).getString() : "";
				default:
					throw new ArgeoException("Unmanaged column " + columnIndex);
				}
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot get text", e);
			}
		}

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
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
