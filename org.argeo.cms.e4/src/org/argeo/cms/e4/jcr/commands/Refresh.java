/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.cms.e4.jcr.commands;

import java.util.List;

import javax.inject.Named;

import org.argeo.cms.e4.jcr.JcrBrowserView;
import org.argeo.cms.ui.jcr.JcrBrowserUtils;
import org.argeo.eclipse.ui.TreeParent;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;

/**
 * Force the selected objects of the active view to be refreshed doing the
 * following:
 * <ol>
 * <li>The model objects are recomputed</li>
 * <li>the view is refreshed</li>
 * </ol>
 */
public class Refresh {

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part, EPartService partService,
			ESelectionService selectionService) {

		JcrBrowserView view = (JcrBrowserView) part.getObject();
		List<?> selection = (List<?>) selectionService.getSelection();

		if (selection != null && !selection.isEmpty()) {
			for (Object obj : selection)
				if (obj instanceof TreeParent) {
					TreeParent tp = (TreeParent) obj;
					JcrBrowserUtils.forceRefreshIfNeeded(tp);
					view.refresh(obj);
				}
		} else if (view instanceof JcrBrowserView)
			view.refresh(null); // force full refresh
	}
}
