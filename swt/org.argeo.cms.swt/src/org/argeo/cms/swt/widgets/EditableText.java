package org.argeo.cms.swt.widgets;

import org.argeo.cms.swt.CmsSwtUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/** Editable text part displaying styled text. */
public class EditableText extends StyledControl {
	private static final long serialVersionUID = -6372283442330912755L;

	private boolean editable = true;
	private boolean multiLine = true;

	private Color highlightColor;
	private Composite highlight;

	private boolean useTextAsLabel = false;

	/**
	 * Message to display if there is not value. Only used with SWT.FLAT (label
	 * displayed with a {@link Text})
	 * 
	 * @see Text#setMessage(String)
	 */
	private String message;

	public EditableText(Composite parent, int style) {
		super(parent, style);
		editable = !(SWT.READ_ONLY == (style & SWT.READ_ONLY));
		multiLine = !(SWT.SINGLE == (style & SWT.SINGLE));
		highlightColor = parent.getDisplay().getSystemColor(SWT.COLOR_GRAY);
		useTextAsLabel = SWT.FLAT == (style & SWT.FLAT);
	}

	@Override
	protected Control createControl(Composite box, String style) {
		if (isEditing() && getEditable()) {
			return createText(box, style, true);
		} else {
			if (useTextAsLabel) {
				return createTextLabel(box, style);
			} else {
				return createLabel(box, style);
			}
		}
	}

	protected Label createLabel(Composite box, String style) {
		Label lbl = new Label(box, getStyle() | SWT.WRAP);
		lbl.setLayoutData(CmsSwtUtils.fillWidth());
		if (style != null)
			CmsSwtUtils.style(lbl, style);
		CmsSwtUtils.markup(lbl);
		if (mouseListener != null)
			lbl.addMouseListener(mouseListener);
		return lbl;
	}

	protected Text createTextLabel(Composite box, String style) {
		Text lbl = new Text(box, getStyle() | (multiLine ? SWT.MULTI | SWT.WRAP : SWT.SINGLE));
		lbl.setEditable(false);
		if (message != null)
			lbl.setMessage(message);
		lbl.setLayoutData(multiLine ? CmsSwtUtils.fillAll() : CmsSwtUtils.fillWidth());
		if (style != null)
			CmsSwtUtils.style(lbl, style);
		CmsSwtUtils.markup(lbl);
		if (mouseListener != null)
			lbl.addMouseListener(mouseListener);
		return lbl;
	}

	protected Text createText(Composite box, String style, boolean editable) {
		highlight = new Composite(box, SWT.NONE);
		highlight.setBackground(highlightColor);
		GridData highlightGd = new GridData(SWT.FILL, SWT.FILL, false, multiLine);
		highlightGd.widthHint = 5;
		if (!multiLine)
			highlightGd.heightHint = 3;
		highlight.setLayoutData(highlightGd);

		final Text text = new Text(box, getStyle() | (multiLine ? SWT.MULTI : SWT.SINGLE) | SWT.WRAP);
		text.setEditable(editable);
		GridData textLayoutData = multiLine ? CmsSwtUtils.fillAll() : CmsSwtUtils.fillWidth();
		// textLayoutData.heightHint = preferredHeight;
		text.setLayoutData(textLayoutData);
		if (style != null)
			CmsSwtUtils.style(text, style);
		text.setFocus();
		return text;
	}

	@Override
	protected void clear(boolean deep) {
		if (highlight != null)
			highlight.dispose();
		super.clear(deep);
	}

	public void setText(String text) {
		Control child = getControl();
		if (child instanceof Label)
			((Label) child).setText(text);
		else if (child instanceof Text)
			((Text) child).setText(text);
	}

	public Text getAsText() {
		return (Text) getControl();
	}

	public Label getAsLabel() {
		return (Label) getControl();
	}

	public String getText() {
		Control child = getControl();

		if (child instanceof Label)
			return ((Label) child).getText();
		else if (child instanceof Text)
			return ((Text) child).getText();
		else
			throw new IllegalStateException("Unsupported control " + child.getClass());
	}

	/** @deprecated Use {@link #isEditable()} instead. */
	@Deprecated
	public boolean getEditable() {
		return isEditable();
	}

	public boolean isEditable() {
		return editable;
	}

	public void setUseTextAsLabel(boolean useTextAsLabel) {
		this.useTextAsLabel = useTextAsLabel;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
		Control control = getControl();
		if (control != null && control instanceof Text txt)
			txt.setMessage(this.message);
	}

	@Override
	protected void setContainerLayoutData(Composite composite) {
		if (multiLine)
			composite.setLayoutData(CmsSwtUtils.fillAll());
		else
			super.setContainerLayoutData(composite);
	}

	@Override
	protected void setControlLayoutData(Control control) {
//		if (multiLine)
//			control.setLayoutData(CmsSwtUtils.fillAll());
//		else
		super.setControlLayoutData(control);
	}

}
