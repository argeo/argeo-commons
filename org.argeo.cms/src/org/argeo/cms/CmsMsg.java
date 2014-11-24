package org.argeo.cms;

/** Standard CMS messages. */
public class CmsMsg extends DefaultsResourceBundle {
	public final static Msg username = new Msg("username");
	public final static Msg password = new Msg("password");
	public final static Msg logout = new Msg("log out");

	static {
		Msg.init(CmsMsg.class);
	}
}
