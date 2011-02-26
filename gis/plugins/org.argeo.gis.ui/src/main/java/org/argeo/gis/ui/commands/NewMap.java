package org.argeo.gis.ui.commands;

import org.argeo.eclipse.ui.dialogs.Error;
import org.argeo.gis.ui.editors.DefaultMapEditor;
import org.argeo.gis.ui.editors.MapEditorInput;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.geotools.map.DefaultMapContext;

/** Opens a new map editor */
public class NewMap extends AbstractHandler {
	private String editorId = DefaultMapEditor.ID;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			HandlerUtil
					.getActiveWorkbenchWindow(event)
					.getActivePage()
					.openEditor(new MapEditorInput(new DefaultMapContext()),
							editorId);
		} catch (Exception e) {
			Error.show("Cannot open editor", e);
		}
		return null;
	}

	public void setEditorId(String editorId) {
		this.editorId = editorId;
	}

}
