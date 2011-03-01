package org.argeo.security.ui.views;

import java.util.ArrayList;

import org.argeo.ArgeoException;
import org.argeo.security.ArgeoSecurityService;
import org.argeo.security.ArgeoUser;
import org.argeo.security.nature.SimpleUserNature;
import org.argeo.security.ui.SecurityUiPlugin;
import org.argeo.security.ui.commands.OpenArgeoUserEditor;
import org.argeo.security.ui.internal.CurrentUser;
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
public class UsersView extends ViewPart {
	public final static String ID = "org.argeo.security.ui.usersView";

	private TableViewer viewer;
	private ArgeoSecurityService securityService;

	private String simpleNatureType = null;

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(createTable(parent));
		viewer.setContentProvider(new UsersContentProvider());
		viewer.setLabelProvider(new UsersLabelProvider());
		viewer.addDoubleClickListener(new ViewDoubleClickListener());
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

	public void setSecurityService(ArgeoSecurityService securityService) {
		this.securityService = securityService;
	}

	public void setSimpleNatureType(String simpleNatureType) {
		this.simpleNatureType = simpleNatureType;
	}

	public void refresh() {
		viewer.refresh();
	}

	private class UsersContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object inputElement) {
			return securityService.listUsers().toArray();
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

	private class UsersLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		public String getColumnText(Object element, int columnIndex) {
			String currentUsername = CurrentUser.getUsername();
			ArgeoUser user = (ArgeoUser) element;
			SimpleUserNature simpleNature = SimpleUserNature
					.findSimpleUserNature(user, simpleNatureType);
			switch (columnIndex) {
			case 0:
				String userName = user.getUsername();
				if (userName.equals(currentUsername))
					userName = userName + "*";
				return userName;
			case 1:
				return simpleNature.getFirstName();
			case 2:
				return simpleNature.getLastName();
			case 3:
				return simpleNature.getEmail();
			default:
				throw new ArgeoException("Unmanaged column " + columnIndex);
			}
		}

		public Image getColumnImage(Object element, int columnIndex) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	class ViewDoubleClickListener implements IDoubleClickListener {
		public void doubleClick(DoubleClickEvent evt) {
			Object obj = ((IStructuredSelection) evt.getSelection())
					.getFirstElement();

			if (obj instanceof ArgeoUser) {
				ArgeoUser argeoUser = (ArgeoUser) obj;

				IWorkbench iw = SecurityUiPlugin.getDefault().getWorkbench();
				IHandlerService handlerService = (IHandlerService) iw
						.getService(IHandlerService.class);
				try {
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
							argeoUser.getUsername());
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
