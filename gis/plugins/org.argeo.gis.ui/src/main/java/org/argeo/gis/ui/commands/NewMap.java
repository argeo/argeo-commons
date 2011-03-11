package org.argeo.gis.ui.commands;

import javax.jcr.Session;

import org.argeo.eclipse.ui.dialogs.Error;
import org.argeo.gis.ui.editors.MapEditorInput;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

/** Opens a new map editor */
public class NewMap extends AbstractHandler {
	private Session session;
	private String editorId;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			HandlerUtil
					.getActiveWorkbenchWindow(event)
					.getActivePage()
					.openEditor(new MapEditorInput(session.getRootNode()),
							editorId);
		} catch (Exception e) {
			Error.show("Cannot open editor", e);
		}
		return null;
	}

	public void setEditorId(String editorId) {
		this.editorId = editorId;
	}

	public void setSession(Session session) {
		this.session = session;
	}

}
