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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/** Utilities to simplify UI development. */
public class EclipseUiUtils {

	/** Dispose all children of a Composite */
	public static void clear(Composite composite) {
		for (Control child : composite.getChildren())
			child.dispose();
	}

	/**
	 * Enables efficient call to the layout method of a composite, refreshing only
	 * some of the children controls.
	 */
	public static void layout(Composite parent, Control... toUpdateControls) {
		parent.layout(toUpdateControls);
	}

	//
	// FONTS
	//
	/** Shortcut to retrieve default italic font from display */
	public static Font getItalicFont(Composite parent) {
		return JFaceResources.getFontRegistry().defaultFontDescriptor().setStyle(SWT.ITALIC)
				.createFont(parent.getDisplay());
	}

	/** Shortcut to retrieve default bold font from display */
	public static Font getBoldFont(Composite parent) {
		return JFaceResources.getFontRegistry().defaultFontDescriptor().setStyle(SWT.BOLD)
				.createFont(parent.getDisplay());
	}

	/** Shortcut to retrieve default bold italic font from display */
	public static Font getBoldItalicFont(Composite parent) {
		return JFaceResources.getFontRegistry().defaultFontDescriptor().setStyle(SWT.BOLD | SWT.ITALIC)
				.createFont(parent.getDisplay());
	}

	//
	// Simplify grid layouts management
	//
	public static GridLayout noSpaceGridLayout() {
		return noSpaceGridLayout(new GridLayout());
	}

	public static GridLayout noSpaceGridLayout(int columns) {
		return noSpaceGridLayout(new GridLayout(columns, false));
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

	public static GridData grabWidth(int horizontalAlignment, int verticalAlignment) {
		return new GridData(horizontalAlignment, horizontalAlignment, true, false);
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
	 * Create a label and a text field for a grid layout, the text field grabbing
	 * excess horizontal
	 * 
	 * @param parent
	 *            the parent composite
	 * @param label
	 *            the label to display
	 * @param modifyListener
	 *            a {@link ModifyListener} to listen on events on the text, can be
	 *            null
	 * @return the created text
	 * 
	 */
	// FIXME why was this deprecated.
	// * @ deprecated use { @ link #createGridLT(Composite, String)} instead
	// @ Deprecated
	public static Text createGridLT(Composite parent, String label, ModifyListener modifyListener) {
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
	 * Create a label and a text field for a grid layout, the text field grabbing
	 * excess horizontal
	 */
	public static Text createGridLT(Composite parent, String label) {
		return createGridLT(parent, label, null);
	}

	/**
	 * Creates one label and a text field not editable with background colour of the
	 * parent (like a label but with selectable text)
	 */
	public static Text createGridLL(Composite parent, String label, String text) {
		Text txt = createGridLT(parent, label);
		txt.setText(text);
		txt.setEditable(false);
		txt.setBackground(parent.getBackground());
		return txt;
	}

	/**
	 * Create a label and a text field with password display for a grid layout, the
	 * text field grabbing excess horizontal
	 */
	public static Text createGridLP(Composite parent, String label) {
		return createGridLP(parent, label, null);
	}

	/**
	 * Create a label and a text field with password display for a grid layout, the
	 * text field grabbing excess horizontal. The given modify listener will be
	 * added to the newly created text field if not null.
	 */
	public static Text createGridLP(Composite parent, String label, ModifyListener modifyListener) {
		Label lbl = new Label(parent, SWT.LEAD);
		lbl.setText(label);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		Text txt = new Text(parent, SWT.LEAD | SWT.BORDER | SWT.PASSWORD);
		txt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (modifyListener != null)
			txt.addModifyListener(modifyListener);
		return txt;
	}

	// MISCELLANEOUS

	/** Simply checks if a string is not null nor empty */
	public static boolean notEmpty(String stringToTest) {
		return !(stringToTest == null || "".equals(stringToTest.trim()));
	}

	/** Simply checks if a string is null or empty */
	public static boolean isEmpty(String stringToTest) {
		return stringToTest == null || "".equals(stringToTest.trim());
	}
}