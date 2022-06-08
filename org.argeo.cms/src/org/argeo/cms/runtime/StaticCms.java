package org.argeo.cms.runtime;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.concurrent.CompletableFuture;

import org.argeo.api.cms.CmsContext;
import org.argeo.api.cms.CmsDeployment;
import org.argeo.api.cms.CmsState;
import org.argeo.cms.internal.osgi.DeployConfig;
import org.argeo.cms.internal.runtime.CmsContextImpl;
import org.argeo.cms.internal.runtime.CmsDeploymentImpl;
import org.argeo.cms.internal.runtime.CmsStateImpl;
import org.argeo.cms.internal.runtime.CmsUserAdmin;
import org.argeo.osgi.transaction.SimpleTransactionManager;
import org.argeo.osgi.transaction.WorkControl;
import org.argeo.osgi.transaction.WorkTransaction;
import org.argeo.util.register.Component;
import org.argeo.util.register.SimpleRegister;
import org.osgi.service.useradmin.UserAdmin;

/**
 * A CMS assembly which is programatically defined, as an alternative to OSGi
 * deployment. Useful for testing or AOT compilation.
 */
public class StaticCms {
	private static SimpleRegister register = new SimpleRegister();

	private CompletableFuture<Void> stopped = new CompletableFuture<Void>();

	public void start() {
		// CMS State
		CmsStateImpl cmsState = new CmsStateImpl();
		Component<CmsStateImpl> cmsStateC = new Component.Builder<>(cmsState) //
				.addType(CmsState.class) //
				.addActivation(cmsState::start) //
				.addDeactivation(cmsState::stop) //
				.build(register);

		// Deployment Configuration
		DeployConfig deployConfig = new DeployConfig();
		Component<DeployConfig> deployConfigC = new Component.Builder<>(deployConfig) //
				.addType(DeployConfig.class) //
				.addActivation(deployConfig::start) //
				.addDeactivation(deployConfig::stop) //
				.build(register);

		// CMS Deployment
		CmsDeploymentImpl cmsDeployment = new CmsDeploymentImpl();
		Component<CmsDeploymentImpl> cmsDeploymentC = new Component.Builder<>(cmsDeployment) //
				.addType(CmsDeployment.class) //
				.addActivation(cmsDeployment::start) //
				.addDeactivation(cmsDeployment::stop) //
				.addDependency(cmsStateC.getType(CmsState.class), cmsDeployment::setCmsState, null) //
				.addDependency(deployConfigC.getType(DeployConfig.class), cmsDeployment::setDeployConfig, null) //
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
				.addDependency(deployConfigC.getType(DeployConfig.class), (d) -> {
					for (Dictionary<String, Object> userDirectoryConfig : d.getUserDirectoryConfigs())
						userAdmin.enableUserDirectory(userDirectoryConfig);
				}, null) //
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
