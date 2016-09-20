package org.argeo.cms.ui.workbench.internal.useradmin.providers;

import static org.argeo.eclipse.ui.EclipseUiUtils.notEmpty;

import org.argeo.cms.auth.AuthConstants;
import org.argeo.cms.util.useradmin.UserAdminUtils;
import org.argeo.osgi.useradmin.LdifName;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.osgi.service.useradmin.User;

public class UserFilter extends ViewerFilter {
	private static final long serialVersionUID = 5082509381672880568L;

	private String searchString;
	private boolean showSystemRole = true;

	private final String[] knownProps = { LdifName.dn.name(),
			LdifName.cn.name(), LdifName.givenName.name(), LdifName.sn.name(),
			LdifName.uid.name(), LdifName.description.name(),
			LdifName.mail.name() };

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
						".*(" + AuthConstants.ROLES_BASEDN + ")"))
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