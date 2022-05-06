package org.argeo.api.cms;

import java.util.Set;

/** An extensible user interface base on the CMS backend. */
public interface CmsApp {
	/**
	 * If {@link CmsUi#setData(String, Object)} is set with this property, it
	 * indicates a different UI (typically with another theming. The {@link CmsApp}
	 * can use this information, but it doesn't have to be set, in which case a
	 * default UI must be provided. The provided value must belong to the values
	 * returned by {@link CmsApp#getUiNames()}.
	 */
	final static String UI_NAME_PROPERTY = CmsApp.class.getName() + ".ui.name";

	final static String CONTEXT_NAME_PROPERTY = "argeo.cms.app.contextName";

	Set<String> getUiNames();

	CmsUi initUi(Object uiParent);

	void refreshUi(CmsUi cmsUi, String state);

	void setState(CmsUi cmsUi, String state);

	CmsTheme getTheme(String uiName);

	boolean allThemesAvailable();

	void addCmsAppListener(CmsAppListener listener);

	void removeCmsAppListener(CmsAppListener listener);
}
