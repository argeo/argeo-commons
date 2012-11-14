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
package org.argeo.eclipse.ui;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/** Utilities to simplify UI development. */
public class EclipseUiUtils {
	/**
	 * Create a label and a text field for a grid layout, the text field grabing
	 * excess horizontal
	 * 
	 * @param parent
	 *            the parent composite
	 * @param label
	 *            the lable to display
	 * @param modifyListener
	 *            a {@link ModifyListener} to listen on events on the text, can
	 *            be null
	 * @return the created text
	 * 
	 * @deprecated use {@link #createGridLT(Composite, String)} instead
	 */
	@Deprecated
	public static Text createGridLT(Composite parent, String label,
			ModifyListener modifyListener) {
		Label lbl = new Label(parent, SWT.LEAD);
		lbl.setText(label);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		Text txt = new Text(parent, SWT.LEAD | SWT.BORDER);
		txt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (modifyListener != null)
			txt.addModifyListener(modifyListener);
		return txt;
	}

	public static Text createGridLT(Composite parent, String label) {
		return createGridLT(parent, label, null);
	}

	/**
	 * Creates one label and a text field not editable with background color of
	 * the parent (like a label but with selectable text)
	 */
	public static Text createGridLL(Composite parent, String label, String text) {
		Text txt = createGridLT(parent, label);
		txt.setText(text);
		txt.setEditable(false);
		txt.setBackground(parent.getBackground());
		return txt;
	}

	public static Text createGridLP(Composite parent, String label,
			ModifyListener modifyListener) {
		Label lbl = new Label(parent, SWT.LEAD);
		lbl.setText(label);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		Text txt = new Text(parent, SWT.LEAD | SWT.BORDER | SWT.PASSWORD);
		txt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (txt != null)
			txt.addModifyListener(modifyListener);
		return txt;
	}

	public static Font getItalicFont(Composite parent) {
		return JFaceResources.getFontRegistry().defaultFontDescriptor()
				.setStyle(SWT.ITALIC).createFont(parent.getDisplay());
	}

	public static Font getBoldFont(Composite parent) {
		return JFaceResources.getFontRegistry().defaultFontDescriptor()
				.setStyle(SWT.BOLD).createFont(parent.getDisplay());
	}

	public static Font getBoldItalicFont(Composite parent) {
		return JFaceResources.getFontRegistry().defaultFontDescriptor()
				.setStyle(SWT.BOLD | SWT.ITALIC)
				.createFont(parent.getDisplay());
	}

}
