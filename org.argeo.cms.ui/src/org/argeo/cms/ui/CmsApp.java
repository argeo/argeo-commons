package org.argeo.cms.ui;

import java.util.Set;

import org.eclipse.swt.widgets.Composite;

/** An extensible user interface base on the CMS backend. */
public interface CmsApp {
	Set<String> getUiNames();

	void initUi(String uiName, Composite parent);

	CmsTheme getTheme(String uiName);

	void addCmsAppListener(CmsAppListener listener);

	void removeCmsAppListener(CmsAppListener listener);
}
