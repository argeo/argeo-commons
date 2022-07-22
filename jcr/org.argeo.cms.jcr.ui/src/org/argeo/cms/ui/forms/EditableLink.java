package org.argeo.cms.ui.forms;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.cms.swt.SwtEditablePart;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/** Editable String that displays a browsable link when read-only */
public class EditableLink extends EditablePropertyString implements
		SwtEditablePart {
	private static final long serialVersionUID = 5055000749992803591L;

	private String type;
	private String message;
	private boolean readOnly;

	public EditableLink(Composite parent, int style, Node node,
			String propertyName, String type, String message)
			throws RepositoryException {
		super(parent, style, node, propertyName, message);
		this.message = message;
		this.type = type;

		readOnly = SWT.READ_ONLY == (style & SWT.READ_ONLY);
		if (node.hasProperty(propertyName)) {
			this.setStyle(FormStyle.propertyText.style());
			this.setText(node.getProperty(propertyName).getString());
		} else {
			this.setStyle(FormStyle.propertyMessage.style());
			this.setText("");
		}
	}

	public void setText(String text) {
		Control child = getControl();
		if (child instanceof Label) {
			Label lbl = (Label) child;
			if (EclipseUiUtils.isEmpty(text))
				lbl.setText(message);
			else if (readOnly)
				setLinkValue(lbl, text);
			else
				// if canEdit() we put only the value with no link
				// to avoid glitches of the edition life cycle
				lbl.setText(text);
		} else if (child instanceof Text) {
			Text txt = (Text) child;
			if (EclipseUiUtils.isEmpty(text)) {
				txt.setText("");
				txt.setMessage(message);
			} else
				txt.setText(text);
		}
	}

	private void setLinkValue(Label lbl, String text) {
		if (FormStyle.email.style().equals(type))
			lbl.setText(FormUtils.getMailLink(text));
		else if (FormStyle.phone.style().equals(type))
			lbl.setText(FormUtils.getPhoneLink(text));
		else if (FormStyle.website.style().equals(type))
			lbl.setText(FormUtils.getUrlLink(text));
		else if (FormStyle.facebook.style().equals(type)
				|| FormStyle.instagram.style().equals(type)
				|| FormStyle.linkedIn.style().equals(type)
				|| FormStyle.twitter.style().equals(type))
			lbl.setText(FormUtils.getUrlLink(text));
	}
}