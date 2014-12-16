package org.argeo.cms.users;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;
import org.argeo.cms.CmsUtils;
import org.argeo.cms.viewers.EditablePart;
import org.argeo.cms.viewers.NodePart;
import org.argeo.cms.widgets.StyledControl;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.jcr.ArgeoNames;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/** Display a single user main info once it has been created. */
public class UserPart extends StyledControl implements EditablePart, NodePart,
		FocusListener {
	private static final long serialVersionUID = -2883661960366940505L;
	// private final static Log log = LogFactory.getLog(UserPart.class);

	// A static list of supported properties.
	private List<Text> texts;
	private final static String KEY_PROP_NAME = "jcr:propertyName";

	// TODO implement to provide user creation ability for anonymous user?
	// public UserPart(Composite parent, int swtStyle) {
	// super(parent, swtStyle);
	// }

	public UserPart(Composite parent, int style, Item item)
			throws RepositoryException {
		this(parent, style, item, true);
	}

	public UserPart(Composite parent, int style, Item item,
			boolean cacheImmediately) throws RepositoryException {
		super(parent, style, item, cacheImmediately);
	}

	@Override
	public Item getItem() throws RepositoryException {
		return getNode();
	}

	// Experimental, remove
	public void setMouseListener(MouseListener mouseListener) {
		super.setMouseListener(mouseListener);

		for (Text txt : texts)
			txt.addMouseListener(mouseListener);

	}

	@Override
	protected Control createControl(Composite box, String style) {
		if (isEditing())
			return createEditLayout(box, style);
		else
			return createROLayout(box, style);
	}

	protected Composite createROLayout(Composite parent, String style) {
		Composite body = new Composite(parent, SWT.NO_FOCUS);
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridLayout layout = new GridLayout(2, false);
		body.setLayout(layout);

		createTexts(body, UserStyles.USER_FORM_TEXT);
		CmsUtils.style(body, UserStyles.USER_FORM_TEXT);
		return body;
	}

	private void createTexts(Composite parent, String style) {
		texts = new ArrayList<Text>();
		texts.add(createLT(parent, style, "Displayed Name", Property.JCR_TITLE));
		texts.add(createLT(parent, style, "First name",
				ArgeoNames.ARGEO_FIRST_NAME));
		texts.add(createLT(parent, style, "Last name",
				ArgeoNames.ARGEO_LAST_NAME));
		texts.add(createLT(parent, style, "Email",
				ArgeoNames.ARGEO_PRIMARY_EMAIL));
		texts.add(createLMT(parent, style, "Description",
				Property.JCR_DESCRIPTION));
	}

	protected Composite createEditLayout(Composite parent, String style) {
		Composite body = new Composite(parent, SWT.NO_FOCUS);
		GridLayout layout = new GridLayout(2, false);
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		body.setLayout(layout);

		createTexts(body, UserStyles.USER_FORM_TEXT);

		for (Text txt : texts)
			txt.addFocusListener(this);
		CmsUtils.style(body, UserStyles.USER_FORM_TEXT);
		return body;
	}

	void refresh() {
		for (Text txt : texts) {
			txt.setText(get(getNode(), (String) txt.getData(KEY_PROP_NAME)));
			txt.setEditable(isEditing());
		}
	}

	// THE LISTENER
	@Override
	public void focusGained(FocusEvent e) {
		// Do nothing
	}

	@Override
	public void focusLost(FocusEvent e) {
		// Save change if needed
		Text text = (Text) e.getSource();
		set(getNode(), (String) text.getData(KEY_PROP_NAME), text.getText());
	}

	// HELPERS
	/** Creates label and text. */
	protected Text createLT(Composite body, String style, String label,
			String propName) {
		Label lbl = new Label(body, SWT.NONE);
		lbl.setText(label);
		lbl.setFont(EclipseUiUtils.getBoldFont(body));
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		Text text = new Text(body, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		CmsUtils.style(text, style);
		text.setData(KEY_PROP_NAME, propName);
		return text;
	}

	/** Creates label and multiline text. */
	protected Text createLMT(Composite body, String style, String label,
			String propName) {
		Label lbl = new Label(body, SWT.NONE);
		lbl.setText(label);
		lbl.setFont(EclipseUiUtils.getBoldFont(body));
		GridData gd = new GridData(SWT.RIGHT, SWT.TOP, false, false);
		gd.verticalIndent = 0;
		lbl.setLayoutData(gd);
		Text text = new Text(body, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		gd = new GridData(SWT.FILL, SWT.CENTER, true, true);
		gd.heightHint = 100;
		text.setLayoutData(gd);
		CmsUtils.style(text, style);
		text.setData(KEY_PROP_NAME, propName);
		return text;
	}

	/**
	 * Concisely get the string value of a property. Returns an empty String
	 * rather than null if this node doesn't have this property or if the
	 * corresponding property is an empty string.
	 */
	private String get(Node node, String propertyName) {
		try {
			if (!node.hasProperty(propertyName))
				return "";
			else
				return node.getProperty(propertyName).getString();
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot get property " + propertyName
					+ " of " + node, e);
		}
	}

	private boolean set(Node node, String propName, String value) {
		try {
			if ("".equals(value)
					&& (!node.hasProperty(propName) || node
							.hasProperty(propName)
							&& "".equals(node.getProperty(propName).getString())))
				return false;
			else if (node.hasProperty(propName)
					&& node.getProperty(propName).getString()
							.equals((String) value))
				return false;
			else {
				node.setProperty(propName, (String) value);
				node.getSession().save();
				return true;
			}
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot  property " + propName + " on "
					+ node + " with value " + value, e);
		}
	}
}