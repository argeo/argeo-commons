package org.argeo.cms.jcr.internal.osgi;

import java.util.Dictionary;

import org.argeo.cms.jcr.internal.StatisticsThread;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class CmsJcrActivator implements BundleActivator {
	private static BundleContext bundleContext;

//	private List<Runnable> stopHooks = new ArrayList<>();
	private StatisticsThread kernelThread;

//	private JackrabbitRepositoryContextsFactory repositoryServiceFactory;
//	private CmsJcrDeployment jcrDeployment;

	@Override
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
		
		// kernel thread
		kernelThread = new StatisticsThread("Kernel Thread");
		kernelThread.setContextClassLoader(getClass().getClassLoader());
		kernelThread.start();

		// JCR
//		repositoryServiceFactory = new JackrabbitRepositoryContextsFactory();
////		stopHooks.add(() -> repositoryServiceFactory.shutdown());
//		registerService(ManagedServiceFactory.class, repositoryServiceFactory,
//				LangUtils.dict(Constants.SERVICE_PID, CmsConstants.NODE_REPOS_FACTORY_PID));

//		JcrRepositoryFactory repositoryFactory = new JcrRepositoryFactory();
//		registerService(RepositoryFactory.class, repositoryFactory, null);

		// File System
//		CmsJcrFsProvider cmsFsProvider = new CmsJcrFsProvider();
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
//		registerService(FileSystemProvider.class, cmsFsProvider,
//				LangUtils.dict(Constants.SERVICE_PID, CmsConstants.NODE_FS_PROVIDER_PID));

//		jcrDeployment = new CmsJcrDeployment();
//		jcrDeployment.init();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
//		if (jcrDeployment != null)
//			jcrDeployment.destroy();

//		if (repositoryServiceFactory != null)
//			repositoryServiceFactory.shutdown();

		if (kernelThread != null)
			kernelThread.destroyAndJoin();

		bundleContext = null;
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
