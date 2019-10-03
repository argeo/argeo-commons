package org.argeo.cms.internal.http;

/** Compatible with Jetty. */
public interface HttpConstants {
	static final String HTTP_ENABLED = "http.enabled";
	static final String HTTP_PORT = "http.port";
	static final String HTTP_HOST = "http.host";
	static final String HTTPS_ENABLED = "https.enabled";
	static final String HTTPS_HOST = "https.host";
	static final String HTTPS_PORT = "https.port";
	static final String SSL_KEYSTORE = "ssl.keystore";
	static final String SSL_PASSWORD = "ssl.password";
	static final String SSL_KEYPASSWORD = "ssl.keypassword";
	static final String SSL_NEEDCLIENTAUTH = "ssl.needclientauth";
	static final String SSL_WANTCLIENTAUTH = "ssl.wantclientauth";
	static final String SSL_PROTOCOL = "ssl.protocol";
	static final String SSL_ALGORITHM = "ssl.algorithm";
	static final String SSL_KEYSTORETYPE = "ssl.keystoretype";
	static final String JETTY_PROPERTY_PREFIX = "org.eclipse.equinox.http.jetty.";
	// Argeo specific
	static final String WEB_SOCKET_ENABLED = "websocket.enabled";

}
