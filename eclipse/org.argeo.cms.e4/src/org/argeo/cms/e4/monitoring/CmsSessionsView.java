//package org.argeo.eclipse.ui.workbench.osgi;
//public class BundlesView {}

package org.argeo.cms.e4.monitoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.naming.ldap.LdapName;

import org.argeo.api.cms.CmsSession;
import org.argeo.eclipse.ui.ColumnViewerComparator;
import org.argeo.eclipse.ui.specific.EclipseUiSpecificUtils;
import org.argeo.util.LangUtils;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Overview of the active CMS sessions.
 */
public class CmsSessionsView {
	private final static BundleContext bc = FrameworkUtil.getBundle(CmsSessionsView.class).getBundleContext();

	private TableViewer viewer;

	@PostConstruct
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
				return ((CmsSession) element).getDisplayName();
			}

			public String getToolTipText(Object element) {
				return ((CmsSession) element).getUserDn().toString();
			}
		});
		new ColumnViewerComparator(column);

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
		new ColumnViewerComparator(column);

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
		new ColumnViewerComparator(column);

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
		new ColumnViewerComparator(column);

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
		new ColumnViewerComparator(column);

		viewer.setInput(bc);

	}

	@Focus
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
					throw new IllegalArgumentException("Cannot retrieve CMS sessions", e);
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
