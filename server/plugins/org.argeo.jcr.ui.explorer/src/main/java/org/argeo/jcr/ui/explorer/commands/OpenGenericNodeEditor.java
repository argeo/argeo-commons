package org.argeo.jcr.ui.explorer.commands;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.jcr.editors.NodeEditorInput;
import org.argeo.jcr.ui.explorer.editors.GenericNodeEditor;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

public class OpenGenericNodeEditor extends AbstractHandler {
	private final static Log log = LogFactory
			.getLog(OpenGenericNodeEditor.class);
	public final static String ID = "org.argeo.jcr.ui.explorer.openGenericNodeEditor";
	public final static String PARAM_PATH = "org.argeo.jcr.ui.explorer.nodePath";

	public Object execute(ExecutionEvent event) throws ExecutionException {
		String path = event.getParameter(PARAM_PATH);
		try {
			NodeEditorInput nei = new NodeEditorInput(path);
			HandlerUtil.getActiveWorkbenchWindow(event).getActivePage()
					.openEditor(nei, GenericNodeEditor.ID);
		} catch (Exception e) {
			throw new ArgeoException("Cannot open editor", e);
		}
		return null;
	}

}
