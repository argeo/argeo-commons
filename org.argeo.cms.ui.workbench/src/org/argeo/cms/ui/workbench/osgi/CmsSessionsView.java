//package org.argeo.eclipse.ui.workbench.osgi;
//public class BundlesView {}

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
package org.argeo.cms.ui.workbench.osgi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.naming.ldap.LdapName;

import org.argeo.cms.CmsException;
import org.argeo.cms.auth.CmsSession;
import org.argeo.cms.ui.workbench.WorkbenchUiPlugin;
import org.argeo.eclipse.ui.ColumnViewerComparator;
import org.argeo.eclipse.ui.specific.EclipseUiSpecificUtils;
import org.argeo.util.LangUtils;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Overview of the active CMS sessions.
 */
public class CmsSessionsView extends ViewPart {
	private TableViewer viewer;

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent);
		viewer.setContentProvider(new CmsSessionContentProvider());
		viewer.getTable().setHeaderVisible(true);

		EclipseUiSpecificUtils.enableToolTipSupport(viewer);

		int longColWidth = 150;
		int smallColWidth = 100;

		// Display name
		TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setWidth(longColWidth);
		column.getColumn().setText("User");
		column.setLabelProvider(new ColumnLabelProvider() {
			private static final long serialVersionUID = -5234573509093747505L;

			public String getText(Object element) {
				return ((CmsSession) element).getAuthorization().toString();
			}

			public String getToolTipText(Object element) {
				return ((CmsSession) element).getUserDn().toString();
			}
		});
		new ColumnViewerComparator<CmsSession>(column, new Comparator<CmsSession>() {
			public int compare(CmsSession o1, CmsSession o2) {
				return o1.getAuthorization().toString().compareTo(o2.getAuthorization().toString());
			}
		});

		// Creation time
		column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setWidth(smallColWidth);
		column.getColumn().setText("Since");
		column.setLabelProvider(new ColumnLabelProvider() {
			private static final long serialVersionUID = -5234573509093747505L;

			public String getText(Object element) {
				return LangUtils.since(((CmsSession) element).getCreationTime());
			}

			public String getToolTipText(Object element) {
				return ((CmsSession) element).getCreationTime().toString();
			}
		});
		new ColumnViewerComparator<CmsSession>(column, new Comparator<CmsSession>() {
			public int compare(CmsSession o1, CmsSession o2) {
				return o1.getCreationTime().compareTo(o2.getCreationTime());
			}
		});

		// Username
		column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setWidth(smallColWidth);
		column.getColumn().setText("Username");
		column.setLabelProvider(new ColumnLabelProvider() {
			private static final long serialVersionUID = -5234573509093747505L;

			public String getText(Object element) {
				LdapName userDn = ((CmsSession) element).getUserDn();
				return userDn.getRdn(userDn.size() - 1).getValue().toString();
			}

			public String getToolTipText(Object element) {
				return ((CmsSession) element).getUserDn().toString();
			}
		});
		new ColumnViewerComparator<CmsSession>(column, new Comparator<CmsSession>() {
			public int compare(CmsSession o1, CmsSession o2) {
				return o1.getUserDn().compareTo(o2.getUserDn());
			}
		});

		// UUID
		column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setWidth(smallColWidth);
		column.getColumn().setText("UUID");
		column.setLabelProvider(new ColumnLabelProvider() {
			private static final long serialVersionUID = -5234573509093747505L;

			public String getText(Object element) {
				return ((CmsSession) element).getUuid().toString();
			}

			public String getToolTipText(Object element) {
				return getText(element);
			}
		});
		new ColumnViewerComparator<CmsSession>(column, new Comparator<CmsSession>() {
			public int compare(CmsSession o1, CmsSession o2) {
				return o1.getUuid().compareTo(o2.getUuid());
			}
		});

		// Local ID
		column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setWidth(smallColWidth);
		column.getColumn().setText("Local ID");
		column.setLabelProvider(new ColumnLabelProvider() {
			private static final long serialVersionUID = -5234573509093747505L;

			public String getText(Object element) {
				return ((CmsSession) element).getLocalId();
			}

			public String getToolTipText(Object element) {
				return getText(element);
			}
		});
		new ColumnViewerComparator<CmsSession>(column, new Comparator<CmsSession>() {
			public int compare(CmsSession o1, CmsSession o2) {
				return o1.getLocalId().compareTo(o2.getLocalId());
			}
		});

		viewer.setInput(WorkbenchUiPlugin.getDefault().getBundle().getBundleContext());

	}

	@Override
	public void setFocus() {
		if (viewer != null)
			viewer.getControl().setFocus();
	}

	/** Content provider managing the array of bundles */
	private static class CmsSessionContentProvider implements IStructuredContentProvider {
		private static final long serialVersionUID = -8533792785725875977L;

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof BundleContext) {
				BundleContext bc = (BundleContext) inputElement;
				Collection<ServiceReference<CmsSession>> srs;
				try {
					srs = bc.getServiceReferences(CmsSession.class, null);
				} catch (InvalidSyntaxException e) {
					throw new CmsException("Cannot retrieve CMS sessions", e);
				}
				List<CmsSession> res = new ArrayList<>();
				for (ServiceReference<CmsSession> sr : srs) {
					res.add(bc.getService(sr));
				}
				return res.toArray();
			}
			return null;
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}
}
