package org.argeo.jcr.ui.explorer.commands;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.jcr.editors.NodeEditorInput;
import org.argeo.jcr.ui.explorer.JcrExplorerConstants;
import org.argeo.jcr.ui.explorer.editors.GenericNodeEditor;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

/** Opens the generic node editor. */
public class OpenGenericNodeEditor extends AbstractHandler {
	public final static String ID = "org.argeo.jcr.ui.explorer.openGenericNodeEditor";

	public Object execute(ExecutionEvent event) throws ExecutionException {
		String path = event.getParameter(JcrExplorerConstants.PARAM_PATH);
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
