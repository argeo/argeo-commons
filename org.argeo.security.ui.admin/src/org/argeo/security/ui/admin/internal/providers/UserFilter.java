package org.argeo.security.ui.admin.internal.providers;

import org.argeo.osgi.useradmin.LdifName;
import org.argeo.security.ui.admin.internal.UiAdminUtils;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.osgi.service.useradmin.User;

public class UserFilter extends ViewerFilter {
	private static final long serialVersionUID = 5082509381672880568L;

	private String searchString;

	private final String[] knownProps = { LdifName.dn.name(),
			LdifName.cn.name(), LdifName.givenname.name(), LdifName.sn.name(),
			LdifName.uid.name(), LdifName.description.name(),
			LdifName.mail.name() };

	public void setSearchText(String s) {
		// ensure that the value can be used for matching
		if (notNull(s))
			searchString = ".*" + s.toLowerCase() + ".*";
		else
			searchString = ".*";
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (searchString == null || searchString.length() == 0) {
			return true;
		}
		User user = (User) element;

		if (user.getName().matches(searchString))
			return true;

		for (String key : knownProps) {
			String currVal = UiAdminUtils.getProperty(user, key);
			if (notNull(currVal) && currVal.toLowerCase().matches(searchString))
				return true;
		}

		return false;
	}

	private boolean notNull(String str) {
		return !(str == null || "".equals(str.trim()));
	}

}