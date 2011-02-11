package org.argeo.security.ui.views;

import java.util.ArrayList;

import org.argeo.ArgeoException;
import org.argeo.security.ArgeoSecurityService;
import org.argeo.security.ArgeoUser;
import org.argeo.security.ui.SecurityUiPlugin;
import org.argeo.security.ui.commands.AddRole;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ViewPart;

/** List all roles. */
public class RolesView extends ViewPart {
	public final static String ID = "org.argeo.security.ui.rolesView";

	private Text newRole;

	private TableViewer viewer;
	private ArgeoSecurityService securityService;

	private String addNewRoleText = "<add new role here>";

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(1, false));

		// new role text field
		newRole = new Text(parent, SWT.BORDER);
		newRole.setText(addNewRoleText);
		newRole.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		// default action is add role
		newRole.addListener(SWT.DefaultSelection, new Listener() {
			public void handleEvent(Event evt) {
				IWorkbench iw = SecurityUiPlugin.getDefault().getWorkbench();
				IHandlerService handlerService = (IHandlerService) iw
						.getService(IHandlerService.class);
				try {
					handlerService.executeCommand(AddRole.COMMAND_ID, evt);
				} catch (Exception e) {
					throw new ArgeoException("Cannot execute add role command",
							e);
				}
			}
		});
		// select all on focus
		newRole.addListener(SWT.FocusIn, new Listener() {
			public void handleEvent(Event e) {
				newRole.selectAll();
			}
		});

		// roles table
		Table table = new Table(parent, SWT.V_SCROLL | SWT.BORDER);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setLinesVisible(false);
		table.setHeaderVisible(false);
		viewer = new TableViewer(table);
		viewer.setContentProvider(new RolesContentProvider());
		viewer.setLabelProvider(new UsersLabelProvider());
		viewer.setInput(getViewSite());
		viewer.addDoubleClickListener(new ViewDoubleClickListener());
	}

	@Override
	public void setFocus() {
		viewer.getTable().setFocus();
	}

	public void setSecurityService(ArgeoSecurityService securityService) {
		this.securityService = securityService;
	}

	public String getAddNewRoleText() {
		return addNewRoleText;
	}

	private class RolesContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object inputElement) {
			return securityService.listEditableRoles().toArray();
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

	private class UsersLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		public String getColumnText(Object element, int columnIndex) {
			return element.toString();
		}

		public Image getColumnImage(Object element, int columnIndex) {
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

	public String getNewRole() {
		return newRole.getText();
	}

	public void refresh() {
		viewer.refresh();
		newRole.setText(addNewRoleText);
	}
}
