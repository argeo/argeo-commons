package org.argeo.security.ui.commands;

import org.argeo.security.ui.editors.ArgeoUserEditor;
import org.argeo.security.ui.editors.ArgeoUserEditorInput;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;

/** Command handler to set visible or open a Argeo user. */
public class OpenArgeoUserEditor extends AbstractHandler {
	public final static String COMMAND_ID = "org.argeo.security.ui.openArgeoUserEditor";
	public final static String PARAM_USERNAME = "org.argeo.security.ui.username";

	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			ArgeoUserEditorInput editorInput = new ArgeoUserEditorInput(
					event.getParameter(PARAM_USERNAME));
			IWorkbenchPage activePage = HandlerUtil.getActiveWorkbenchWindow(
					event).getActivePage();
			activePage.openEditor(editorInput, ArgeoUserEditor.ID);
		} catch (Exception e) {
			throw new ExecutionException("Cannot open editor", e);
		}
		return null;
	}
}
