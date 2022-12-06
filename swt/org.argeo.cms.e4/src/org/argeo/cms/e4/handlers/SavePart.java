package org.argeo.cms.e4.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

public class SavePart {
	@Execute
	void execute(EPartService partService, MPart part) {
		partService.savePart(part, false);
	}

	@CanExecute
	boolean canExecute(MPart part) {
		return part.isDirty();
	}
}