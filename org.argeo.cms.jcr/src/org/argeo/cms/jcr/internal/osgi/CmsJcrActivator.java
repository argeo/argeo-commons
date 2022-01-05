package org.argeo.cms.jcr.internal.osgi;

import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import javax.jcr.RepositoryFactory;

import org.argeo.api.cms.CmsConstants;
import org.argeo.cms.jcr.internal.CmsFsProvider;
import org.argeo.cms.jcr.internal.StatisticsThread;
import org.argeo.cms.jcr.internal.NodeRepositoryFactory;
import org.argeo.cms.jcr.internal.RepositoryServiceFactory;
import org.argeo.util.LangUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.ManagedServiceFactory;

public class CmsJcrActivator implements BundleActivator {
	private static BundleContext bundleContext;
	static {
		Bundle bundle = FrameworkUtil.getBundle(CmsJcrActivator.class);
		if (bundle != null) {
			bundleContext = bundle.getBundleContext();
		}
	}

//	private List<Runnable> stopHooks = new ArrayList<>();
	private StatisticsThread kernelThread;

	private RepositoryServiceFactory repositoryServiceFactory;
//	private JcrDeployment jcrDeployment;

	@Override
	public void start(BundleContext context) throws Exception {
		// kernel thread
		kernelThread = new StatisticsThread("Kernel Thread");
		kernelThread.setContextClassLoader(getClass().getClassLoader());
		kernelThread.start();

		// JCR
		repositoryServiceFactory = new RepositoryServiceFactory();
//		stopHooks.add(() -> repositoryServiceFactory.shutdown());
		registerService(ManagedServiceFactory.class, repositoryServiceFactory,
				LangUtils.dict(Constants.SERVICE_PID, CmsConstants.NODE_REPOS_FACTORY_PID));

		NodeRepositoryFactory repositoryFactory = new NodeRepositoryFactory();
		registerService(RepositoryFactory.class, repositoryFactory, null);

		// File System
		CmsFsProvider cmsFsProvider = new CmsFsProvider();
//		ServiceLoader<FileSystemProvider> fspSl = ServiceLoader.load(FileSystemProvider.class);
//		for (FileSystemProvider fsp : fspSl) {
//			log.debug("FileSystemProvider " + fsp);
//			if (fsp instanceof CmsFsProvider) {
//				cmsFsProvider = (CmsFsProvider) fsp;
//			}
//		}
//		for (FileSystemProvider fsp : FileSystemProvider.installedProviders()) {
//			log.debug("Installed FileSystemProvider " + fsp);
//		}
		registerService(FileSystemProvider.class, cmsFsProvider,
				LangUtils.dict(Constants.SERVICE_PID, CmsConstants.NODE_FS_PROVIDER_PID));

//		jcrDeployment = new JcrDeployment();
//		jcrDeployment.init();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
//		if (jcrDeployment != null)
//			jcrDeployment.destroy();

		if (repositoryServiceFactory != null)
			repositoryServiceFactory.shutdown();

		if (kernelThread != null)
			kernelThread.destroyAndJoin();

	}

	@Deprecated
	public static <T> void registerService(Class<T> clss, T service, Dictionary<String, ?> properties) {
		if (bundleContext != null) {
			bundleContext.registerService(clss, service, properties);
		}

	}

	@Deprecated
	public static BundleContext getBundleContext() {
		return bundleContext;
	}

	@Deprecated
	public static <T> T getService(Class<T> clss) {
		if (bundleContext != null) {
			return bundleContext.getService(bundleContext.getServiceReference(clss));
		} else {
			return null;
		}
	}

}
