package org.argeo.cms.e4;

import org.argeo.cms.CmsException;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

public class CmsE4Utils {
	public static void openEditor(EPartService partService, String editorId, String key, String state) {
		for (MPart part : partService.getParts()) {
			String id = part.getPersistedState().get(key);
			if (id != null && state.equals(id)) {
				partService.showPart(part, PartState.ACTIVATE);
				return;
			}
		}

		// new part
		MPart part = partService.createPart(editorId);
		if (part == null)
			throw new CmsException("No editor found with id " + editorId);
		part.getPersistedState().put(key, state);
		partService.showPart(part, PartState.ACTIVATE);
	}

	private CmsE4Utils() {
	}
}
