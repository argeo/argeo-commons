package org.argeo.maintenance.internal;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.jcr.Repository;

import org.argeo.maintenance.backup.LogicalBackup;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		// Start backup
		Repository repository = context.getService(context.getServiceReference(Repository.class));
		Path basePath = Paths.get(System.getProperty("user.dir"), "backup");
		LogicalBackup backup = new LogicalBackup(context, repository, basePath);
		backup.run();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

}
