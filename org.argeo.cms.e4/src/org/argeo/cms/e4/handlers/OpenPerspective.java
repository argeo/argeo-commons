package org.argeo.cms.e4.handlers;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

public class OpenPerspective {
	@Inject
	MApplication application;
	@Inject
	EPartService partService;
	@Inject
	EModelService modelService;

	@Execute
	public void execute(@Named("perspectiveId") String perspectiveId) {
		List<MPerspective> perspectives = modelService.findElements(application, perspectiveId, MPerspective.class,
				null);
		if (perspectives.size() == 0)
			return;
		MPerspective perspective = perspectives.get(0);
		partService.switchPerspective(perspective);
	}
}
