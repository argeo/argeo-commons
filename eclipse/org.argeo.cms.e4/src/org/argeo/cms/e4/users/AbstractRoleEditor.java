package org.argeo.cms.e4.users;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.argeo.cms.auth.UserAdminUtils;
import org.argeo.cms.ui.eclipse.forms.AbstractFormPart;
import org.argeo.cms.ui.eclipse.forms.IManagedForm;
import org.argeo.cms.ui.eclipse.forms.ManagedForm;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.util.naming.LdapAttrs;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.service.useradmin.UserAdminEvent;

/** Editor for a user, might be a user or a group. */
public abstract class AbstractRoleEditor {

	// public final static String USER_EDITOR_ID = WorkbenchUiPlugin.PLUGIN_ID +
	// ".userEditor";
	// public final static String GROUP_EDITOR_ID = WorkbenchUiPlugin.PLUGIN_ID +
	// ".groupEditor";

	/* DEPENDENCY INJECTION */
	@Inject
	protected UserAdminWrapper userAdminWrapper;

	@Inject
	private MPart mPart;

	// @Inject
	// Composite parent;

	private UserAdmin userAdmin;

	// Context
	private User user;
	private String username;

	private NameChangeListener listener;

	private ManagedForm managedForm;

	// public void init(IEditorSite site, IEditorInput input) throws
	// PartInitException {
	@PostConstruct
	public void init(Composite parent) {
		this.userAdmin = userAdminWrapper.getUserAdmin();
		username = mPart.getPersistedState().get(LdapAttrs.uid.name());
		user = (User) userAdmin.getRole(username);

		listener = new NameChangeListener(Display.getCurrent());
		userAdminWrapper.addListener(listener);
		updateEditorTitle(null);

		managedForm = new ManagedForm(parent) {

			@Override
			public void staleStateChanged() {
				refresh();
			}
		};
		ScrolledComposite scrolled = managedForm.getForm();
		Composite body = new Composite(scrolled, SWT.NONE);
		scrolled.setContent(body);
		createUi(body);
		managedForm.refresh();
	}

	abstract void createUi(Composite parent);

	/**
	 * returns the list of all authorizations for the given user or of the current
	 * displayed user if parameter is null
	 */
	protected List<User> getFlatGroups(User aUser) {
		Authorization currAuth;
		if (aUser == null)
			currAuth = userAdmin.getAuthorization(this.user);
		else
			currAuth = userAdmin.getAuthorization(aUser);

		String[] roles = currAuth.getRoles();

		List<User> groups = new ArrayList<User>();
		for (String roleStr : roles) {
			User currRole = (User) userAdmin.getRole(roleStr);
			if (currRole != null && !groups.contains(currRole))
				groups.add(currRole);
		}
		return groups;
	}

	protected IManagedForm getManagedForm() {
		return managedForm;
	}

	/** Exposes the user (or group) that is displayed by the current editor */
	protected User getDisplayedUser() {
		return user;
	}

	private void setDisplayedUser(User user) {
		this.user = user;
	}

	void updateEditorTitle(String title) {
		if (title == null) {
			String commonName = UserAdminUtils.getProperty(user, LdapAttrs.cn.name());
			title = "".equals(commonName) ? user.getName() : commonName;
		}
		setPartName(title);
	}

	protected void setPartName(String name) {
		mPart.setLabel(name);
	}

	// protected void addPages() {
	// try {
	// if (user.getType() == Role.GROUP)
	// addPage(new GroupMainPage(this, userAdminWrapper, repository, nodeInstance));
	// else
	// addPage(new UserMainPage(this, userAdminWrapper));
	// } catch (Exception e) {
	// throw new CmsException("Cannot add pages", e);
	// }
	// }

	@Persist
	public void doSave(IProgressMonitor monitor) {
		userAdminWrapper.beginTransactionIfNeeded();
		commitPages(true);
		userAdminWrapper.commitOrNotifyTransactionStateChange();
		// firePropertyChange(PROP_DIRTY);
		userAdminWrapper.notifyListeners(new UserAdminEvent(null, UserAdminEvent.ROLE_REMOVED, user));
	}

