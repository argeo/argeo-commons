package org.argeo.security.ui.views;

import java.util.ArrayList;

import org.argeo.ArgeoLogListener;
import org.argeo.ArgeoLogger;
import org.argeo.security.ui.SecurityUiPlugin;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.part.ViewPart;

/**
 * Display log lines with a virtual table. Register and unregisters a
 * {@link ArgeoLogListener} via OSGi services.
 */
public class LogView extends ViewPart {
	public static String ID = SecurityUiPlugin.PLUGIN_ID + ".logView";

	private TableViewer viewer;

	private LogContentProvider logContentProvider;
	private ArgeoLogger argeoLogger;

	@Override
	public void createPartControl(Composite parent) {
		Font font = JFaceResources.getTextFont();
		Table table = new Table(parent, SWT.VIRTUAL | SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		table.setFont(font);

		viewer = new TableViewer(table);
		viewer.setLabelProvider(new LabelProvider());
		logContentProvider = new LogContentProvider(viewer);
		viewer.setContentProvider(logContentProvider);
		// viewer.setUseHashlookup(true);
		viewer.setInput(new ArrayList<String>());

		if (argeoLogger != null)
			argeoLogger.register(logContentProvider, 1000);
	}

	@Override
	public void setFocus() {
		viewer.getTable().setFocus();
	}

	@Override
	public void dispose() {
		if (argeoLogger != null)
			argeoLogger.unregister(logContentProvider);
	}

	public void setArgeoLogger(ArgeoLogger argeoLogger) {
		this.argeoLogger = argeoLogger;
	}

}
