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
package org.argeo.security.ui.rcp;

import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

/**
 * Workbench configuration which is aware of the logged in user and can remember
 * workbench state.
 */
public class SecureWorkbenchAdvisor extends WorkbenchAdvisor {
	public final static String INITIAL_PERSPECTIVE_PROPERTY = "org.argeo.security.ui.initialPerspective";
	private String initialPerspective = System.getProperty(
			INITIAL_PERSPECTIVE_PROPERTY, null);

	private final String username;

	public SecureWorkbenchAdvisor(String username) {
		this.username = username;
	}

	@Override
	public void initialize(final IWorkbenchConfigurer configurer) {
		super.initialize(configurer);
		configurer.setSaveAndRestore(true);
	}

	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
			IWorkbenchWindowConfigurer configurer) {
		return new SecureWorkbenchWindowAdvisor(configurer, username);
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

	protected String getUsername() {
		return username;
	}
}
