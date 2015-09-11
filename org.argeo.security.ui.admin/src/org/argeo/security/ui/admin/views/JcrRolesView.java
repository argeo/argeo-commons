/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.security.ui.admin.views;

public class JcrRolesView {
}

/** List all roles. Legacy. TODO Remove */
// public class JcrRolesView extends ViewPart {
// public final static String ID = SecurityAdminPlugin.PLUGIN_ID
// + ".jcrRolesView";
//
// private Text newRole;
//
// private TableViewer viewer;
// private UserAdminService userAdminService;
//
// private String addNewRoleText = "<add new role here>";
//
// @Override
// public void createPartControl(Composite parent) {
// parent.setLayout(new GridLayout(1, false));
//
// // new role text field
// newRole = new Text(parent, SWT.BORDER);
// newRole.setText(addNewRoleText);
// newRole.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
// // default action is add role
// newRole.addListener(SWT.DefaultSelection, new Listener() {
// private static final long serialVersionUID = -2367261849260929505L;
//
// public void handleEvent(Event evt) {
// IWorkbench iw = SecurityAdminPlugin.getDefault().getWorkbench();
// IHandlerService handlerService = (IHandlerService) iw
// .getService(IHandlerService.class);
// try {
// handlerService.executeCommand(AddRole.COMMAND_ID, evt);
// } catch (Exception e) {
// throw new ArgeoException("Cannot execute add role command",
// e);
// }
// }
// });
// // select all on focus
// newRole.addListener(SWT.FocusIn, new Listener() {
// private static final long serialVersionUID = 2612811281477034356L;
//
// public void handleEvent(Event e) {
// newRole.selectAll();
// }
// });
//
// // roles table
// Table table = new Table(parent, SWT.V_SCROLL | SWT.BORDER);
// table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
// table.setLinesVisible(false);
// table.setHeaderVisible(false);
// viewer = new TableViewer(table);
// viewer.setContentProvider(new RolesContentProvider());
// viewer.setLabelProvider(new UsersLabelProvider());
// getViewSite().setSelectionProvider(viewer);
// viewer.setInput(getViewSite());
// }
//
// @Override
// public void setFocus() {
// viewer.getTable().setFocus();
// }
//
// public void setUserAdminService(UserAdminService userAdminService) {
// this.userAdminService = userAdminService;
// }
//
// public String getAddNewRoleText() {
// return addNewRoleText;
// }
//
// private class RolesContentProvider implements IStructuredContentProvider {
// private static final long serialVersionUID = 7446442682717419289L;
//
// public Object[] getElements(Object inputElement) {
// return userAdminService.listEditableRoles().toArray();
// }
//
// public void dispose() {
// }
//
// public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
// }
//
// }
//
// private class UsersLabelProvider extends LabelProvider implements
// ITableLabelProvider {
// private static final long serialVersionUID = -1886204791002421430L;
//
// public String getColumnText(Object element, int columnIndex) {
// return element.toString();
// }
//
// public Image getColumnImage(Object element, int columnIndex) {
// return null;
// }
//
// }
//
// public String getNewRole() {
// return newRole.getText();
// }
//
// public void refresh() {
// viewer.refresh();
// newRole.setText(addNewRoleText);
// }
// }
