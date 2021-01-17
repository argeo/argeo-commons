package org.argeo.cms.ui.widgets;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

import org.argeo.cms.ui.util.CmsUiUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/** Editable text part displaying styled text. */
public class EditableText extends StyledControl {
	private static final long serialVersionUID = -6372283442330912755L;

	private boolean editable = true;

	public EditableText(Composite parent, int style) {
		super(parent, style);
		editable = !(SWT.READ_ONLY == (style & SWT.READ_ONLY));
	}

	public EditableText(Composite parent, int style, Item item) throws RepositoryException {
		this(parent, style, item, false);
	}

	public EditableText(Composite parent, int style, Item item, boolean cacheImmediately) throws RepositoryException {
		super(parent, style, item, cacheImmediately);
		editable = !(SWT.READ_ONLY == (style & SWT.READ_ONLY));
	}

	@Override
	protected Control createControl(Composite box, String style) {
		if (isEditing() && getEditable()) {
			return createText(box, style, true);
		} else {
//			return createText(box, style, false);
			return createLabel(box, style);
		}
	}

	protected Label createLabel(Composite box, String style) {
		Label lbl = new Label(box, getStyle() | SWT.WRAP);
		lbl.setLayoutData(CmsUiUtils.fillWidth());
		CmsUiUtils.style(lbl, style);
		CmsUiUtils.markup(lbl);
		if (mouseListener != null)
			lbl.addMouseListener(mouseListener);
		return lbl;
	}

	protected Text createText(Composite box, String style, boolean editable) {
		final Text text = new Text(box, getStyle() | SWT.MULTI | SWT.WRAP);
		text.setEditable(editable);
		GridData textLayoutData = CmsUiUtils.fillWidth();
		// textLayoutData.heightHint = preferredHeight;
		text.setLayoutData(textLayoutData);
		CmsUiUtils.style(text, style);
		text.setFocus();
		return text;
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

	public boolean getEditable() {
		return editable;
	}

}
