package org.argeo.cms.internal.kernel;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.Dictionary;

import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.api.NodeConstants;
import org.argeo.api.NodeDeployment;
import org.argeo.api.NodeState;
import org.argeo.osgi.useradmin.UserAdminConf;
import org.eclipse.equinox.http.jetty.JettyConfigurator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.http.HttpService;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.util.tracker.ServiceTracker;

/** Implementation of a CMS deployment. */
public class CmsDeployment implements NodeDeployment {
	private final Log log = LogFactory.getLog(getClass());
	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

	private DeployConfig deployConfig;

	private Long availableSince;


	// Readiness
	private boolean nodeAvailable = false;
	private boolean userAdminAvailable = false;
	private boolean httpExpected = false;
	private boolean httpAvailable = false;

	public CmsDeployment() {
//		ServiceReference<NodeState> nodeStateSr = bc.getServiceReference(NodeState.class);
//		if (nodeStateSr == null)
//			throw new CmsException("No node state available");

//		NodeState nodeState = bc.getService(nodeStateSr);
//		cleanState = nodeState.isClean();

//		nodeHttp = new NodeHttp();
		initTrackers();
	}

	private void initTrackers() {
		ServiceTracker<?, ?> httpSt = new ServiceTracker<HttpService, HttpService>(bc, HttpService.class, null) {

			@Override
			public HttpService addingService(ServiceReference<HttpService> sr) {
				httpAvailable = true;
				Object httpPort = sr.getProperty("http.port");
				Object httpsPort = sr.getProperty("https.port");
				log.info(httpPortsMsg(httpPort, httpsPort));
				checkReadiness();
				return super.addingService(sr);
			}
		};
		// httpSt.open();
		KernelUtils.asyncOpen(httpSt);


		ServiceTracker<?, ?> userAdminSt = new ServiceTracker<UserAdmin, UserAdmin>(bc, UserAdmin.class, null) {
			@Override
			public UserAdmin addingService(ServiceReference<UserAdmin> reference) {
				UserAdmin userAdmin = super.addingService(reference);
				addStandardSystemRoles(userAdmin);
				userAdminAvailable = true;
				checkReadiness();
				return userAdmin;
			}
		};
		// userAdminSt.open();
		KernelUtils.asyncOpen(userAdminSt);

		ServiceTracker<?, ?> confAdminSt = new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(bc,
				ConfigurationAdmin.class, null) {
			@Override
			public ConfigurationAdmin addingService(ServiceReference<ConfigurationAdmin> reference) {
				ConfigurationAdmin configurationAdmin = bc.getService(reference);
				boolean isClean;
				try {
					Configuration[] confs = configurationAdmin
							.listConfigurations("(service.factoryPid=" + NodeConstants.NODE_USER_ADMIN_PID + ")");
					isClean = confs == null || confs.length == 0;
				} catch (Exception e) {
					throw new IllegalStateException("Cannot analyse clean state", e);
				}
				deployConfig = new DeployConfig(configurationAdmin,  isClean);
				Activator.registerService(NodeDeployment.class, CmsDeployment.this, null);
//				JcrInitUtils.addToDeployment(CmsDeployment.this);
				httpExpected = deployConfig.getProps(KernelConstants.JETTY_FACTORY_PID, "default") != null;
				try {
					Configuration[] configs = configurationAdmin
							.listConfigurations("(service.factoryPid=" + NodeConstants.NODE_USER_ADMIN_PID + ")");

					boolean hasDomain = false;
					for (Configuration config : configs) {
						Object realm = config.getProperties().get(UserAdminConf.realm.name());
						if (realm != null) {
							log.debug("Found realm: " + realm);
							hasDomain = true;
						}
					}
					if (hasDomain) {
						loadIpaJaasConfiguration();
					}
				} catch (Exception e) {
					throw new IllegalStateException("Cannot initialize config", e);
				}
				return super.addingService(reference);
			}
		};
		// confAdminSt.open();
		KernelUtils.asyncOpen(confAdminSt);
	}

