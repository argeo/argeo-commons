package org.argeo.cms.internal.runtime;

import static org.argeo.api.cms.CmsConstants.CONTEXT_PATH;

import java.util.Map;
import java.util.TreeMap;

import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsDeployment;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsState;
import org.argeo.cms.CmsDeployProperty;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/** Implementation of a CMS deployment. */
public class CmsDeploymentImpl implements CmsDeployment {
	private final CmsLog log = CmsLog.getLog(getClass());

	// Readiness
//	private HttpService httpService;

	private CmsState cmsState;
//	private DeployConfig deployConfig;

	private boolean httpExpected = false;
	private HttpServer httpServer;
	private Map<String, HttpHandler> httpHandlers = new TreeMap<>();

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

//	public boolean isHttpAvailableOrNotExpected() {
//		return (httpExpected ? httpService != null : true);
//	}

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
		String httpPort = this.cmsState.getDeployProperty(CmsDeployProperty.HTTP_PORT.getProperty());
		String httpsPort = this.cmsState.getDeployProperty(CmsDeployProperty.HTTPS_PORT.getProperty());
		httpExpected = httpPort != null || httpsPort != null;
	}

	public void setHttpServer(HttpServer httpServer) {
		this.httpServer = httpServer;
		// create contexts whose handles had already been published
		for (String contextPath : httpHandlers.keySet()) {
			HttpHandler httpHandler = httpHandlers.get(contextPath);
			httpServer.createContext(contextPath, httpHandler);
			log.debug(() -> "Added handler " + contextPath + " : " + httpHandler.getClass().getName());
		}
	}

	public void addHttpHandler(HttpHandler httpHandler, Map<String, String> properties) {
		final String contextPath = properties.get(CONTEXT_PATH);
		if (contextPath == null) {
			log.warn("Property " + CONTEXT_PATH + " not set on HTTP handler " + properties + ". Ignoring it.");
			return;
		}
		httpHandlers.put(contextPath, httpHandler);
		if (httpServer == null)
			return;
		httpServer.createContext(contextPath, httpHandler);
		log.debug(() -> "Added handler " + contextPath + " : " + httpHandler.getClass().getName());

	}

	public void removeHttpHandler(HttpHandler httpHandler, Map<String, String> properties) {
		final String contextPath = properties.get(CmsConstants.CONTEXT_PATH);
		if (contextPath == null)
			return; // ignore silently
		httpHandlers.remove(contextPath);
		if (httpServer == null)
			return;
		httpServer.removeContext(contextPath);
		log.debug(() -> "Removed handler " + contextPath + " : " + httpHandler.getClass().getName());
	}
//	public void setHttpService(HttpService httpService) {
//		this.httpService = httpService;
//	}

}
