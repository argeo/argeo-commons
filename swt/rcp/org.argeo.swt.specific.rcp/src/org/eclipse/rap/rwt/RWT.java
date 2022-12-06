package org.eclipse.rap.rwt;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.argeo.eclipse.ui.rcp.internal.rwt.RcpClient;
import org.argeo.eclipse.ui.rcp.internal.rwt.RcpResourceManager;
import org.eclipse.rap.rwt.client.Client;
import org.eclipse.rap.rwt.service.ResourceManager;

public class RWT {
	public final static String CUSTOM_VARIANT = "argeo-rcp:CUSTOM_VARIANT";
	public final static String MARKUP_ENABLED = "argeo-rcp:MARKUP_ENABLED";
	public static final String TOOLTIP_MARKUP_ENABLED = "argeo-rcp:TOOLTIP_MARKUP_ENABLED";
	public final static String CUSTOM_ITEM_HEIGHT = "argeo-rcp:CUSTOM_ITEM_HEIGHT";
	public final static String ACTIVE_KEYS = "argeo-rcp:ACTIVE_KEYS";
	public final static String CANCEL_KEYS = "argeo-rcp:CANCEL_KEYS";
	public final static String DEFAULT_THEME_ID  = "argeo-rcp:DEFAULT_THEME_ID";

	public final static int HYPERLINK = 0;

	private static Locale locale = Locale.getDefault();
	private static RcpClient client = new RcpClient();
	private static ResourceManager resourceManager = new RcpResourceManager();
	static {

	}

	public static Locale getLocale() {
		return locale;
	}

	public static HttpServletRequest getRequest() {
		return null;
	}

	public static ResourceManager getResourceManager() {
		return resourceManager;
	}

	public static Client getClient() {
		return client;
	}
}
