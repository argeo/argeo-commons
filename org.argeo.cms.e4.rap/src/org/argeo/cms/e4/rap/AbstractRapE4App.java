package org.argeo.cms.e4.rap;

import java.util.HashMap;
import java.util.Map;

import org.argeo.cms.ui.dialogs.CmsFeedback;
import org.eclipse.rap.e4.E4ApplicationConfig;
import org.eclipse.rap.rwt.application.Application;
import org.eclipse.rap.rwt.application.Application.OperationMode;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.eclipse.rap.rwt.application.ExceptionHandler;
import org.eclipse.rap.rwt.client.WebClient;

public abstract class AbstractRapE4App implements ApplicationConfiguration {
	private String pageTitle;
	private String e4Xmi;
	private String path;
	private String lifeCycleUri = "bundleclass://org.argeo.cms.e4.rap/org.argeo.cms.e4.rap.CmsLoginLifecycle";

	public void configure(Application application) {
		application.setExceptionHandler(new ExceptionHandler() {

			@Override
			public void handleException(Throwable throwable) {
				CmsFeedback.show("Unexpected RWT exception", throwable);
			}
		});

		Map<String, String> properties = new HashMap<String, String>();
		properties.put(WebClient.PAGE_TITLE, pageTitle);
		E4ApplicationConfig config = new E4ApplicationConfig(e4Xmi, lifeCycleUri, null, null, false, true, true);
		addEntryPoint(application, config, properties);
	}

	protected void addEntryPoint(Application application, E4ApplicationConfig config, Map<String, String> properties) {
		CmsE4EntryPointFactory entryPointFactory = new CmsE4EntryPointFactory(config);
		application.addEntryPoint(path, entryPointFactory, properties);
		application.setOperationMode(OperationMode.SWT_COMPATIBILITY);
	}

	public void setPageTitle(String pageTitle) {
		this.pageTitle = pageTitle;
	}

	public void setE4Xmi(String e4Xmi) {
		this.e4Xmi = e4Xmi;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setLifeCycleUri(String lifeCycleUri) {
		this.lifeCycleUri = lifeCycleUri;
	}

}
