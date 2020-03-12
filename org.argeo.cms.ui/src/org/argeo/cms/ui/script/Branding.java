package org.argeo.cms.ui.script;

import java.util.Map;

public interface Branding {
	public void applyBranding(Map<String, String> properties);

	public String getThemeId();

	public String getAdditionalHeaders();

	public String getBodyHtml();

	public String getPageTitle();

	public String getPageOverflow();

	public String getFavicon();

}
