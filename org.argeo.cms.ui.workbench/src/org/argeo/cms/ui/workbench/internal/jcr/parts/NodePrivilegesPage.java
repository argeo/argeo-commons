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
package org.argeo.cms.ui.workbench.internal.jcr.parts;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.argeo.cms.ui.workbench.WorkbenchUiPlugin;
import org.argeo.eclipse.ui.EclipseUiException;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * Display and edit a given node privilege. For the time being it is completely
 * JackRabbit specific (and hardcoded for this) and will display an empty page
 * if using any other implementation
 */
public class NodePrivilegesPage extends FormPage {

	private Node context;

	private TableViewer viewer;

	public NodePrivilegesPage(FormEditor editor, String title, Node context) {
		super(editor, "NodePrivilegesPage", title);
		this.context = context;
	}

	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		form.setText(WorkbenchUiPlugin
				.getMessage("nodeRightsManagementPageTitle"));
		FillLayout layout = new FillLayout();
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		form.getBody().setLayout(layout);
		if (isJackRabbit())
			createRightsPart(form.getBody());
	}

	/** Creates the authorization part */
	protected void createRightsPart(Composite parent) {
		Table table = new Table(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		viewer = new TableViewer(table);

		// Group / user name
		TableViewerColumn column = createTableViewerColumn(viewer,
				"User/Group Name", 280);
		column.setLabelProvider(new ColumnLabelProvider() {
			private static final long serialVersionUID = -2290781173498395973L;

			public String getText(Object element) {
				Node node = (Node) element;
				try {
					if (node.hasProperty("rep:principalName"))
						return node.getProperty("rep:principalName")
								.getString();
				} catch (RepositoryException e) {
					throw new EclipseUiException("Unable to retrieve "
							+ "principal name on " + node, e);
				}
				return "";
			}

			public Image getImage(Object element) {
				return null;
			}
		});

		// Privileges
		column = createTableViewerColumn(viewer, "Assigned privileges", 300);
		column.setLabelProvider(new ColumnLabelProvider() {
			private static final long serialVersionUID = -2290781173498395973L;
			private String propertyName = "rep:privileges";

			public String getText(Object element) {
				Node node = (Node) element;
				try {
					if (node.hasProperty(propertyName)) {
						String separator = ", ";
						Value[] langs = node.getProperty(propertyName)
								.getValues();
						StringBuilder builder = new StringBuilder();
						for (Value val : langs) {
							String currStr = val.getString();
							builder.append(currStr).append(separator);
						}
						if (builder.lastIndexOf(separator) >= 0)
							return builder.substring(0, builder.length()
									- separator.length());
						else
							return builder.toString();

					}
				} catch (RepositoryException e) {
					throw new EclipseUiException("Unable to retrieve "
							+ "privileges on " + node, e);
				}
				return "";
			}

			public Image getImage(Object element) {
				return null;
			}
		});

		// Relevant node
		column = createTableViewerColumn(viewer, "Relevant node", 300);
		column.setLabelProvider(new ColumnLabelProvider() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 4245522992038244849L;

			public String getText(Object element) {
				Node node = (Node) element;
				try {
					return node.getParent().getParent().getPath();
				} catch (RepositoryException e) {
					throw new EclipseUiException("Unable get path for " + node,
							e);
				}
			}

			public Image getImage(Object element) {
				return null;
			}
		});

		viewer.setContentProvider(new RightsContentProvider());
		viewer.setInput(getEditorSite());
	}

	protected TableViewerColumn createTableViewerColumn(TableViewer viewer,
			String title, int bound) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer,
				SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}

	private class RightsContentProvider implements IStructuredContentProvider {
		private static final long serialVersionUID = -7631476348552802706L;

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		// TODO JackRabbit specific retrieval of authorization. Clean and
		// generalize
		public Object[] getElements(Object inputElement) {
			try {
				List<Node> privs = new ArrayList<Node>();

				Node currNode = context;
				String currPath = currNode.getPath();

				loop: while (true) {
					if (currNode.hasNode("rep:policy")) {
						NodeIterator nit = currNode.getNode("rep:policy")
								.getNodes();
						while (nit.hasNext()) {
							Node currPrivNode = nit.nextNode();
							if (currPrivNode.getName().startsWith("allow"))
								privs.add(currPrivNode);
						}
					}
					if ("/".equals(currPath))
						break loop;
					else {
						currNode = currNode.getParent();
						currPath = currNode.getPath();
					}
				}

				// AccessControlManager acm = context.getSession()
				// .getAccessControlManager();
				// AccessControlPolicyIterator acpi = acm
				// .getApplicablePolicies(context.getPath());
				//
				// List<AccessControlPolicy> acps = new
				// ArrayList<AccessControlPolicy>();
				// try {
				// while (true) {
				// Object obj = acpi.next();
				// acps.add((AccessControlPolicy) obj);
				// }
				// } catch (Exception e) {
				// // No more elements
				// }
				//
				// AccessControlList acl = ((AccessControlList) acps.get(0));
				// AccessControlEntry[] entries = acl.getAccessControlEntries();

				return privs.toArray();
			} catch (Exception e) {
				throw new EclipseUiException(
						"Cannot retrieve authorization for " + context, e);
			}
		}
	}

	// simply check if we are using jackrabbit without adding code dependencies
	private boolean isJackRabbit() {
		try {
			String cname = context.getSession().getClass().getName();
			return cname.startsWith("org.apache.jackrabbit");
		} catch (RepositoryException e) {
			throw new EclipseUiException(
					"Cannot check JCR implementation used on " + context, e);
		}
	}

}