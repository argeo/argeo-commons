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
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/** Utilities to simplify UI development. */
public class EclipseUiUtils {

	//
	// Simplify grid layouts management
	//
	public static GridLayout noSpaceGridLayout() {
		return noSpaceGridLayout(new GridLayout());
	}

	public static GridLayout noSpaceGridLayout(GridLayout layout) {
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		return layout;
	}

	public static GridData fillWidth() {
		return grabWidth(SWT.FILL, SWT.FILL);
	}

	public static GridData fillWidth(int colSpan) {
		GridData gd = grabWidth(SWT.FILL, SWT.FILL);
		gd.horizontalSpan = colSpan;
		return gd;
	}

	public static GridData fillAll() {
		return new GridData(SWT.FILL, SWT.FILL, true, true);
	}

	public static GridData fillAll(int colSpan, int rowSpan) {
		return new GridData(SWT.FILL, SWT.FILL, true, true, colSpan, rowSpan);
	}

	public static GridData grabWidth(int horizontalAlignment,
			int verticalAlignment) {
		return new GridData(horizontalAlignment, horizontalAlignment, true,
				false);
	}

	//
	// Simplify Form layout management
	//

	/**
	 * Creates a basic form data that is attached to the 4 corners of the parent
	 * composite
	 */
	public static FormData fillFormData() {
		FormData formData = new FormData();
		formData.top = new FormAttachment(0, 0);
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.bottom = new FormAttachment(100, 0);
		return formData;
	}

	/**
	 * Create a label and a text field for a grid layout, the text field
	 * grabbing excess horizontal
	 * 
	 * @param parent
	 *            the parent composite
	 * @param label
	 *            the label to display
	 * @param modifyListener
	 *            a {@link ModifyListener} to listen on events on the text, can
	 *            be null
	 * @return the created text
	 * 
	 */
	// FIXME why was this deprecated.
	// * @ deprecated use { @ link #createGridLT(Composite, String)} instead
	// @ Deprecated
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

	/**
	 * Create a label and a text field for a grid layout, the text field
	 * grabbing excess horizontal
	 */
	public static Text createGridLT(Composite parent, String label) {
		return createGridLT(parent, label, null);
	}

	/**
	 * Creates one label and a text field not editable with background colour of
	 * the parent (like a label but with selectable text)
	 */
	public static Text createGridLL(Composite parent, String label, String text) {
		Text txt = createGridLT(parent, label);
		txt.setText(text);
		txt.setEditable(false);
		txt.setBackground(parent.getBackground());
		return txt;
	}

	/**
	 * Create a label and a text field with password display for a grid layout,
	 * the text field grabbing excess horizontal
	 */
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

	//
	// FONTS
	//

	/** Shortcut to retrieve default italic font from display */
	public static Font getItalicFont(Composite parent) {
		return JFaceResources.getFontRegistry().defaultFontDescriptor()
				.setStyle(SWT.ITALIC).createFont(parent.getDisplay());
	}

	/** Shortcut to retrieve default bold font from display */
	public static Font getBoldFont(Composite parent) {
		return JFaceResources.getFontRegistry().defaultFontDescriptor()
				.setStyle(SWT.BOLD).createFont(parent.getDisplay());
	}

	/** Shortcut to retrieve default bold italic font from display */
	public static Font getBoldItalicFont(Composite parent) {
		return JFaceResources.getFontRegistry().defaultFontDescriptor()
				.setStyle(SWT.BOLD | SWT.ITALIC)
				.createFont(parent.getDisplay());
	}
}