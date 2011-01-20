package org.argeo.security.ui.views;

import java.util.ArrayList;

import org.argeo.ArgeoException;
import org.argeo.security.ArgeoSecurityService;
import org.argeo.security.ArgeoUser;
import org.argeo.security.equinox.CurrentUser;
import org.argeo.security.ui.SecurityUiPlugin;
import org.argeo.security.ui.commands.OpenArgeoUserEditor;
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

public class UsersView extends ViewPart {
	private TableViewer viewer;
	private ArgeoSecurityService securityService;

	@Override
	public void createPartControl(Composite parent) {

		// viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
		// | SWT.V_SCROLL);
		viewer = new TableViewer(createTable(parent));
		viewer.setContentProvider(new UsersContentProvider());
		viewer.setLabelProvider(new UsersLabelProvider());
		viewer.setInput(getViewSite());
		viewer.addDoubleClickListener(new ViewDoubleClickListener());
		// viewer.setInput(SecurityContextHolder.getContext());
	}

	protected Table createTable(Composite parent) {
		int style = SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL;

		Table table = new Table(parent, style);

		// GridData gridData = new GridData(GridData.FILL_BOTH);
		// gridData.grabExcessVerticalSpace = true;
		// gridData.grabExcessHorizontalSpace = true;
		// gridData.horizontalSpan = 3;
		// table.setLayoutData(gridData);

		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		TableColumn column = new TableColumn(table, SWT.LEFT, 0);
		column.setText("ID");
		column.setWidth(100);

		column = new TableColumn(table, SWT.LEFT, 1);
		column.setText("Password");
		column.setWidth(200);

		// column = new TableColumn(table, SWT.LEFT, 2);
		// column.setText("Roles");
		// column.setWidth(300);

		return table;
	}

	@Override
	public void setFocus() {
		viewer.getTable().setFocus();
	}

	public void setSecurityService(ArgeoSecurityService securityService) {
		this.securityService = securityService;
	}

	private class UsersContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		public Object[] getChildren(Object parentElement) {
			// try {
			// Thread.sleep(3000);
			// } catch (InterruptedException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }

			return securityService.getSecurityDao().listUsers().toArray();
		}

		public void dispose() {
			// TODO Auto-generated method stub

		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// TODO Auto-generated method stub

		}

	}

	private class UsersLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		public String getColumnText(Object element, int columnIndex) {
			String currentUsername = CurrentUser.getUsername();
			ArgeoUser argeoUser = (ArgeoUser) element;
			switch (columnIndex) {
			case 0:
				String userName = argeoUser.getUsername();
				if (userName.equals(currentUsername))
					userName = userName + "*";
				return userName;
			case 1:
				return argeoUser.getPassword();
			case 2:
				return argeoUser.getRoles().toString();
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
