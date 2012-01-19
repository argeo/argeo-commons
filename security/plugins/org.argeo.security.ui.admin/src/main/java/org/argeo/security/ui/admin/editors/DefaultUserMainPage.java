package org.argeo.security.ui.admin.editors;

import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoNames;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Display/edit the properties common to all Argeo users
 */
public class DefaultUserMainPage extends FormPage implements ArgeoNames {
	final static String ID = "argeoUserEditor.mainPage";

	private final static Log log = LogFactory.getLog(DefaultUserMainPage.class);
	private Node userProfile;

	private char[] newPassword;

	public DefaultUserMainPage(FormEditor editor, Node userProfile) {
		super(editor, ID, "Main");
		this.userProfile = userProfile;
	}

	protected void createFormContent(final IManagedForm mf) {
		try {
			ScrolledForm form = mf.getForm();
			refreshFormTitle(form);
			GridLayout mainLayout = new GridLayout(1, true);
			form.getBody().setLayout(mainLayout);

			createGeneralPart(form.getBody());
			createPassworPart(form.getBody());
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot create form content", e);
		}
	}

	/** Creates the general section */
	protected void createGeneralPart(Composite parent)
			throws RepositoryException {
		FormToolkit tk = getManagedForm().getToolkit();
		Section section = tk.createSection(parent, Section.TITLE_BAR);
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		section.setText("General");
		Composite body = tk.createComposite(section, SWT.WRAP);
		section.setClient(body);
		GridLayout layout = new GridLayout(2, false);
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		body.setLayout(layout);

		final Text commonName = createLT(body, "Displayed Name",
				getProperty(Property.JCR_TITLE));
		final Text firstName = createLT(body, "First name",
				getProperty(ARGEO_FIRST_NAME));
		final Text lastName = createLT(body, "Last name",
				getProperty(ARGEO_LAST_NAME));
		final Text email = createLT(body, "Email",
				getProperty(ARGEO_PRIMARY_EMAIL));
		final Text description = createLMT(body, "Description",
				getProperty(Property.JCR_DESCRIPTION));

		// create form part (controller)
		AbstractFormPart part = new SectionPart(section) {
			public void commit(boolean onSave) {
				try {
					userProfile.getSession().getWorkspace().getVersionManager()
							.checkout(userProfile.getPath());
					userProfile.setProperty(Property.JCR_TITLE,
							commonName.getText());
					userProfile.setProperty(ARGEO_FIRST_NAME,
							firstName.getText());
					userProfile
							.setProperty(ARGEO_LAST_NAME, lastName.getText());
					userProfile.setProperty(ARGEO_PRIMARY_EMAIL,
							email.getText());
					userProfile.setProperty(Property.JCR_DESCRIPTION,
							description.getText());
					userProfile.getSession().save();
					userProfile.getSession().getWorkspace().getVersionManager()
							.checkin(userProfile.getPath());
					super.commit(onSave);
					refreshFormTitle(getManagedForm().getForm());
					if (log.isTraceEnabled())
						log.trace("General part committed");
				} catch (RepositoryException e) {
					throw new ArgeoException("Cannot commit", e);
				}
			}
		};
		// if (username != null)
		// username.addModifyListener(new FormPartML(part));
		firstName.addModifyListener(new FormPartML(part));
		lastName.addModifyListener(new FormPartML(part));
		email.addModifyListener(new FormPartML(part));
		description.addModifyListener(new FormPartML(part));
		getManagedForm().addPart(part);
	}

	private void refreshFormTitle(ScrolledForm form) throws RepositoryException {
		form.setText(getProperty(Property.JCR_TITLE)
				+ (userProfile.getProperty(ARGEO_ENABLED).getBoolean() ? ""
						: " [DISABLED]"));
	}

	/** @return the property, or the empty string if not set */
	protected String getProperty(String name) throws RepositoryException {
		return userProfile.hasProperty(name) ? userProfile.getProperty(name)
				.getString() : "";
	}

	/** Creates the password section */
	protected void createPassworPart(Composite parent) {
		FormToolkit tk = getManagedForm().getToolkit();
		Section section = tk.createSection(parent, Section.TITLE_BAR);
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		section.setText("Password");

		Composite body = tk.createComposite(section, SWT.WRAP);
		section.setClient(body);
		GridLayout layout = new GridLayout(2, false);
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		body.setLayout(layout);

		// add widgets (view)
		final Text password1 = createLP(body, "New password", "");
		final Text password2 = createLP(body, "Repeat password", "");
		// create form part (controller)
		AbstractFormPart part = new SectionPart(section) {

			public void commit(boolean onSave) {
				if (!password1.getText().equals("")
						|| !password2.getText().equals("")) {
					if (password1.getText().equals(password2.getText())) {
						newPassword = password1.getText().toCharArray();
						password1.setText("");
						password2.setText("");
						super.commit(onSave);
					} else {
						password1.setText("");
						password2.setText("");
						throw new ArgeoException("Passwords are not equals");
					}
				}
			}

		};
		password1.addModifyListener(new FormPartML(part));
		password2.addModifyListener(new FormPartML(part));
		getManagedForm().addPart(part);
	}

	/** Creates label and text. */
	protected Text createLT(Composite body, String label, String value) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		Label lbl = toolkit.createLabel(body, label);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		Text text = toolkit.createText(body, value, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return text;
	}

	/** Creates label and multiline text. */
	protected Text createLMT(Composite body, String label, String value) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		Label lbl = toolkit.createLabel(body, label);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		Text text = toolkit.createText(body, value, SWT.BORDER | SWT.MULTI);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		return text;
	}

	/** Creates label and password. */
	protected Text createLP(Composite body, String label, String value) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		Label lbl = toolkit.createLabel(body, label);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		Text text = toolkit.createText(body, value, SWT.BORDER | SWT.PASSWORD);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return text;
	}

	private class FormPartML implements ModifyListener {
		private AbstractFormPart formPart;

		public FormPartML(AbstractFormPart generalPart) {
			this.formPart = generalPart;
		}

		public void modifyText(ModifyEvent e) {
			formPart.markDirty();
		}

	}

	public String getNewPassword() {
		if (newPassword != null)
			return new String(newPassword);
		else
			return null;
	}

	public void resetNewPassword() {
		if (newPassword != null)
			Arrays.fill(newPassword, 'x');
		newPassword = null;
	}
}
