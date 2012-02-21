package org.argeo.security.ui.views;

import java.util.ArrayList;

import org.argeo.ArgeoLogListener;
import org.argeo.security.ui.SecurityUiPlugin;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Display log lines with a virtual table. Register and unregisters a
 * {@link ArgeoLogListener} via OSGi services.
 */
public class LogView extends ViewPart {
	public static String ID = SecurityUiPlugin.PLUGIN_ID + ".logView";
	
	private TableViewer viewer;

	private LogContentProvider logContentProvider;

	private ServiceRegistration serviceRegistration;

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.VIRTUAL);
		viewer.setLabelProvider(new LabelProvider());
		logContentProvider = new LogContentProvider(viewer);
		serviceRegistration = getBundleContext().registerService(
				ArgeoLogListener.class.getName(), logContentProvider, null);
		viewer.setContentProvider(logContentProvider);
		viewer.setUseHashlookup(true);
		viewer.setInput(new ArrayList<String>());
	}

	@Override
	public void setFocus() {
		viewer.getTable().setFocus();
	}

	@Override
	public void dispose() {
		if (serviceRegistration != null)
			serviceRegistration.unregister();
	}

	private BundleContext getBundleContext() {
		return SecurityUiPlugin.getDefault().getBundle().getBundleContext();
	}
}
