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

import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class SecureWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {
	private final String username;

	public SecureWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer,
			String username) {
		super(configurer);
		this.username = username;
	}

	public ActionBarAdvisor createActionBarAdvisor(
			IActionBarConfigurer configurer) {
		return new SecureActionBarAdvisor(configurer, true);
	}

	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(1200, 900));
		configurer.setShowCoolBar(true);
		configurer.setShowMenuBar(true);
		configurer.setShowStatusLine(true);
		configurer.setShowProgressIndicator(true);

		configurer.setShowPerspectiveBar(true);
		String remoteUri = System
				.getProperty(AbstractSecureApplication.NODE_REPO_URI);
		if (remoteUri != null)
			configurer
					.setTitle("Argeo UI - " + username + " (" + remoteUri + ")"); //$NON-NLS-1$
		else
			configurer.setTitle("Argeo UI - " + username); //$NON-NLS-1$

	}
}
