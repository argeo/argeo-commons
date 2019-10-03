package org.argeo.cms.script;

import javax.jcr.Repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class ScriptAppActivator implements BundleActivator {
	private final static Log log = LogFactory.getLog(ScriptAppActivator.class);

	@Override
	public void start(BundleContext context) throws Exception {
		try {
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
		} catch (Exception e) {
			log.error("Cannot initialise script bundle " + context.getBundle().getSymbolicName(), e);
			throw e;
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

}
