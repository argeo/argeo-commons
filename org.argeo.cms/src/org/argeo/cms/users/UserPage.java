package org.argeo.cms.users;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.cms.CmsEditable;
import org.argeo.cms.CmsUiProvider;
import org.argeo.cms.CmsUtils;
import org.argeo.cms.viewers.JcrVersionCmsEditable;
import org.argeo.cms.widgets.ScrolledPage;
import org.argeo.security.UserAdminService;
import org.argeo.security.jcr.JcrSecurityModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/** Enable management of a given user */
public class UserPage implements CmsUiProvider {

	// Enable user CRUD // INJECTED
	private UserAdminService userAdminService;
	private JcrSecurityModel jcrSecurityModel;

	// public UserPage(UserAdminService userAdminService,
	// JcrSecurityModel jcrSecurityModel) {
	// this.userAdminService = userAdminService;
	// this.jcrSecurityModel = jcrSecurityModel;
	// }

	@Override
	public Control createUi(Composite parent, Node context)
			throws RepositoryException {
		CmsEditable cmsEditable = new JcrVersionCmsEditable(context);
		Composite page = createPage(parent);
		UserViewer userViewer = new UserViewer(page, SWT.NONE, context,
				cmsEditable);

		Control control = userViewer.getControl();
		Composite par = control.getParent();

		new Label(par, SWT.NONE).setText("Work in progress terminate.");

		UserRolesPart rolesPart = new UserRolesPart(par, SWT.NO_FOCUS, context,
				true);
		rolesPart.setUserAdminService(userAdminService);
		rolesPart.createControl(rolesPart, UserStyles.USER_FORM_TEXT);
		rolesPart.refresh();

		return page;
	}

	protected Composite createPage(Composite parent) {
		parent.setLayout(CmsUtils.noSpaceGridLayout());
		ScrolledPage scrolled = new ScrolledPage(parent, SWT.NONE);
		scrolled.setLayoutData(CmsUtils.fillAll());
		scrolled.setLayout(CmsUtils.noSpaceGridLayout());
		// TODO manage style
		// CmsUtils.style(scrolled, "maintenance_user_form");

		Composite page = new Composite(scrolled, SWT.NONE);
		page.setLayout(CmsUtils.noSpaceGridLayout());
		page.setBackgroundMode(SWT.INHERIT_NONE);
		page.setLayoutData(CmsUtils.fillAll());
		return page;
	}

	public void setUserAdminService(UserAdminService userAdminService) {
		this.userAdminService = userAdminService;
	}

	public void setJcrSecurityModel(JcrSecurityModel jcrSecurityModel) {
		this.jcrSecurityModel = jcrSecurityModel;
	}
}