package org.argeo.cms.maintenance;

import java.net.URI;

import org.argeo.cms.util.CmsUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;

class SecurityDeploymentUi extends AbstractOsgiComposite {
	private static final long serialVersionUID = 590221539553514693L;

	public SecurityDeploymentUi(Composite parent, int style) {
		super(parent, style);
	}

	@Override
	protected void initUi(int style) {
		if (isDeployed()) {
			initCurrentUi(this);
		} else {
			initNewUi(this);
		}
	}

	private void initNewUi(Composite parent) {
		new Label(parent, SWT.NONE).setText("Security is not configured");
	}

	private void initCurrentUi(Composite parent) {
		ServiceReference<UserAdmin> userAdminRef = bc.getServiceReference(UserAdmin.class);
		UserAdmin userAdmin = bc.getService(userAdminRef);
		StringBuffer text = new StringBuffer();
		text.append("<span style='font-variant: small-caps;'>Domains</span><br/>");
		domains: for (String key : userAdminRef.getPropertyKeys()) {
			if (!key.startsWith("/"))
				continue domains;
			URI uri;
			try {
				uri = new URI(key);
			} catch (Exception e) {
				// ignore non URI keys
				continue domains;
			}

			String rootDn = uri.getPath().substring(1, uri.getPath().length());
			// FIXME make reading query options more robust, using utils
			boolean readOnly = uri.getQuery().equals("readOnly=true");
			if (readOnly)
				text.append("<span style='font-weight:bold;font-style: italic'>");
			else
				text.append("<span style='font-weight:bold'>");

			text.append(rootDn);
			text.append("</span><br/>");
			try {
				Role[] roles = userAdmin.getRoles("(dn=*," + rootDn + ")");
				long userCount = 0;
				long groupCount = 0;
				for (Role role : roles) {
					if (role.getType() == Role.USER)
						userCount++;
					else
						groupCount++;
				}
				text.append(" " + userCount + " users, " + groupCount +" groups.<br/>");
			} catch (InvalidSyntaxException e) {
				log.error("Invalid syntax", e);
			}
		}
		Label label = new Label(parent, SWT.NONE);
		label.setData(new GridData(SWT.FILL, SWT.FILL, false, false));
		CmsUtils.markup(label);
		label.setText(text.toString());
	}

	protected boolean isDeployed() {
		return bc.getServiceReference(UserAdmin.class) != null;
	}
}
