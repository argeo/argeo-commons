package org.argeo.cms.jetty;

import java.util.Objects;

import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsState;
import org.argeo.cms.CmsDeployProperty;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import com.sun.net.httpserver.HttpServer;

/**
 * An {@link HttpServer} implementation based on Jetty and integrated in a
 * running Argeo CMS.
 */
public class CmsJettyServer extends JettyHttpServer {
	private final static CmsLog log = CmsLog.getLog(CmsJettyServer.class);

	private CmsState cmsState;

	@Override
	public void start() {
		Objects.requireNonNull(cmsState, "No CMS state is configured");
		String httpPortArg = getDeployProperty(CmsDeployProperty.HTTP_PORT);
		String httpsPortArg = getDeployProperty(CmsDeployProperty.HTTPS_PORT);
		if (httpPortArg != null && httpsPortArg != null)
			throw new IllegalArgumentException("Either an HTTP or an HTTPS port should be configured, not both");
		if (httpPortArg == null && httpsPortArg == null) {
			log.warn("Neither an HTTP or an HTTPS port was configured, not starting Jetty");
		}

		/// TODO make it more generic
		String httpHostArg = getDeployProperty(CmsDeployProperty.HOST);

		setHttpPortArg(httpPortArg);
		setHttpsPortArg(httpsPortArg);
		setHttpHostArg(httpHostArg);

		super.start();
	}

	@Override
	protected SslContextFactory.Server newSslContextFactory() {
		SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
		// sslContextFactory.setKeyStore(KeyS)

		// FIXME check values and warn/fail when mandatory ones are not set
		sslContextFactory.setKeyStoreType(getDeployProperty(CmsDeployProperty.SSL_KEYSTORETYPE));
		sslContextFactory.setKeyStorePath(getDeployProperty(CmsDeployProperty.SSL_KEYSTORE));
		sslContextFactory.setKeyStorePassword(getDeployProperty(CmsDeployProperty.SSL_PASSWORD));
		// sslContextFactory.setKeyManagerPassword(getFrameworkProp(CmsDeployProperty.SSL_KEYPASSWORD));
		sslContextFactory.setProtocol("TLS");

		sslContextFactory.setTrustStoreType(getDeployProperty(CmsDeployProperty.SSL_TRUSTSTORETYPE));
		sslContextFactory.setTrustStorePath(getDeployProperty(CmsDeployProperty.SSL_TRUSTSTORE));
		sslContextFactory.setTrustStorePassword(getDeployProperty(CmsDeployProperty.SSL_TRUSTSTOREPASSWORD));

		String wantClientAuth = getDeployProperty(CmsDeployProperty.SSL_WANTCLIENTAUTH);
		if (wantClientAuth != null && wantClientAuth.equals(Boolean.toString(true)))
			sslContextFactory.setWantClientAuth(true);
		String needClientAuth = getDeployProperty(CmsDeployProperty.SSL_NEEDCLIENTAUTH);
		if (needClientAuth != null && needClientAuth.equals(Boolean.toString(true)))
			sslContextFactory.setNeedClientAuth(true);
		return sslContextFactory;
	}

	@Override
	protected String getFallbackHostname() {
		String fallBackHostname = cmsState != null ? cmsState.getHostname() : "::1";
		return fallBackHostname;
	}

	protected String getDeployProperty(CmsDeployProperty deployProperty) {
		return cmsState != null ? cmsState.getDeployProperty(deployProperty.getProperty())
				: System.getProperty(deployProperty.getProperty());
	}

	public void setCmsState(CmsState cmsState) {
		this.cmsState = cmsState;
	}

}