	protected void commitPages(boolean b) {
		managedForm.commit(b);
	}

	@PreDestroy
	public void dispose() {
		userAdminWrapper.removeListener(listener);
		managedForm.dispose();
	}

	// CONTROLERS FOR THIS EDITOR AND ITS PAGES

	class NameChangeListener extends UiUserAdminListener {
		public NameChangeListener(Display display) {
			super(display);
		}

		@Override
		public void roleChangedToUiThread(UserAdminEvent event) {
			Role changedRole = event.getRole();
			if (changedRole == null || changedRole.equals(user)) {
				updateEditorTitle(null);
				User reloadedUser = (User) userAdminWrapper.getUserAdmin().getRole(user.getName());
				setDisplayedUser(reloadedUser);
			}
		}
	}

	class MainInfoListener extends UiUserAdminListener {
		private final AbstractFormPart part;

		public MainInfoListener(Display display, AbstractFormPart part) {
			super(display);
			this.part = part;
		}

		@Override
		public void roleChangedToUiThread(UserAdminEvent event) {
			// Rollback
			if (event.getRole() == null)
				part.markStale();
		}
	}

	class GroupChangeListener extends UiUserAdminListener {
		private final AbstractFormPart part;

		public GroupChangeListener(Display display, AbstractFormPart part) {
			super(display);
			this.part = part;
		}

		@Override
		public void roleChangedToUiThread(UserAdminEvent event) {
			// always mark as stale
			part.markStale();
		}
	}

	/** Registers a listener that will notify this part */
	class FormPartML implements ModifyListener {
		private static final long serialVersionUID = 6299808129505381333L;
		private AbstractFormPart formPart;

		public FormPartML(AbstractFormPart generalPart) {
			this.formPart = generalPart;
		}

		public void modifyText(ModifyEvent e) {
			// Discard event when the control does not have the focus, typically
			// to avoid all editors being marked as dirty during a Rollback
			if (((Control) e.widget).isFocusControl())
				formPart.markDirty();
		}
	}

	/* DEPENDENCY INJECTION */
	public void setUserAdminWrapper(UserAdminWrapper userAdminWrapper) {
		this.userAdminWrapper = userAdminWrapper;
	}

	/** Creates label and multiline text. */
	Text createLMT(Composite parent, String label, String value) {
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(label);
		lbl.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, false, false));
		Text text = new Text(parent, SWT.NONE);
		text.setText(value);
		text.setLayoutData(new GridData(SWT.LEAD, SWT.FILL, true, true));
		return text;
	}

	/** Creates label and password. */
	Text createLP(Composite parent, String label, String value) {
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(label);
		lbl.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, false, false));
		Text text = new Text(parent, SWT.PASSWORD | SWT.BORDER);
		text.setText(value);
		text.setLayoutData(new GridData(SWT.LEAD, SWT.FILL, true, false));
		return text;
	}

	/** Creates label and text. */
	Text createLT(Composite parent, String label, String value) {
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(label);
		lbl.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, false, false));
		lbl.setFont(EclipseUiUtils.getBoldFont(parent));
		Text text = new Text(parent, SWT.BORDER);
		text.setText(value);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		// CmsUiUtils.style(text, CmsWorkbenchStyles.WORKBENCH_FORM_TEXT);
		return text;
	}

	Text createReadOnlyLT(Composite parent, String label, String value) {
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(label);
		lbl.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, false, false));
		lbl.setFont(EclipseUiUtils.getBoldFont(parent));
		Text text = new Text(parent, SWT.NONE);
		text.setText(value);
		text.setLayoutData(new GridData(SWT.LEAD, SWT.FILL, true, false));
		text.setEditable(false);
		// CmsUiUtils.style(text, CmsWorkbenchStyles.WORKBENCH_FORM_TEXT);
		return text;
	}

}
