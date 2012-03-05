package org.argeo.security.ui.views;

import java.util.ArrayList;

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
 * Display log lines for all users with a virtual table.
 */
public class AdminLogView extends ViewPart {
	public static String ID = SecurityUiPlugin.PLUGIN_ID + ".adminLogView";

	private TableViewer viewer;

	private LogContentProvider logContentProvider;
	private ArgeoLogger argeoLogger;

	@Override
	public void createPartControl(Composite parent) {
		// FIXME doesn't return a monospace font in RAP
		Font font = JFaceResources.getTextFont();
		Table table = new Table(parent, SWT.VIRTUAL | SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		table.setFont(font);

		viewer = new TableViewer(table);
		viewer.setLabelProvider(new LabelProvider());
		logContentProvider = new LogContentProvider(viewer) {

			@Override
			protected StringBuffer prefix(String username, Long timestamp,
					String level, String category, String thread) {
				return super
						.prefix(username, timestamp, level, category, thread)
						.append(norm(level, 5))
						.append(' ')
						.append(norm(username != null ? username
								: "<anonymous>", 16)).append(' ');
			}
		};
		viewer.setContentProvider(logContentProvider);
		// viewer.setUseHashlookup(true);
		viewer.setInput(new ArrayList<String>());

		if (argeoLogger != null)
			argeoLogger.registerForAll(logContentProvider, 1000, true);
	}

	@Override
	public void setFocus() {
		viewer.getTable().setFocus();
	}

	@Override
	public void dispose() {
		if (argeoLogger != null)
			argeoLogger.unregisterForAll(logContentProvider);
	}

	public void setArgeoLogger(ArgeoLogger argeoLogger) {
		this.argeoLogger = argeoLogger;
	}

}
