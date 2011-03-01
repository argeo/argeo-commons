package org.argeo.security.ui.views;

import org.argeo.security.ui.internal.CurrentUser;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.part.ViewPart;

public class CurrentUserView extends ViewPart {
	private TableViewer viewer;

	@Override
	public void createPartControl(Composite parent) {

		// viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
		// | SWT.V_SCROLL);
		viewer = new TableViewer(createTable(parent));
		viewer.setContentProvider(new UsersContentProvider());
		viewer.setLabelProvider(new UsersLabelProvider());
		viewer.setInput(getViewSite());
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

		// column = new TableColumn(table, SWT.LEFT, 1);
		// column.setText("Password");
		// column.setWidth(200);

		// column = new TableColumn(table, SWT.LEFT, 2);
		// column.setText("Roles");
		// column.setWidth(300);

		return table;
	}

	@Override
	public void setFocus() {
		viewer.getTable().setFocus();
	}

	private class UsersContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		public Object[] getChildren(Object parentElement) {
			// ILoginContext secureContext = LoginContextFactory
			// .createContext("SPRING");
			// try {
			// secureContext.login();
			// } catch (LoginException e) {
			// // login failed
			// }
			//
			// Subject subject = null;
			// // subject = Subject.getSubject(AccessController.getContext());
			// try {
			// subject = secureContext.getSubject();
			// } catch (Exception e) {
			// e.printStackTrace();
			// throw new ArgeoException("Cannot retrieve subject", e);
			// }
			//
			// if (subject == null)
			// throw new ArgeoException("No subject found");
			// return subject.getPrincipals().toArray();
			return CurrentUser.roles().toArray();
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
			// Principal argeoUser = (Principal) element;
			// switch (columnIndex) {
			// case 0:
			// return argeoUser.getName();
			// case 1:
			// return argeoUser.toString();
			// default:
			// throw new ArgeoException("Unmanaged column " + columnIndex);
			// }
			return element.toString();
		}

		public Image getColumnImage(Object element, int columnIndex) {
			// TODO Auto-generated method stub
			return null;
		}

	}

}
