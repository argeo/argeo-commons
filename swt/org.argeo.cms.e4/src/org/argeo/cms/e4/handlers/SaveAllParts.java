package org.argeo.cms.e4.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

public class SaveAllParts {

	@Execute
	void execute(EPartService partService) {
		partService.saveAll(false);
	}

	@CanExecute
	boolean canExecute(EPartService partService) {
		return partService.getDirtyParts().size() > 0;
	}

}