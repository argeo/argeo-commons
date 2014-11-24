package org.argeo.cms;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.argeo.jcr.JcrUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class SimpleDynamicPages implements CmsUiProvider {

	@Override
	public Control createUi(Composite parent, Node context)
			throws RepositoryException {
		if (context == null)
			throw new CmsException("Context cannot be null");
		parent.setLayout(new GridLayout(2, false));

		// parent
		if (!context.getPath().equals("/")) {
			new CmsLink("..", context.getParent().getPath()).createUi(parent,
					context);
			new Label(parent, SWT.NONE).setText(context.getParent()
					.getPrimaryNodeType().getName());
		}

		// context
		Label contextL = new Label(parent, SWT.NONE);
		contextL.setData(RWT.MARKUP_ENABLED, true);
		contextL.setText("<b>" + context.getName() + "</b>");
		new Label(parent, SWT.NONE).setText(context.getPrimaryNodeType()
				.getName());

		// children
		// Label childrenL = new Label(parent, SWT.NONE);
		// childrenL.setData(RWT.MARKUP_ENABLED, true);
		// childrenL.setText("<i>Children:</i>");
		// childrenL.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, false,
		// false, 2, 1));

		for (NodeIterator nIt = context.getNodes(); nIt.hasNext();) {
			Node child = nIt.nextNode();
			new CmsLink(child.getName(), child.getPath()).createUi(parent,
					context);

			new Label(parent, SWT.NONE).setText(child.getPrimaryNodeType()
					.getName());
		}

		// properties
		// Label propsL = new Label(parent, SWT.NONE);
		// propsL.setData(RWT.MARKUP_ENABLED, true);
		// propsL.setText("<i>Properties:</i>");
		// propsL.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, false, false,
		// 2, 1));
		for (PropertyIterator pIt = context.getProperties(); pIt.hasNext();) {
			Property property = pIt.nextProperty();

			Label label = new Label(parent, SWT.NONE);
			label.setText(property.getName());
			label.setToolTipText(JcrUtils
					.getPropertyDefinitionAsString(property));

			new Label(parent, SWT.NONE).setText(getPropAsString(property));
		}

		return null;
	}

	private String getPropAsString(Property property)
			throws RepositoryException {
		String result = "";
		DateFormat timeFormatter = new SimpleDateFormat("");
		if (property.isMultiple()) {
			result = getMultiAsString(property, ", ");
		} else {
			Value value = property.getValue();
			if (value.getType() == PropertyType.BINARY)
				result = "<binary>";
			else if (value.getType() == PropertyType.DATE)
				result = timeFormatter.format(value.getDate().getTime());
			else
				result = value.getString();
		}
		return result;
	}

	private String getMultiAsString(Property property, String separator)
			throws RepositoryException {
		if (separator == null)
			separator = "; ";
		Value[] values = property.getValues();
		StringBuilder builder = new StringBuilder();
		for (Value val : values) {
			String currStr = val.getString();
			if (!"".equals(currStr.trim()))
				builder.append(currStr).append(separator);
		}
		if (builder.lastIndexOf(separator) >= 0)
			return builder.substring(0, builder.length() - separator.length());
		else
			return builder.toString();
	}
}