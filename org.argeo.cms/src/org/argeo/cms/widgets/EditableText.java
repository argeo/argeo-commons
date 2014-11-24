package org.argeo.cms.widgets;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

import org.argeo.cms.CmsUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/** Editable text part displaying styled text. */
public class EditableText extends StyledControl {
	private static final long serialVersionUID = -6372283442330912755L;

	public EditableText(Composite parent, int swtStyle) {
		super(parent, swtStyle);
	}

	public EditableText(Composite parent, int style, Item item)
			throws RepositoryException {
		this(parent, style, item, false);
	}

	public EditableText(Composite parent, int style, Item item,
			boolean cacheImmediately) throws RepositoryException {
		super(parent, style, item, cacheImmediately);
	}

	@Override
	protected Control createControl(Composite box, String style) {
		if (isEditing())
			return createText(box, style);
		else
			return createLabel(box, style);
	}

	protected Label createLabel(Composite box, String style) {
		Label lbl = new Label(box, getStyle() | SWT.WRAP);
		lbl.setLayoutData(CmsUtils.fillWidth());
		CmsUtils.style(lbl, style);
		CmsUtils.markup(lbl);
		if (mouseListener != null)
			lbl.addMouseListener(mouseListener);
		return lbl;
	}

	protected Text createText(Composite box, String style) {
		final Text text = new Text(box, getStyle() | SWT.MULTI | SWT.WRAP);
		GridData textLayoutData = CmsUtils.fillWidth();
		// textLayoutData.heightHint = preferredHeight;
		text.setLayoutData(textLayoutData);
		CmsUtils.style(text, style);
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

}
