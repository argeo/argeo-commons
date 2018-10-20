package org.argeo.cms.script;

import javax.jcr.Repository;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class ScriptAppActivator implements BundleActivator {
	@Override
	public void start(BundleContext context) throws Exception {
		CmsScriptRwtApplication appConfig = new CmsScriptRwtApplication();
		appConfig.init(context);
		CmsScriptApp app = appConfig.getApp();
		ServiceTracker<Repository, Repository> repoSt = new ServiceTracker<Repository, Repository>(context,
				FrameworkUtil.createFilter("(&" + app.getRepo() + "(objectClass=javax.jcr.Repository))"), null) {

			@Override
			public Repository addingService(ServiceReference<Repository> reference) {
				Repository repository = super.addingService(reference);
				appConfig.setRepository(repository);
				CmsScriptApp app = appConfig.getApp();
				app.register(context, appConfig);
				return repository;
			}

		};
		repoSt.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

}
