package org.argeo.cms;

import org.argeo.cms.i18n.DefaultsResourceBundle;
import org.argeo.cms.i18n.Msg;

/** Standard CMS messages. */
public class CmsMsg extends DefaultsResourceBundle {
	public final static Msg username = new Msg("username");
	public final static Msg password = new Msg("password");
	public final static Msg logout = new Msg("log out");
	public final static Msg login = new Msg("sign in");
	public final static Msg register = new Msg("register");

	public final static Msg changePassword = new Msg("change password");
	public final static Msg currentPassword = new Msg("current password");
	public final static Msg newPassword = new Msg("new password");
	public final static Msg repeatNewPassword = new Msg("repeat new password");
	public final static Msg passwordChanged = new Msg("password changed");

	static {
		Msg.init(CmsMsg.class);
	}
}
