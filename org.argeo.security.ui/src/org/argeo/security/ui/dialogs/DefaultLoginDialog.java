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
package org.argeo.security.ui.dialogs;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;

import org.argeo.security.core.BundleContextCallback;
import org.argeo.security.ui.SecurityUiPlugin;
import org.argeo.util.LocaleCallback;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** Default authentication dialog, to be used as {@link CallbackHandler}. */
public class DefaultLoginDialog extends AbstractLoginDialog {
	public DefaultLoginDialog() {
		this(SecurityUiPlugin.display.get().getActiveShell());
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
		Composite composite = new Composite(dialogarea, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createCallbackHandlers(composite);
		// parent.pack();
		return composite;
	}

	private void createCallbackHandlers(Composite composite) {
		Callback[] callbacks = getCallbacks();
		for (int i = 0; i < callbacks.length; i++) {
			Callback callback = callbacks[i];
			if (callback instanceof TextOutputCallback) {
				createLabelTextoutputHandler(composite,
						(TextOutputCallback) callback);
			} else if (callback instanceof NameCallback) {
				createNameHandler(composite, (NameCallback) callback);
			} else if (callback instanceof PasswordCallback) {
				createPasswordHandler(composite, (PasswordCallback) callback);
			} else if (callback instanceof LocaleCallback) {
				createLocaleHandler(composite, (LocaleCallback) callback);
			} else if (callback instanceof BundleContextCallback) {
				((BundleContextCallback) callback)
						.setBundleContext(SecurityUiPlugin.getBundleContext());
			}
		}
	}

	private void createPasswordHandler(Composite composite,
			final PasswordCallback callback) {
		Label label = new Label(composite, SWT.NONE);
		label.setText(callback.getPrompt());
		final Text passwordText = new Text(composite, SWT.SINGLE | SWT.LEAD
				| SWT.PASSWORD | SWT.BORDER);
		passwordText
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		passwordText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent event) {
				// FIXME use getTextChars() in Eclipse 3.7
				callback.setPassword(passwordText.getText().toCharArray());
			}
		});
	}

	private void createLocaleHandler(Composite composite,
			final LocaleCallback callback) {
		String[] labels = callback.getSupportedLocalesLabels();
		if (labels.length == 0)
			return;
		Label label = new Label(composite, SWT.NONE);
		label.setText(callback.getPrompt());

		final Combo combo = new Combo(composite, SWT.READ_ONLY);
		combo.setItems(labels);
		combo.select(callback.getDefaultIndex());
		combo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		combo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				callback.setSelectedIndex(combo.getSelectionIndex());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	}

	private void createNameHandler(Composite composite,
			final NameCallback callback) {
		Label label = new Label(composite, SWT.NONE);
		label.setText(callback.getPrompt());
		final Text text = new Text(composite, SWT.SINGLE | SWT.LEAD
				| SWT.BORDER);
		if (callback.getDefaultName() != null) {
			// set default value, if provided
			text.setText(callback.getDefaultName());
			callback.setName(callback.getDefaultName());
		}
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		text.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent event) {
				callback.setName(text.getText());
			}
		});
	}

	private void createLabelTextoutputHandler(Composite composite,
			final TextOutputCallback callback) {
		Label label = new Label(composite, SWT.NONE);
		label.setText(callback.getMessage());
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		// TODO: find a way to pass this information
		// int messageType = callback.getMessageType();
		// int dialogMessageType = IMessageProvider.NONE;
		// switch (messageType) {
		// case TextOutputCallback.INFORMATION:
		// dialogMessageType = IMessageProvider.INFORMATION;
		// break;
		// case TextOutputCallback.WARNING:
		// dialogMessageType = IMessageProvider.WARNING;
		// break;
		// case TextOutputCallback.ERROR:
		// dialogMessageType = IMessageProvider.ERROR;
		// break;
		// }
		// setMessage(callback.getMessage(), dialogMessageType);
	}

	public void internalHandle() {
	}
}
