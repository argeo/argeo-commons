package org.argeo.cms.users;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;
import org.argeo.cms.util.CmsUtils;
import org.argeo.cms.viewers.EditablePart;
import org.argeo.cms.viewers.NodePart;
import org.argeo.cms.widgets.StyledControl;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.jcr.ArgeoNames;
import org.argeo.security.UserAdminService;
import org.argeo.security.jcr.JcrUserDetails;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

/** Display a single user main info once it has been created. */
public class UserPart extends StyledControl implements EditablePart, NodePart,
		FocusListener {
	private static final long serialVersionUID = -2883661960366940505L;
	// private final static Log log = LogFactory.getLog(UserPart.class);

	// A static list of supported properties.
	private List<Text> texts;
	private final static String KEY_PROP_NAME = "jcr:propertyName";

	// the 2 password fields
	private Text pwd1, pwd2;

	private UserAdminService userAdminService;

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

	@Override
	protected Control createControl(Composite box, String style) {
		Composite body = new Composite(box, SWT.NO_FOCUS);
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		CmsUtils.style(body, UserStyles.USER_FORM_TEXT);

		body.setLayout(new GridLayout(2, false));

		// Header
		Label headerLbl = new Label(body, SWT.NONE);
		headerLbl.setText(" Main user information");
		headerLbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,
				2, 1));
		CmsUtils.style(headerLbl, UserStyles.USER_FORM_TITLE);

		// Form field
		createTexts(body, UserStyles.USER_FORM_TEXT);

		if (isEditing())
			for (Text txt : texts)
				txt.addFocusListener(this);

		// Change password link
		headerLbl = new Label(body, SWT.NONE);
		headerLbl.setText(" Reset password");
		headerLbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,
				2, 1));
		CmsUtils.style(headerLbl, UserStyles.USER_FORM_TITLE);

		pwd1 = createLP(body, UserStyles.USER_FORM_TEXT, "Enter password");
		pwd2 = createLP(body, UserStyles.USER_FORM_TEXT, "Re-Enter");

		final Link link = new Link(body, SWT.NONE);
		link.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 2,
				1));
		link.setText("<a>Change password</a>");
		link.addSelectionListener(new SelectionAdapter() {
			private static final long serialVersionUID = 8348668888548451776L;

			@Override
			public void widgetSelected(SelectionEvent e) {
				String msg = null;
				if ("".equals(pwd1.getText().trim()))
					msg = "Passwords cannot be blank";
				else if (!pwd1.getText().equals(pwd2.getText()))
					msg = "Passwords do not match, please try again.";

				if (msg != null) {
					MessageDialog.openError(link.getShell(), "Error", msg);
				} else {
					try {
						String username = getNode().getProperty(
								ArgeoNames.ARGEO_USER_ID).getString();
						if (userAdminService.userExists(username)) {
							JcrUserDetails userDetails = (JcrUserDetails) userAdminService
									.loadUserByUsername(username);
							userDetails = userDetails.cloneWithNewPassword(pwd1
									.getText());
							userAdminService.updateUser(userDetails);
							MessageDialog.openInformation(link.getShell(),
									"Password changed", "Password changed.");
						}
					} catch (Exception re) {
						throw new ArgeoException(
								"unable to reset password for user "
										+ getNode(), re);
					}
				}

				pwd1.setText("");
				pwd2.setText("");

			}
		});
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

	void refresh() {
		for (Text txt : texts) {
			txt.setText(get(getNode(), (String) txt.getData(KEY_PROP_NAME)));
			txt.setEditable(isEditing());
		}
	}

	// his.listener methods
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

	// HELPERS
	/** Creates label and password text. */
	protected Text createLP(Composite body, String style, String label) {
		Label lbl = new Label(body, SWT.NONE);
		lbl.setText(label);
		lbl.setFont(EclipseUiUtils.getBoldFont(body));
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		Text text = new Text(body, SWT.BORDER | SWT.PASSWORD);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		CmsUtils.style(text, style);
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

	public void setUserAdminService(UserAdminService userAdminService) {
		this.userAdminService = userAdminService;
	}
}