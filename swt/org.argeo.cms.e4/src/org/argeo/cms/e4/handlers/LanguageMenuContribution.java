
package org.argeo.cms.e4.handlers;

import java.util.Date;
import java.util.List;

import org.eclipse.e4.ui.di.AboutToHide;
import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

public class LanguageMenuContribution {
	@AboutToShow
	public void aboutToShow(List<MMenuElement> items, EModelService modelService) {
		MDirectMenuItem dynamicItem = modelService.createModelElement(MDirectMenuItem.class);
		dynamicItem.setLabel("Dynamic Menu Item (" + new Date() + ")");
		//dynamicItem.setContributorURI("platform:/plugin/org.argeo.cms.e4");
		//dynamicItem.setContributionURI("bundleclass://org.argeo.cms.e4/" + ChangeLanguage.class.getName());
		dynamicItem.setEnabled(true);
		dynamicItem.setContributionURI("bundleclass://org.argeo.cms.e4/org.argeo.cms.e4.handlers.ChangeLanguage");
		items.add(dynamicItem);
	}

	@AboutToHide
	public void aboutToHide() {
		
	}
}