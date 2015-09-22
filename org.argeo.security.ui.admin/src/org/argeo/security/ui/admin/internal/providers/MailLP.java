package org.argeo.security.ui.admin.internal.providers;

import java.util.Dictionary;

import org.osgi.service.useradmin.User;

/** Simply declare a label provider that returns the Primary Mail for a user */
public class MailLP extends UserAdminAbstractLP {
	private static final long serialVersionUID = 8329764452141982707L;

	@Override
	public String getText(User user) {
		@SuppressWarnings("rawtypes")
		Dictionary props = user.getProperties();
		Object obj = props.get(KEY_MAIL);
		if (obj != null)
			return (String) obj;
		else
			return "";
	}
}