package org.argeo.cms.runtime;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.argeo.api.acr.ContentRepository;
import org.argeo.api.acr.spi.ProvidedRepository;
import org.argeo.api.cms.CmsContext;
import org.argeo.api.cms.CmsDeployment;
import org.argeo.api.cms.CmsState;
import org.argeo.api.uuid.UuidFactory;
import org.argeo.cms.CmsUserManager;
import org.argeo.cms.acr.CmsUuidFactory;
import org.argeo.cms.internal.auth.CmsUserManagerImpl;
import org.argeo.cms.internal.osgi.DeployConfig;
import org.argeo.cms.internal.runtime.CmsContextImpl;
import org.argeo.cms.internal.runtime.CmsDeploymentImpl;
import org.argeo.cms.internal.runtime.CmsStateImpl;
import org.argeo.cms.internal.runtime.CmsUserAdmin;
import org.argeo.cms.internal.runtime.DeployedContentRepository;
import org.argeo.osgi.useradmin.UserDirectory;
import org.argeo.util.register.Component;
import org.argeo.util.register.SimpleRegister;
import org.argeo.util.transaction.SimpleTransactionManager;
import org.argeo.util.transaction.WorkControl;
import org.argeo.util.transaction.WorkTransaction;
import org.osgi.service.useradmin.UserAdmin;

/**
 * A CMS assembly which is programatically defined, as an alternative to OSGi
 * deployment. Useful for testing or AOT compilation.
 */
public class StaticCms {
	private static SimpleRegister register = new SimpleRegister();

	private CompletableFuture<Void> stopped = new CompletableFuture<Void>();

	public void start() {
		// UID factory
		CmsUuidFactory uuidFactory = new CmsUuidFactory();
		Component<CmsUuidFactory> uuidFactoryC = new Component.Builder<>(uuidFactory) //
				.addType(UuidFactory.class) //
				.build(register);

		// CMS State
		CmsStateImpl cmsState = new CmsStateImpl();
		Component<CmsStateImpl> cmsStateC = new Component.Builder<>(cmsState) //
				.addType(CmsState.class) //
				.addActivation(cmsState::start) //
				.addDeactivation(cmsState::stop) //
				.addDependency(uuidFactoryC.getType(UuidFactory.class), cmsState::setUuidFactory, null) //
				.build(register);

		// Deployment Configuration
//		DeployConfig deployConfig = new DeployConfig();
//		Component<DeployConfig> deployConfigC = new Component.Builder<>(deployConfig) //
//				.addType(DeployConfig.class) //
//				.addActivation(deployConfig::start) //
//				.addDeactivation(deployConfig::stop) //
//				.build(register);

		// CMS Deployment
		CmsDeploymentImpl cmsDeployment = new CmsDeploymentImpl();
		Component<CmsDeploymentImpl> cmsDeploymentC = new Component.Builder<>(cmsDeployment) //
				.addType(CmsDeployment.class) //
				.addActivation(cmsDeployment::start) //
				.addDeactivation(cmsDeployment::stop) //
				.addDependency(cmsStateC.getType(CmsState.class), cmsDeployment::setCmsState, null) //
//				.addDependency(deployConfigC.getType(DeployConfig.class), cmsDeployment::setDeployConfig, null) //
				.build(register);

		// Transaction manager
		SimpleTransactionManager transactionManager = new SimpleTransactionManager();
		Component<SimpleTransactionManager> transactionManagerC = new Component.Builder<>(transactionManager) //
				.addType(WorkControl.class) //
				.addType(WorkTransaction.class) //
				.build(register);

		// User Admin
		CmsUserAdmin userAdmin = new CmsUserAdmin();
		Component<CmsUserAdmin> userAdminC = new Component.Builder<>(userAdmin) //
				.addType(UserAdmin.class) //
				.addDependency(transactionManagerC.getType(WorkControl.class), userAdmin::setTransactionManager, null) //
				.addDependency(transactionManagerC.getType(WorkTransaction.class), userAdmin::setUserTransaction, null) //
//				.addDependency(deployConfigC.getType(DeployConfig.class), (d) -> {
//					for (Dictionary<String, Object> userDirectoryConfig : d.getUserDirectoryConfigs())
//						userAdmin.enableUserDirectory(userDirectoryConfig);
//				}, null) //
				.build(register);

		// User manager
		CmsUserManagerImpl userManager = new CmsUserManagerImpl();
//		for (UserDirectory userDirectory : userAdmin.getUserDirectories()) {
//			// FIXME deal with properties
//			userManager.addUserDirectory(userDirectory, new HashMap<>());
//		}
		Component<CmsUserManagerImpl> userManagerC = new Component.Builder<>(userManager) //
				.addType(CmsUserManager.class) //
				.addDependency(userAdminC.getType(UserAdmin.class), userManager::setUserAdmin, null) //
				.addDependency(transactionManagerC.getType(WorkTransaction.class), userManager::setUserTransaction,
						null) //
				.build(register);

		// Content Repository
		DeployedContentRepository contentRepository = new DeployedContentRepository();
		Component<DeployedContentRepository> contentRepositoryC = new Component.Builder<>(contentRepository) //
				.addType(ProvidedRepository.class) //
				.addType(ContentRepository.class) //
				.addActivation(contentRepository::start) //
				.addDeactivation(contentRepository::stop) //
				.addDependency(cmsStateC.getType(CmsState.class), contentRepository::setCmsState, null) //
				.addDependency(uuidFactoryC.getType(UuidFactory.class), contentRepository::setUuidFactory, null) //
				.addDependency(userManagerC.getType(CmsUserManager.class), contentRepository::setUserManager, null) //
				.build(register);

		// CMS Context
		CmsContextImpl cmsContext = new CmsContextImpl();
		Component<CmsContextImpl> cmsContextC = new Component.Builder<>(cmsContext) //
				.addType(CmsContext.class) //
				.addActivation(cmsContext::start) //
				.addDeactivation(cmsContext::stop) //
				.addDependency(cmsStateC.getType(CmsState.class), cmsContext::setCmsState, null) //
				.addDependency(cmsDeploymentC.getType(CmsDeployment.class), cmsContext::setCmsDeployment, null) //
				.addDependency(userAdminC.getType(UserAdmin.class), cmsContext::setUserAdmin, null) //
				.addDependency(uuidFactoryC.getType(UuidFactory.class), cmsContext::setUuidFactory, null) //
//				.addDependency(contentRepositoryC.getType(ProvidedRepository.class), cmsContext::setContentRepository,
//						null) //
				.build(register);
		assert cmsContextC.get() == cmsContext;

		register.activate();
	}

	public void stop() {
		if (register.isActive()) {
			register.deactivate();
		}
		register.clear();
		stopped.complete(null);
	}

	public void waitForStop() {
		stopped.join();
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("Usage: <data path>");
			System.exit(1);
		}
		Path instancePath = Paths.get(args[0]);
		System.setProperty("osgi.instance.area", instancePath.toUri().toString());

		StaticCms staticCms = new StaticCms();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> staticCms.stop(), "Static CMS Shutdown"));
		staticCms.start();
		staticCms.waitForStop();
	}

}
