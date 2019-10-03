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

/** Base class for CMS RAP applications. */
public abstract class AbstractRapE4App implements ApplicationConfiguration {
	private String e4Xmi;
	private String path;
	private String lifeCycleUri = "bundleclass://org.argeo.cms.e4.rap/org.argeo.cms.e4.rap.CmsLoginLifecycle";

	Map<String, String> baseProperties = new HashMap<String, String>();

	public void configure(Application application) {
		application.setExceptionHandler(new ExceptionHandler() {

			@Override
			public void handleException(Throwable throwable) {
				CmsFeedback.show("Unexpected RWT exception", throwable);
			}
		});

		if (e4Xmi != null) {// backward compatibility
			addE4EntryPoint(application, path, e4Xmi, getBaseProperties());
		} else {
			addEntryPoints(application);
		}
	}

	/**
	 * To be overridden in order to add multiple entry points, directly or using
	 * {@link #addE4EntryPoint(Application, String, String, Map)}.
	 */
	protected void addEntryPoints(Application application) {

	}

	protected Map<String, String> getBaseProperties() {
		return baseProperties;
	}

//	protected void addEntryPoint(Application application, E4ApplicationConfig config, Map<String, String> properties) {
//		CmsE4EntryPointFactory entryPointFactory = new CmsE4EntryPointFactory(config);
//		application.addEntryPoint(path, entryPointFactory, properties);
//		application.setOperationMode(OperationMode.SWT_COMPATIBILITY);
//	}

	protected void addE4EntryPoint(Application application, String path, String e4Xmi, Map<String, String> properties) {
		E4ApplicationConfig config = new E4ApplicationConfig(e4Xmi, lifeCycleUri, null, null, false, true, true);
		CmsE4EntryPointFactory entryPointFactory = new CmsE4EntryPointFactory(config);
		application.addEntryPoint(path, entryPointFactory, properties);
		application.setOperationMode(OperationMode.SWT_COMPATIBILITY);
	}

	public void setPageTitle(String pageTitle) {
		if (pageTitle != null)
			baseProperties.put(WebClient.PAGE_TITLE, pageTitle);
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

	public void init(Map<String, Object> properties) {
		for (String key : properties.keySet()) {
			Object value = properties.get(key);
			if (value != null)
				baseProperties.put(key, value.toString());
		}
	}
}
