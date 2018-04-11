package org.argeo.cms.e4.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

public class CloseAllParts {

	@Execute
	void execute(EPartService partService) {
		for (MPart part : partService.getParts()) {
			if (part.isCloseable()) {
				if (part.isDirty()) {
					if (partService.savePart(part, true)) {
						partService.hidePart(part);
					}
				} else {
					partService.hidePart(part);
				}
			}
		}
	}

	@CanExecute
	boolean canExecute(EPartService partService) {
		boolean atLeastOnePart = false;
		for (MPart part : partService.getParts()) {
			if (part.isVisible()) {
				atLeastOnePart = true;
				break;
			}
		}
		return atLeastOnePart;
	}

}