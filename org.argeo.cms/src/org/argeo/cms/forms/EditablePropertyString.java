package org.argeo.cms.forms;

import static org.argeo.cms.forms.FormStyle.propertyMessage;
import static org.argeo.cms.forms.FormStyle.propertyText;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.cms.viewers.EditablePart;
import org.argeo.cms.widgets.EditableText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/** Editable String in a CMS context */
public class EditablePropertyString extends EditableText implements
		EditablePart {
	private static final long serialVersionUID = 5055000749992803591L;

	private String propertyName;
	private String message;

	public EditablePropertyString(Composite parent, int style, Node node,
			String propertyName, String message) throws RepositoryException {
		super(parent, style, node, true);

		this.propertyName = propertyName;
		this.message = message;

		if (node.hasProperty(propertyName)) {
			this.setStyle(propertyText.style());
			this.setText(node.getProperty(propertyName).getString());
		} else {
			this.setStyle(propertyMessage.style());
			this.setText(message + "  ");
		}
	}

	public void setText(String text) {
		Control child = getControl();
		if (child instanceof Label) {
			Label lbl = (Label) child;
			if (FormUtils.notEmpty(text))
				lbl.setText(message + "  ");
			else
				lbl.setText(text);
		} else if (child instanceof Text) {
			Text txt = (Text) child;
			if (FormUtils.notEmpty(text)) {
				txt.setText("");
				txt.setMessage(message + " ");
			} else
				txt.setText(text.replaceAll("<br/>", "\n"));
		}
	}

	public synchronized void startEditing() {
		getControl().setData(STYLE, propertyText.style());
		super.startEditing();
	}

	public synchronized void stopEditing() {
		if (FormUtils.notEmpty(((Text) getControl()).getText()))
			getControl().setData(STYLE, propertyMessage.style());
		else
			getControl().setData(STYLE, propertyText.style());
		super.stopEditing();
	}

	public String getPropertyName() {
		return propertyName;
	}
}