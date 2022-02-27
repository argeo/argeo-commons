package org.argeo.cms.servlet.internal.jetty;

import java.util.Dictionary;

import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsLog;
import org.eclipse.equinox.http.jetty.JettyConfigurator;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class JettyServiceFactory implements ManagedServiceFactory {
	private final CmsLog log = CmsLog.getLog(JettyServiceFactory.class);

	public void start() {

	}

	@Override
	public String getName() {
		return "Jetty Service Factory";
	}

	@Override
	public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
		// Explicitly configures Jetty so that the default server is not started by the
		// activator of the Equinox Jetty bundle.

//		if (!webServerConfig.isEmpty()) {
//		webServerConfig.put("customizer.class", KernelConstants.CMS_JETTY_CUSTOMIZER_CLASS);
//
//		// TODO centralise with Jetty extender
//		Object webSocketEnabled = webServerConfig.get(InternalHttpConstants.WEBSOCKET_ENABLED);
//		if (webSocketEnabled != null && webSocketEnabled.toString().equals("true")) {
//			bc.registerService(ServerEndpointConfig.Configurator.class, new CmsWebSocketConfigurator(), null);
//			webServerConfig.put(InternalHttpConstants.WEBSOCKET_ENABLED, "true");
//		}
//	}

		int tryCount = 60;
		try {
			tryGettyJetty: while (tryCount > 0) {
				try {
					// FIXME deal with multiple ids
					JettyConfigurator.startServer(CmsConstants.DEFAULT, properties);
					// Explicitly starts Jetty OSGi HTTP bundle, so that it gets triggered if OSGi
					// configuration is not cleaned
					FrameworkUtil.getBundle(JettyConfigurator.class).start();
					break tryGettyJetty;
				} catch (IllegalStateException e) {
					// Jetty may not be ready
					try {
						Thread.sleep(1000);
					} catch (Exception e1) {
						// silent
					}
					tryCount--;
				}
			}
		} catch (Exception e) {
			log.error("Cannot start default Jetty server with config " + properties, e);
		}

	}

	@Override
	public void deleted(String pid) {
	}

	public void stop() {
		try {
			JettyConfigurator.stopServer(CmsConstants.DEFAULT);
		} catch (Exception e) {
			log.error("Cannot stop default Jetty server.", e);
		}

	}

}
