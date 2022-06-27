package org.argeo.cms.internal.runtime;

import org.argeo.api.cms.CmsDeployment;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsState;
import org.osgi.service.http.HttpService;

/** Implementation of a CMS deployment. */
public class CmsDeploymentImpl implements CmsDeployment {
	private final CmsLog log = CmsLog.getLog(getClass());

	// Readiness
	private boolean httpExpected = false;
	private HttpService httpService;

	private CmsState cmsState;
//	private DeployConfig deployConfig;

	public void start() {
//		httpExpected = deployConfig.getProps(KernelConstants.JETTY_FACTORY_PID, "default") != null;
//		if (deployConfig.hasDomain()) {
//			loadIpaJaasConfiguration();
//		}

		log.debug(() -> "CMS deployment available");
	}

//	public void addFactoryDeployConfig(String factoryPid, Dictionary<String, Object> props) {
//		deployConfig.putFactoryDeployConfig(factoryPid, props);
//		deployConfig.save();
//		try {
//			deployConfig.loadConfigs();
//		} catch (IOException e) {
//			throw new IllegalStateException(e);
//		}
//	}
//
//	public Dictionary<String, Object> getProps(String factoryPid, String cn) {
//		return deployConfig.getProps(factoryPid, cn);
//	}

	public boolean isHttpAvailableOrNotExpected() {
		return (httpExpected ? httpService != null : true);
	}

//	private void loadIpaJaasConfiguration() {
//		if (System.getProperty(KernelConstants.JAAS_CONFIG_PROP) == null) {
//			String jaasConfig = KernelConstants.JAAS_CONFIG_IPA;
//			URL url = getClass().getClassLoader().getResource(jaasConfig);
//			KernelUtils.setJaasConfiguration(url);
//			log.debug("Set IPA JAAS configuration.");
//		}
//	}

	public void stop() {
//		if (deployConfig != null) {
//			deployConfig.save();
//		}
	}

//	public void setDeployConfig(DeployConfig deployConfig) {
//		this.deployConfig = deployConfig;
//	}

	public void setCmsState(CmsState cmsState) {
		this.cmsState = cmsState;
		String httpPort = this.cmsState.getDeployProperty("org.osgi.service.http.port");
		String httpsPort = this.cmsState.getDeployProperty("org.osgi.service.http.port.secure");
		httpExpected = httpPort != null || httpsPort != null;
	}

	public void setHttpService(HttpService httpService) {
		this.httpService = httpService;
	}

}
