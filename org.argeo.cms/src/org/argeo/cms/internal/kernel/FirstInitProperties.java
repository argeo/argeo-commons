package org.argeo.cms.internal.kernel;

import static org.argeo.cms.internal.kernel.KernelUtils.getFrameworkProp;

import java.util.Dictionary;
import java.util.Hashtable;

import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.node.NodeConstants;
import org.argeo.node.RepoConf;
import org.eclipse.equinox.http.jetty.JettyConstants;

/**
 * Interprets framework properties in order to generate the initial deploy
 * configuration.
 */
class FirstInitProperties {
	Dictionary<String, Object> getNodeRepositoryConfig() {
		Hashtable<String, Object> props = new Hashtable<String, Object>();
		for (RepoConf repoConf : RepoConf.values()) {
			Object value = getFrameworkProp(NodeConstants.NODE_REPO_PROP_PREFIX + repoConf.name());
			if (value != null)
				props.put(repoConf.name(), value);
		}
		props.put(NodeConstants.CN, ArgeoJcrConstants.ALIAS_NODE);
		props.put(ArgeoJcrConstants.JCR_REPOSITORY_ALIAS, ArgeoJcrConstants.ALIAS_NODE);
		return props;
	}

	Dictionary<String, Object> getHttpServerConfig() {
		String httpPort = getFrameworkProp("org.osgi.service.http.port");
		String httpsPort = getFrameworkProp("org.osgi.service.http.port.secure");
		/// TODO make it more generic
		String httpHost = getFrameworkProp("org.eclipse.equinox.http.jetty.http.host");

		final Hashtable<String, Object> props = new Hashtable<String, Object>();
		// try {
		if (httpPort != null || httpsPort != null) {
			if (httpPort != null) {
				props.put(JettyConstants.HTTP_PORT, httpPort);
				props.put(JettyConstants.HTTP_ENABLED, true);
			}
			if (httpsPort != null) {
				props.put(JettyConstants.HTTPS_PORT, httpsPort);
				props.put(JettyConstants.HTTPS_ENABLED, true);
				props.put(JettyConstants.SSL_KEYSTORETYPE, "PKCS12");
				// jettyProps.put(JettyConstants.SSL_KEYSTORE,
				// nodeSecurity.getHttpServerKeyStore().getCanonicalPath());
				props.put(JettyConstants.SSL_PASSWORD, "changeit");
				props.put(JettyConstants.SSL_WANTCLIENTAUTH, true);
			}
			if (httpHost != null) {
				props.put(JettyConstants.HTTP_HOST, httpHost);
			}
			props.put(NodeConstants.CN, "default");
		}
		return props;
	}
}
