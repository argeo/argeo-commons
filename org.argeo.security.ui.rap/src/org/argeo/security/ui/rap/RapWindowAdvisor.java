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
package org.argeo.security.ui.rap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

/** Eclipse RAP specific window advisor */
public class RapWindowAdvisor extends WorkbenchWindowAdvisor {

	private String username;

	public RapWindowAdvisor(IWorkbenchWindowConfigurer configurer,
			String username) {
		super(configurer);
		this.username = username;
	}

	@Override
	public ActionBarAdvisor createActionBarAdvisor(
			IActionBarConfigurer configurer) {
		return new RapActionBarAdvisor(configurer, username);
	}

	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setShowCoolBar(true);
		configurer.setShowMenuBar(false);
		configurer.setShowStatusLine(false);
		configurer.setShowPerspectiveBar(true);
		configurer.setTitle("Argeo Web UI"); //$NON-NLS-1$
		// Full screen, see
		// http://wiki.eclipse.org/RAP/FAQ#How_to_create_a_fullscreen_application
		configurer.setShellStyle(SWT.NO_TRIM);
		Rectangle bounds = Display.getCurrent().getBounds();
		configurer.setInitialSize(new Point(bounds.width, bounds.height));

		// Handle window resize in Rap 2.1+ see
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=417254
		Display.getCurrent().addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event event) {
				Rectangle bounds = event.display.getBounds();
				IWorkbenchWindow iww = getWindowConfigurer().getWindow()
						.getWorkbench().getActiveWorkbenchWindow();
				iww.getShell().setBounds(bounds);
			}
		});
	}

	@Override
	public void postWindowCreate() {
		Shell shell = getWindowConfigurer().getWindow().getShell();
		shell.setMaximized(true);
	}

	@Override
	public void postWindowOpen() {
		String defaultPerspective = getWindowConfigurer()
				.getWorkbenchConfigurer().getWorkbench()
				.getPerspectiveRegistry().getDefaultPerspective();
		if (defaultPerspective == null) {
			IWorkbenchWindow window = getWindowConfigurer().getWindow();
			if (window == null)
				return;

			IWorkbenchAction openPerspectiveDialogAction = ActionFactory.OPEN_PERSPECTIVE_DIALOG
					.create(window);
			openPerspectiveDialogAction.run();
		}
	}

}
