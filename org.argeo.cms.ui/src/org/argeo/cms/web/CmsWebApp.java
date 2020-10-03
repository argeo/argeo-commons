package org.argeo.cms.web;

import java.util.Map;

import org.argeo.cms.ui.CmsApp;
import org.eclipse.rap.rwt.application.AbstractEntryPoint;
import org.eclipse.rap.rwt.application.Application;
import org.eclipse.swt.widgets.Composite;

public class CmsWebApp extends MinimalWebApp {
	private CmsApp cmsApp;

	@Override
	protected void addEntryPoints(Application application, Map<String, String> properties) {
		for (String uiName : cmsApp.getUiNames()) {
			application.addEntryPoint("/" + uiName, () -> {
				return new AbstractEntryPoint() {
					private static final long serialVersionUID = -9153259126766694485L;

					@Override
					protected void createContents(Composite parent) {
						cmsApp.initUi(uiName, parent);

					}
				};
			}, properties);
		}
	}

	public CmsApp getCmsApp() {
		return cmsApp;
	}

	public void setCmsApp(CmsApp cmsApp) {
		this.cmsApp = cmsApp;
	}

}
