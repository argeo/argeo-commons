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
package org.argeo.cms.widgets.auth;

import javax.security.auth.callback.CallbackHandler;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/** Default authentication dialog, to be used as {@link CallbackHandler}. */
public class DefaultLoginDialog extends AbstractLoginDialog {
	private static final long serialVersionUID = -8551827590693035734L;

	public DefaultLoginDialog() {
		this(Display.getCurrent().getActiveShell());
	}

	public DefaultLoginDialog(Shell parentShell) {
		super(parentShell);
	}

	protected Point getInitialSize() {
		return new Point(350, 180);
	}

	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		parent.pack();

		// Move the dialog to the center of the top level shell.
		Rectangle shellBounds;
		if (Display.getCurrent().getActiveShell() != null) // RCP
			shellBounds = Display.getCurrent().getActiveShell().getBounds();
		else
			shellBounds = Display.getCurrent().getBounds();// RAP
		Point dialogSize = parent.getSize();
		int x = shellBounds.x + (shellBounds.width - dialogSize.x) / 2;
		int y = shellBounds.y + (shellBounds.height - dialogSize.y) / 2;
		parent.setLocation(x, y);
		return control;
	}

	protected Control createDialogArea(Composite parent) {
		Composite dialogarea = (Composite) super.createDialogArea(parent);
		CompositeCallbackHandler composite = new CompositeCallbackHandler(
				dialogarea, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		composite.createCallbackHandlers(getCallbacks());
		return composite;
	}

	public void internalHandle() {
	}
}
