package org.argeo.jcr.ui.explorer.commands;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Property;
import javax.jcr.nodetype.NodeType;

import org.argeo.eclipse.ui.ErrorFeedback;
import org.argeo.eclipse.ui.jcr.editors.NodeEditorInput;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

/** Generic command to open a path in an editor. */
public class EditNode extends AbstractHandler {
	public final static String EDITOR_PARAM = "editor";

	private String defaultEditorId;

	private Map<String, String> nodeTypeToEditor = new HashMap<String, String>();

	public Object execute(ExecutionEvent event) throws ExecutionException {
		String path = event.getParameter(Property.JCR_PATH);

		String type = event.getParameter(NodeType.NT_NODE_TYPE);
		if (type == null)
			type = NodeType.NT_UNSTRUCTURED;

		String editorId = event.getParameter(NodeType.NT_NODE_TYPE);
		if (editorId == null)
			editorId = nodeTypeToEditor.containsKey(type) ? nodeTypeToEditor
					.get(type) : defaultEditorId;
					
		NodeEditorInput nei = new NodeEditorInput(path);

		try {
			HandlerUtil.getActiveWorkbenchWindow(event).getActivePage()
					.openEditor(nei, editorId);
		} catch (PartInitException e) {
			ErrorFeedback.show("Cannot open " + editorId + " with " + path
					+ " of type " + type, e);
		}
		// TODO Auto-generated method stub
		return null;
	}

	public void setDefaultEditorId(String defaultEditorId) {
		this.defaultEditorId = defaultEditorId;
	}

}
