package org.argeo.cms.ui;

import java.util.Set;

import org.eclipse.swt.widgets.Composite;

/** An extensible user interface base on the CMS backend. */
public interface CmsApp {
	/**
	 * If {@link Composite#setData(String, Object)} is set with this property, it
	 * indicates a different UI (typically with another theming. The {@link CmsApp}
	 * can use this information, but it doesn't have to be set, in which case a
	 * default UI must be provided. The provided value must belong to the values
	 * returned by {@link CmsApp#getUiNames()}.
	 */
	final static String UI_NAME_PROPERTY = CmsApp.class.getName() + ".ui.name";

	Set<String> getUiNames();

	Composite initUi(Composite parent);

	void refreshUi(Composite parent, String state);

	void setState(Composite parent, String state);

	CmsTheme getTheme(String uiName);

	boolean allThemesAvailable();

	void addCmsAppListener(CmsAppListener listener);

	void removeCmsAppListener(CmsAppListener listener);
}
