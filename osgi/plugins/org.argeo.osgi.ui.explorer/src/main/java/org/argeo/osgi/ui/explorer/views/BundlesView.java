package org.argeo.osgi.ui.explorer.views;

import java.util.Comparator;

import org.argeo.eclipse.ui.ColumnViewerComparator;
import org.argeo.osgi.ui.explorer.OsgiExplorerImages;
import org.argeo.osgi.ui.explorer.OsgiExplorerPlugin;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

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

		// FIXME methodNotFound issue in RAP when built in Maven against RCP
		try {
			ColumnViewerToolTipSupport.enableFor(viewer);
		} catch (Exception e) {
			viewer.getControl()
					.setData(Table.ENABLE_CELL_TOOLTIP, Boolean.TRUE);
		}

		// ID
		TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setWidth(30);
		column.getColumn().setText("ID");
		column.getColumn().setAlignment(SWT.RIGHT);
		column.setLabelProvider(new ColumnLabelProvider() {
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
		column.getColumn().setWidth(16);
		column.getColumn().setText("State");
		column.setLabelProvider(new StateLabelProvider());
		new ColumnViewerComparator<Bundle>(column, new Comparator<Bundle>() {
			public int compare(Bundle o1, Bundle o2) {
				return o1.getState() - o2.getState();
			}
		});

		// Symbolic name
		column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setWidth(300);
		column.getColumn().setText("Symbolic Name");
		column.setLabelProvider(new ColumnLabelProvider() {
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
			public String getText(Object element) {
				return ((Bundle) element).getVersion().toString();
			}
		});
		new ColumnViewerComparator<Bundle>(column, new Comparator<Bundle>() {
			public int compare(Bundle o1, Bundle o2) {
				return o1.getVersion().compareTo(o2.getVersion());
			}
		});

		viewer.setInput(OsgiExplorerPlugin.getDefault().getBundle()
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

	/** Label provider for the state column */
	private static class StateLabelProvider extends ColumnLabelProvider {
		@Override
		public Image getImage(Object element) {
			Integer state = ((Bundle) element).getState();
			switch (state) {
			case Bundle.UNINSTALLED:
				return OsgiExplorerImages.INSTALLED;
			case Bundle.INSTALLED:
				return OsgiExplorerImages.INSTALLED;
			case Bundle.RESOLVED:
				return OsgiExplorerImages.RESOLVED;
			case Bundle.STARTING:
				return OsgiExplorerImages.STARTING;
			case Bundle.STOPPING:
				return OsgiExplorerImages.STARTING;
			case Bundle.ACTIVE:
				return OsgiExplorerImages.ACTIVE;
			default:
				return null;
			}
		}

		@Override
		public String getText(Object element) {
			return null;
		}

		@Override
		public String getToolTipText(Object element) {
			Bundle bundle = (Bundle) element;
			Integer state = bundle.getState();
			switch (state) {
			case Bundle.UNINSTALLED:
				return "UNINSTALLED";
			case Bundle.INSTALLED:
				return "INSTALLED";
			case Bundle.RESOLVED:
				return "RESOLVED";
			case Bundle.STARTING:
				String activationPolicy = bundle.getHeaders()
						.get(Constants.BUNDLE_ACTIVATIONPOLICY).toString();
				if (activationPolicy != null
						&& activationPolicy.equals(Constants.ACTIVATION_LAZY))
					return "<<LAZY>>";
				return "STARTING";
			case Bundle.STOPPING:
				return "STOPPING";
			case Bundle.ACTIVE:
				return "ACTIVE";
			default:
				return null;
			}
		}

	}
}
