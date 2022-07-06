package org.argeo.cms;

import java.util.Objects;

/** A property that can be used to configure a CMS node deployment. */
public enum CmsDeployProperty {
	//
	// DIRECTORY
	//
	DIRECTORY("argeo.directory", 64),
	//
	// DATABASE
	//
	/** URL of the database backend. */
	DB_URL("argeo.db.url"),
	/** DB user of the database backend. */
	DB_USER("argeo.db.user"),
	/** DB user password of the database backend. */
	DB_PASSWORD("argeo.db.password"),
	//
	// NETWORK
	//
	/** Either a host or an IP address. Restricts all servers to it. */
	HOST("argeo.host"),
	/** Either a host or an IP address. Restricts all servers to it. */
	DNS("argeo.dns", 16),
	//
	// HTTP
	//
	/** Request an HTTP server on this port. */
	HTTP_PORT("argeo.http.port"),
	/** Request an HTTPS server on this port. */
	HTTPS_PORT("argeo.https.port"),
	/**
	 * The HTTP header used to convey the DN of a client verified by a reverse
	 * proxy. Typically SSL_CLIENT_S_DN for Apache.
	 */
	HTTP_PROXY_SSL_HEADER_DN("argeo.http.proxy.ssl.header.dn"),
	//
	// SSL
	//
	/** SSL keystore for the system. */
	SSL_KEYSTORE("argeo.ssl.keystore"),
	/** SSL keystore password for the system. */
	SSL_PASSWORD("argeo.ssl.password"),
	/** SSL keystore type password for the system. */
	SSL_KEYSTORETYPE("argeo.ssl.keystoretype"),
	/** SSL password for the private key. */
	SSL_KEYPASSWORD("argeo.ssl.keypassword"),
	/** Whether a client certificate is required. */
	SSL_NEEDCLIENTAUTH("argeo.ssl.needclientauth"),
	/** Whether a client certificate can be used. */
	SSL_WANTCLIENTAUTH("argeo.ssl.wantclientauth"),
	/** SSL protocol to use. */
	SSL_PROTOCOL("argeo.ssl.protocol"),
	/** SSL algorithm to use. */
	SSL_ALGORITHM("argeo.ssl.algorithm"),
	/** Custom SSL trust store. */
	SSL_TRUSTSTORE("argeo.ssl.truststore"),
	/** Custom SSL trust store type. */
	SSL_TRUSTSTORETYPE("argeo.ssl.truststoretype"),
	/** Custom SSL trust store type. */
	SSL_TRUSTSTOREPASSWORD("argeo.ssl.truststorepassword"),
	//
	// WEBSOCKET
	//
	/** Whether web socket should be enables in web server. */
	WEBSOCKET_ENABLED("argeo.websocket.enabled"),
	//
	// SSH
	//
	/** Request an HTTP server on this port. */
	SSHD_PORT("argeo.sshd.port"),
	//
	// INTERNATIONALIZATION
	//
	/** Locales enabled for this system, the first one is considered the default. */
	LOCALE("argeo.locale", 256),
	//
	// NODE
	//
	/** Directories to copy to the data area during the first initialisation. */
	NODE_INIT("argeo.node.init", 64),
	//
	// JAVA
	//
	/** Custom JAAS config. */
	JAVA_LOGIN_CONFIG("java.security.auth.login.config", true),
	//
	// OSGi
	//
	/** OSGi writable data area. */
	OSGI_INSTANCE_AREA("osgi.instance.area"),
	/** OSGi writable configuration area. */
	OSGI_CONFIGURATION_AREA("osgi.configuration.area"),
	//
	;

	private String property;
	private boolean systemPropertyOnly = false;

	private int maxCount = 1;

	CmsDeployProperty(String property) {
		this(property, 1, false);
	}

	CmsDeployProperty(String property, int maxCount) {
		this(property, maxCount, false);
	}

	CmsDeployProperty(String property, boolean systemPropertyOnly) {
		this.property = property;
	}

	CmsDeployProperty(String property, int maxCount, boolean systemPropertyOnly) {
		this.property = property;
		this.systemPropertyOnly = systemPropertyOnly;
		this.maxCount = maxCount;
	}

	public String getProperty() {
		return property;
	}

	public boolean isSystemPropertyOnly() {
		return systemPropertyOnly;
	}

	public int getMaxCount() {
		return maxCount;
	}

	public static CmsDeployProperty find(String property) {
		int index = getPropertyIndex(property);
		String propertyName = index == 0 ? property : property.substring(0, property.lastIndexOf('.'));
		for (CmsDeployProperty deployProperty : values()) {
			if (deployProperty.getProperty().equals(propertyName))
				return deployProperty;
		}
		return null;
	}

	public static int getPropertyIndex(String property) {
		Objects.requireNonNull(property);
		int lastDot = property.lastIndexOf('.');
		if (lastDot <= 0 || lastDot == (property.length() - 1)) {
			throw new IllegalArgumentException("Property " + property + " is not qualified (must contain a dot).");
		}
		String lastSegment = property.substring(lastDot + 1);
		int index;
		try {
			index = Integer.parseInt(lastSegment);
		} catch (NumberFormatException e) {
			index = 0;
		}
		return index;
	}

}
