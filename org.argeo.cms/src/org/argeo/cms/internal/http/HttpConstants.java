package org.argeo.cms.internal.http;

/** Compatible with Jetty. */
public interface HttpConstants {
	public static final String HTTP_ENABLED = "http.enabled";
	public static final String HTTP_PORT = "http.port";
	public static final String HTTP_HOST = "http.host";
	public static final String HTTPS_ENABLED = "https.enabled";
	public static final String HTTPS_HOST = "https.host";
	public static final String HTTPS_PORT = "https.port";
	public static final String SSL_KEYSTORE = "ssl.keystore";
	public static final String SSL_PASSWORD = "ssl.password";
	public static final String SSL_KEYPASSWORD = "ssl.keypassword";
	public static final String SSL_NEEDCLIENTAUTH = "ssl.needclientauth";
	public static final String SSL_WANTCLIENTAUTH = "ssl.wantclientauth";
	public static final String SSL_PROTOCOL = "ssl.protocol";
	public static final String SSL_ALGORITHM = "ssl.algorithm";
	public static final String SSL_KEYSTORETYPE = "ssl.keystoretype";
	public static final String JETTY_PROPERTY_PREFIX = "org.eclipse.equinox.http.jetty.";

}
