package org.argeo.equinox.jetty;

import java.util.Dictionary;

import javax.servlet.ServletContext;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;

import org.eclipse.equinox.http.jetty.JettyCustomizer;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer;
import org.eclipse.jetty.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer.Configurator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/** Customises the Jetty HTTP server. */
public class CmsJettyCustomizer extends JettyCustomizer {
	static final String SSL_TRUSTSTORE = "ssl.truststore";
	static final String SSL_TRUSTSTOREPASSWORD = "ssl.truststorepassword";
	static final String SSL_TRUSTSTORETYPE = "ssl.truststoretype";

	private BundleContext bc = FrameworkUtil.getBundle(CmsJettyCustomizer.class).getBundleContext();

	public final static String WEBSOCKET_ENABLED = "argeo.websocket.enabled";

	@Override
	public Object customizeContext(Object context, Dictionary<String, ?> settings) {
		// WebSocket
		Object webSocketEnabled = settings.get(WEBSOCKET_ENABLED);
		if (webSocketEnabled != null && webSocketEnabled.toString().equals("true")) {
			ServletContextHandler servletContextHandler = (ServletContextHandler) context;
			JavaxWebSocketServletContainerInitializer.configure(servletContextHandler, new Configurator() {

				@Override
				public void accept(ServletContext servletContext, ServerContainer serverContainer)
						throws DeploymentException {
					bc.registerService(javax.websocket.server.ServerContainer.class, serverContainer, null);
				}
			});
		}
		return super.customizeContext(context, settings);

	}

	@Override
	public Object customizeHttpsConnector(Object connector, Dictionary<String, ?> settings) {
		ServerConnector httpsConnector = (ServerConnector) connector;
		if (httpsConnector != null)
			for (ConnectionFactory connectionFactory : httpsConnector.getConnectionFactories()) {
				if (connectionFactory instanceof SslConnectionFactory) {
					SslContextFactory.Server sslConnectionFactory = ((SslConnectionFactory) connectionFactory)
							.getSslContextFactory();
					sslConnectionFactory.setTrustStorePath((String) settings.get(SSL_TRUSTSTORE));
					sslConnectionFactory.setTrustStoreType((String) settings.get(SSL_TRUSTSTORETYPE));
					sslConnectionFactory.setTrustStorePassword((String) settings.get(SSL_TRUSTSTOREPASSWORD));
				}
			}
		return super.customizeHttpsConnector(connector, settings);
	}

}
