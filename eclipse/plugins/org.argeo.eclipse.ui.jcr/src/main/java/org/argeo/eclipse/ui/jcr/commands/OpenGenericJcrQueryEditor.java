package org.argeo.eclipse.ui.jcr.commands;

import org.argeo.eclipse.ui.jcr.editors.JcrQueryEditorInput;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;

/** Open a JCR query editor. */
public class OpenGenericJcrQueryEditor extends AbstractHandler {
	private String editorId;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			JcrQueryEditorInput editorInput = new JcrQueryEditorInput("", null);
			IWorkbenchPage activePage = HandlerUtil.getActiveWorkbenchWindow(
					event).getActivePage();
			activePage.openEditor(editorInput, editorId);
		} catch (Exception e) {
			throw new ExecutionException("Cannot open editor", e);
		}
		return null;
	}

	public void setEditorId(String editorId) {
		this.editorId = editorId;
	}

}