	public void addFactoryDeployConfig(String factoryPid, Dictionary<String, Object> props) {
		deployConfig.putFactoryDeployConfig(factoryPid, props);
		deployConfig.save();
		try {
			deployConfig.loadConfigs();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public Dictionary<String, Object> getProps(String factoryPid, String cn) {
		return deployConfig.getProps(factoryPid, cn);
	}

	private String httpPortsMsg(Object httpPort, Object httpsPort) {
		return (httpPort != null ? "HTTP " + httpPort + " " : " ") + (httpsPort != null ? "HTTPS " + httpsPort : "");
	}

	private void addStandardSystemRoles(UserAdmin userAdmin) {
		// we assume UserTransaction is already available (TODO make it more robust)
		UserTransaction userTransaction = bc.getService(bc.getServiceReference(UserTransaction.class));
		try {
			userTransaction.begin();
			Role adminRole = userAdmin.getRole(NodeConstants.ROLE_ADMIN);
			if (adminRole == null) {
				adminRole = userAdmin.createRole(NodeConstants.ROLE_ADMIN, Role.GROUP);
			}
			if (userAdmin.getRole(NodeConstants.ROLE_USER_ADMIN) == null) {
				Group userAdminRole = (Group) userAdmin.createRole(NodeConstants.ROLE_USER_ADMIN, Role.GROUP);
				userAdminRole.addMember(adminRole);
			}
			userTransaction.commit();
		} catch (Exception e) {
			try {
				userTransaction.rollback();
			} catch (Exception e1) {
				// silent
			}
			throw new IllegalStateException("Cannot add standard system roles", e);
		}
	}

	private void loadIpaJaasConfiguration() {
		if (System.getProperty(KernelConstants.JAAS_CONFIG_PROP) == null) {
			String jaasConfig = KernelConstants.JAAS_CONFIG_IPA;
			URL url = getClass().getClassLoader().getResource(jaasConfig);
			KernelUtils.setJaasConfiguration(url);
			log.debug("Set IPA JAAS configuration.");
		}
	}

	public void shutdown() {
//		if (nodeHttp != null)
//			nodeHttp.destroy();


		try {
			JettyConfigurator.stopServer(KernelConstants.DEFAULT_JETTY_SERVER);
		} catch (Exception e) {
			log.error("Cannot stop default Jetty server.", e);
		}

		if (deployConfig != null) {
			new Thread(() -> deployConfig.save(), "Save Argeo Deploy Config").start();
		}
	}

	/**
	 * Checks whether the deployment is available according to expectations, and
	 * mark it as available.
	 */
	private synchronized void checkReadiness() {
		if (isAvailable())
			return;
		if (nodeAvailable && userAdminAvailable && (httpExpected ? httpAvailable : true)) {
			String data = KernelUtils.getFrameworkProp(KernelUtils.OSGI_INSTANCE_AREA);
			String state = KernelUtils.getFrameworkProp(KernelUtils.OSGI_CONFIGURATION_AREA);
			availableSince = System.currentTimeMillis();
			long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
			String jvmUptimeStr = " in " + (jvmUptime / 1000) + "." + (jvmUptime % 1000) + "s";
			log.info("## ARGEO NODE AVAILABLE" + (log.isDebugEnabled() ? jvmUptimeStr : "") + " ##");
			if (log.isDebugEnabled()) {
				log.debug("## state: " + state);
				if (data != null)
					log.debug("## data: " + data);
			}
			long begin = bc.getService(bc.getServiceReference(NodeState.class)).getAvailableSince();
			long initDuration = System.currentTimeMillis() - begin;
			if (log.isTraceEnabled())
				log.trace("Kernel initialization took " + initDuration + "ms");
			tributeToFreeSoftware(initDuration);
		}
	}

	final private void tributeToFreeSoftware(long initDuration) {
		if (log.isTraceEnabled()) {
			long ms = initDuration / 100;
			log.trace("Spend " + ms + "ms" + " reflecting on the progress brought to mankind" + " by Free Software...");
			long beginNano = System.nanoTime();
			try {
				Thread.sleep(ms, 0);
			} catch (InterruptedException e) {
				// silent
			}
			long durationNano = System.nanoTime() - beginNano;
			final double M = 1000d * 1000d;
			double sleepAccuracy = ((double) durationNano) / (ms * M);
			log.trace("Sleep accuracy: " + String.format("%.2f", 100 - (sleepAccuracy * 100 - 100)) + " %");
		}
	}


	@Override
	public synchronized Long getAvailableSince() {
		return availableSince;
	}

	public synchronized boolean isAvailable() {
		return availableSince != null;
	}


}
