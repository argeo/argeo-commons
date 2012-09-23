/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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
package org.argeo.security.ui.rap;

import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

/** Eclipse RAP specific workbench advisor */
public class RapWorkbenchAdvisor extends WorkbenchAdvisor {
	public final static String INITIAL_PERSPECTIVE_PROPERTY = "org.argeo.security.ui.initialPerspective";
	public final static String SAVE_AND_RESTORE_PROPERTY = "org.argeo.security.ui.saveAndRestore";

	private String initialPerspective = System.getProperty(
			INITIAL_PERSPECTIVE_PROPERTY, null);

	private String username;

	public RapWorkbenchAdvisor(String username) {
		this.username = username;
	}

	@Override
	public void initialize(IWorkbenchConfigurer configurer) {
		super.initialize(configurer);
		Boolean saveAndRestore = Boolean.parseBoolean(System.getProperty(
				SAVE_AND_RESTORE_PROPERTY, "false"));
		configurer.setSaveAndRestore(saveAndRestore);
	}

	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
			IWorkbenchWindowConfigurer configurer) {
		return new RapWindowAdvisor(configurer, username);
	}

	public String getInitialWindowPerspectiveId() {
		if (initialPerspective != null) {
			// check whether this user can see the declared perspective
			// (typically the perspective won't be listed if this user doesn't
			// have the right to see it)
			IPerspectiveDescriptor pd = getWorkbenchConfigurer().getWorkbench()
					.getPerspectiveRegistry()
					.findPerspectiveWithId(initialPerspective);
			if (pd == null)
				return null;
		}
		return initialPerspective;
	}
}
