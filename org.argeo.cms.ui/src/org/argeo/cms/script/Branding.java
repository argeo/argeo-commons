package org.argeo.cms.script;

import java.util.Map;

import org.eclipse.rap.rwt.client.WebClient;

public interface Branding {
	public void applyBranding(Map<String, String> properties);

	public String getThemeId();

	public String getAdditionalHeaders();

	public String getBodyHtml();

	public String getPageTitle();

	public String getPageOverflow();

	public String getFavicon();

}
