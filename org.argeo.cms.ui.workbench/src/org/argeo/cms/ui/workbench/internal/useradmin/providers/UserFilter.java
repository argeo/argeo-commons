package org.argeo.cms.ui.workbench.internal.useradmin.providers;

import static org.argeo.eclipse.ui.EclipseUiUtils.notEmpty;

import org.argeo.cms.util.UserAdminUtils;
import org.argeo.naming.LdapAttrs;
import org.argeo.node.NodeConstants;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.osgi.service.useradmin.User;

public class UserFilter extends ViewerFilter {
	private static final long serialVersionUID = 5082509381672880568L;

	private String searchString;
	private boolean showSystemRole = true;

	private final String[] knownProps = { LdapAttrs.DN, LdapAttrs.cn.name(),
			LdapAttrs.givenName.name(), LdapAttrs.sn.name(),
			LdapAttrs.uid.name(), LdapAttrs.description.name(),
			LdapAttrs.mail.name() };

	public void setSearchText(String s) {
		// ensure that the value can be used for matching
		if (notEmpty(s))
			searchString = ".*" + s.toLowerCase() + ".*";
		else
			searchString = ".*";
	}

	public void setShowSystemRole(boolean showSystemRole) {
		this.showSystemRole = showSystemRole;
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		User user = (User) element;
		if (!showSystemRole
				&& user.getName().matches(
						".*(" + NodeConstants.ROLES_BASEDN + ")"))
			// UserAdminUtils.getProperty(user, LdifName.dn.name())
			// .toLowerCase().endsWith(AuthConstants.ROLES_BASEDN))
			return false;

		if (searchString == null || searchString.length() == 0)
			return true;

		if (user.getName().matches(searchString))
			return true;

		for (String key : knownProps) {
			String currVal = UserAdminUtils.getProperty(user, key);
			if (notEmpty(currVal)
					&& currVal.toLowerCase().matches(searchString))
				return true;
		}
		return false;
	}
}
