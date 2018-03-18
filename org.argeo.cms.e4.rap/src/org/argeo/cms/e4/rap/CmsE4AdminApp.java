package org.argeo.cms.e4.rap;

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;

import org.eclipse.rap.e4.E4ApplicationConfig;
import org.eclipse.rap.e4.E4EntryPointFactory;
import org.eclipse.rap.rwt.application.Application;
import org.eclipse.rap.rwt.application.Application.OperationMode;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.eclipse.rap.rwt.application.EntryPoint;
import org.eclipse.rap.rwt.client.WebClient;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class CmsE4AdminApp implements ApplicationConfiguration {
	private final BundleContext bc = FrameworkUtil.getBundle(CmsE4AdminApp.class).getBundleContext();

	String pageTitle = "CMS Admin";
	String e4Xmi = "org.argeo.cms.e4/cms-admin.e4xmi";
	String path = "/admin";
	String lifeCycleUri = "bundleclass://" + bc.getBundle().getSymbolicName() + "/" + CmsLoginLifecycle.class.getName();

	public void configure(Application application) {

		Map<String, String> properties = new HashMap<String, String>();
		properties.put(WebClient.PAGE_TITLE, pageTitle);
		E4ApplicationConfig config = new E4ApplicationConfig(e4Xmi, lifeCycleUri, null, false, true, true);
		config.isClearPersistedState();
		E4EntryPointFactory entryPointFactory = new E4EntryPointFactory(config) {

			@Override
			public EntryPoint create() {
				Subject subject = new Subject();
				EntryPoint ep = createEntryPoint();
				EntryPoint authEp = new EntryPoint() {

					@Override
					public int createUI() {
						return Subject.doAs(subject, new PrivilegedAction<Integer>() {

							@Override
							public Integer run() {
								return ep.createUI();
							}

						});
					}
				};
				return authEp;
			}

			protected EntryPoint createEntryPoint() {
				return super.create();
			}

		};
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

}
