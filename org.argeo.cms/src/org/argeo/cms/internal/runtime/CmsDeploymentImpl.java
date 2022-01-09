package org.argeo.cms.internal.runtime;

import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;

import org.argeo.api.cms.CmsDeployment;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsState;
import org.argeo.cms.internal.osgi.DeployConfig;
import org.eclipse.equinox.http.jetty.JettyConfigurator;
import org.osgi.service.http.HttpService;

/** Implementation of a CMS deployment. */
public class CmsDeploymentImpl implements CmsDeployment {
	private final CmsLog log = CmsLog.getLog(getClass());
//	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

//	private Long availableSince;

	// Readiness
//	private boolean nodeAvailable = false;
//	private boolean userAdminAvailable = false;
	private boolean httpExpected = false;
//	private boolean httpAvailable = false;
	private HttpService httpService;

	private CmsState cmsState;
	private DeployConfig deployConfig;

	public CmsDeploymentImpl() {
//		ServiceReference<NodeState> nodeStateSr = bc.getServiceReference(NodeState.class);
//		if (nodeStateSr == null)
//			throw new CmsException("No node state available");

//		NodeState nodeState = bc.getService(nodeStateSr);
//		cleanState = nodeState.isClean();

//		nodeHttp = new NodeHttp();
		initTrackers();
	}

	private void initTrackers() {
//		ServiceTracker<?, ?> httpSt = new ServiceTracker<HttpService, HttpService>(bc, HttpService.class, null) {
//
//			@Override
//			public HttpService addingService(ServiceReference<HttpService> sr) {
//				httpAvailable = true;
//				Object httpPort = sr.getProperty("http.port");
//				Object httpsPort = sr.getProperty("https.port");
//				log.info(httpPortsMsg(httpPort, httpsPort));
//				checkReadiness();
//				return super.addingService(sr);
//			}
//		};
//		// httpSt.open();
//		KernelUtils.asyncOpen(httpSt);

//		ServiceTracker<?, ?> userAdminSt = new ServiceTracker<UserAdmin, UserAdmin>(bc, UserAdmin.class, null) {
//			@Override
//			public UserAdmin addingService(ServiceReference<UserAdmin> reference) {
//				UserAdmin userAdmin = super.addingService(reference);
//				addStandardSystemRoles(userAdmin);
//				userAdminAvailable = true;
//				checkReadiness();
//				return userAdmin;
//			}
//		};
//		// userAdminSt.open();
//		KernelUtils.asyncOpen(userAdminSt);

//		ServiceTracker<?, ?> confAdminSt = new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(bc,
//				ConfigurationAdmin.class, null) {
//			@Override
//			public ConfigurationAdmin addingService(ServiceReference<ConfigurationAdmin> reference) {
//				ConfigurationAdmin configurationAdmin = bc.getService(reference);
////				boolean isClean;
////				try {
////					Configuration[] confs = configurationAdmin
////							.listConfigurations("(service.factoryPid=" + CmsConstants.NODE_USER_ADMIN_PID + ")");
////					isClean = confs == null || confs.length == 0;
////				} catch (Exception e) {
////					throw new IllegalStateException("Cannot analyse clean state", e);
////				}
//				deployConfig = new DeployConfig(configurationAdmin, isClean);
//				Activator.registerService(CmsDeployment.class, CmsDeploymentImpl.this, null);
////				JcrInitUtils.addToDeployment(CmsDeployment.this);
//				httpExpected = deployConfig.getProps(KernelConstants.JETTY_FACTORY_PID, "default") != null;
//				try {
//					Configuration[] configs = configurationAdmin
//							.listConfigurations("(service.factoryPid=" + CmsConstants.NODE_USER_ADMIN_PID + ")");
//
//					boolean hasDomain = false;
//					for (Configuration config : configs) {
//						Object realm = config.getProperties().get(UserAdminConf.realm.name());
//						if (realm != null) {
//							log.debug("Found realm: " + realm);
//							hasDomain = true;
//						}
//					}
//					if (hasDomain) {
//						loadIpaJaasConfiguration();
//					}
//				} catch (Exception e) {
//					throw new IllegalStateException("Cannot initialize config", e);
//				}
//				return super.addingService(reference);
//			}
//		};
//		// confAdminSt.open();
//		KernelUtils.asyncOpen(confAdminSt);
	}

	public void init() {
		httpExpected = deployConfig.getProps(KernelConstants.JETTY_FACTORY_PID, "default") != null;
		if (deployConfig.hasDomain()) {
			loadIpaJaasConfiguration();
		}

//		while (!isHttpAvailableOrNotExpected()) {
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				log.error("Interrupted while waiting for http");
//			}
//		}
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

//	private void addStandardSystemRoles(UserAdmin userAdmin) {
//		// we assume UserTransaction is already available (TODO make it more robust)
//		WorkTransaction userTransaction = bc.getService(bc.getServiceReference(WorkTransaction.class));
//		try {
//			userTransaction.begin();
//			Role adminRole = userAdmin.getRole(CmsConstants.ROLE_ADMIN);
//			if (adminRole == null) {
//				adminRole = userAdmin.createRole(CmsConstants.ROLE_ADMIN, Role.GROUP);
//			}
//			if (userAdmin.getRole(CmsConstants.ROLE_USER_ADMIN) == null) {
//				Group userAdminRole = (Group) userAdmin.createRole(CmsConstants.ROLE_USER_ADMIN, Role.GROUP);
//				userAdminRole.addMember(adminRole);
//			}
//			userTransaction.commit();
//		} catch (Exception e) {
//			try {
//				userTransaction.rollback();
//			} catch (Exception e1) {
//				// silent
//			}
//			throw new IllegalStateException("Cannot add standard system roles", e);
//		}
//	}

	public boolean isHttpAvailableOrNotExpected() {
		return (httpExpected ? httpService != null : true);
	}

	private void loadIpaJaasConfiguration() {
		if (System.getProperty(KernelConstants.JAAS_CONFIG_PROP) == null) {
			String jaasConfig = KernelConstants.JAAS_CONFIG_IPA;
			URL url = getClass().getClassLoader().getResource(jaasConfig);
			KernelUtils.setJaasConfiguration(url);
			log.debug("Set IPA JAAS configuration.");
		}
	}

	public void destroy() {
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

	public void setDeployConfig(DeployConfig deployConfig) {
		this.deployConfig = deployConfig;
	}

	public void setCmsState(CmsState cmsState) {
		this.cmsState = cmsState;
	}

	public void setHttpService(HttpService httpService) {
		this.httpService = httpService;
	}

}
