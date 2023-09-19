package org.argeo.cms.swt.app;

import java.util.HashSet;
import java.util.Set;

import org.argeo.api.cms.CmsApp;
import org.argeo.api.cms.ux.CmsUi;
import org.argeo.cms.AbstractCmsApp;
import org.argeo.cms.swt.CmsSwtUi;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/** Simplifies creating a simple {@link CmsApp} based on SWT. */
public class SimpleSwtApp extends AbstractCmsApp {
	protected final static String DEFAULT_UI_NAME = "app";

	protected void createDefaultUi(Composite parent) {

	}

	protected void createUi(String uiName, Composite parent) {
		if (DEFAULT_UI_NAME.equals(uiName)) {
			createDefaultUi(parent);
		}
	}

	@Override
	public Set<String> getUiNames() {
		Set<String> uiNames = new HashSet<>();
		uiNames.add(DEFAULT_UI_NAME);
		return uiNames;
	}

	@Override
	public CmsUi initUi(Object uiParent) {
		Composite parent = (Composite) uiParent;
		String uiName = parent.getData(UI_NAME_PROPERTY) != null ? parent.getData(UI_NAME_PROPERTY).toString() : null;
		CmsSwtUi cmsUi = new CmsSwtUi(parent, SWT.NONE);
		if (uiName != null)
			createUi(uiName, cmsUi);
		return cmsUi;
	}

	@Override
	public void refreshUi(CmsUi cmsUi, String state) {
	}

	@Override
	public void setState(CmsUi cmsUi, String state) {
	}

}
