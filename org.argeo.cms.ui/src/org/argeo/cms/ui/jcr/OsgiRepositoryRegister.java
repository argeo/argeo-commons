package org.argeo.cms.ui.jcr;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Repository;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class OsgiRepositoryRegister extends DefaultRepositoryRegister {
	private final static BundleContext bc = FrameworkUtil.getBundle(OsgiRepositoryRegister.class).getBundleContext();
	private final ServiceTracker<Repository, Repository> repositoryTracker;

	public OsgiRepositoryRegister() {
		repositoryTracker = new ServiceTracker<Repository, Repository>(bc, Repository.class, null) {

			@Override
			public Repository addingService(ServiceReference<Repository> reference) {

				Repository repository = super.addingService(reference);
				Map<String, Object> props = new HashMap<>();
				for (String key : reference.getPropertyKeys()) {
					props.put(key, reference.getProperty(key));
				}
				register(repository, props);
				return repository;
			}

			@Override
			public void removedService(ServiceReference<Repository> reference, Repository service) {
				Map<String, Object> props = new HashMap<>();
				for (String key : reference.getPropertyKeys()) {
					props.put(key, reference.getProperty(key));
				}
				unregister(service, props);
				super.removedService(reference, service);
			}

		};
	}

	public void init() {
		repositoryTracker.open();
	}

	public void destroy() {
		repositoryTracker.close();
	}
}
