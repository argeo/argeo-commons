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

import java.util.Comparator;

import org.argeo.cms.ui.workbench.WorkbenchUiPlugin;
import org.argeo.eclipse.ui.ColumnViewerComparator;
import org.argeo.eclipse.ui.specific.EclipseUiSpecificUtils;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Overview of the bundles as a table. Equivalent to Equinox 'ss' console
 * command.
 */
public class BundlesView extends ViewPart {
	private TableViewer viewer;

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent);
		viewer.setContentProvider(new BundleContentProvider());
		viewer.getTable().setHeaderVisible(true);

		EclipseUiSpecificUtils.enableToolTipSupport(viewer);

		// ID
		TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setWidth(30);
		column.getColumn().setText("ID");
		column.getColumn().setAlignment(SWT.RIGHT);
		column.setLabelProvider(new ColumnLabelProvider() {
			private static final long serialVersionUID = -3122136344359358605L;

			public String getText(Object element) {
				return Long.toString(((Bundle) element).getBundleId());
			}
		});
		new ColumnViewerComparator<Bundle>(column, new Comparator<Bundle>() {
			public int compare(Bundle o1, Bundle o2) {
				return (int) (o1.getBundleId() - o2.getBundleId());
			}
		});

		// State
		column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setWidth(18);
		column.getColumn().setText("State");
		column.setLabelProvider(new StateLabelProvider());
		new ColumnViewerComparator<Bundle>(column, new Comparator<Bundle>() {
			public int compare(Bundle o1, Bundle o2) {
				return o1.getState() - o2.getState();
			}
		});

		// Symbolic name
		column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setWidth(250);
		column.getColumn().setText("Symbolic Name");
		column.setLabelProvider(new ColumnLabelProvider() {
			private static final long serialVersionUID = -4280840684440451080L;

			public String getText(Object element) {
				return ((Bundle) element).getSymbolicName();
			}
		});
		new ColumnViewerComparator<Bundle>(column, new Comparator<Bundle>() {
			public int compare(Bundle o1, Bundle o2) {
				return o1.getSymbolicName().compareTo(o2.getSymbolicName());
			}
		});

		// Version
		column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setWidth(150);
		column.getColumn().setText("Version");
		column.setLabelProvider(new ColumnLabelProvider() {
			private static final long serialVersionUID = 6871926308708629989L;

			public String getText(Object element) {
				Bundle bundle = (org.osgi.framework.Bundle) element;
				return bundle.getVersion().toString();
			}
		});
		new ColumnViewerComparator<Bundle>(column, new Comparator<Bundle>() {
			public int compare(Bundle o1, Bundle o2) {
				return o1.getVersion().compareTo(o2.getVersion());
			}
		});

		viewer.setInput(WorkbenchUiPlugin.getDefault().getBundle()
				.getBundleContext());

	}

	@Override
	public void setFocus() {
		if (viewer != null)
			viewer.getControl().setFocus();
	}

	/** Content provider managing the array of bundles */
	private static class BundleContentProvider implements
			IStructuredContentProvider {
		private static final long serialVersionUID = -8533792785725875977L;

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof BundleContext) {
				BundleContext bc = (BundleContext) inputElement;
				return bc.getBundles();
			}
			return null;
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}
}
