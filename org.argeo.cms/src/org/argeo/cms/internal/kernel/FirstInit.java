package org.argeo.cms.internal.kernel;

import static org.argeo.cms.internal.kernel.KernelUtils.getFrameworkProp;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.node.NodeConstants;
import org.argeo.osgi.useradmin.UserAdminConf;
import org.eclipse.equinox.http.jetty.JettyConstants;

/**
 * Interprets framework properties in order to generate the initial deploy
 * configuration.
 */
class FirstInit {
	private final static Log log = LogFactory.getLog(FirstInit.class);

	public FirstInit() {
		log.info("## FIRST INIT ##");
	}

	/** Override the provided config with the framework properties */
	Dictionary<String, Object> getNodeRepositoryConfig(Dictionary<String, Object> provided) {
		Dictionary<String, Object> props = provided != null ? provided : new Hashtable<String, Object>();
		for (RepoConf repoConf : RepoConf.values()) {
			Object value = getFrameworkProp(NodeConstants.NODE_REPO_PROP_PREFIX + repoConf.name());
			if (value != null)
				props.put(repoConf.name(), value);
		}
		props.put(NodeConstants.CN, NodeConstants.NODE);
		// props.put(NodeConstants.JCR_REPOSITORY_ALIAS, NodeConstants.NODE);
		return props;
	}

	/** Override the provided config with the framework properties */
	Dictionary<String, Object> getHttpServerConfig(Dictionary<String, Object> provided) {
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
				props.put(JettyConstants.SSL_KEYSTORE, "../../ssl/server.p12");
				// jettyProps.put(JettyConstants.SSL_KEYSTORE,
				// nodeSecurity.getHttpServerKeyStore().getCanonicalPath());
				props.put(JettyConstants.SSL_PASSWORD, "changeit");
				props.put(JettyConstants.SSL_WANTCLIENTAUTH, true);
			}
			if (httpHost != null) {
				props.put(JettyConstants.HTTP_HOST, httpHost);
			}
			props.put(NodeConstants.CN, NodeConstants.DEFAULT);
		}
		return props;
	}

	List<Dictionary<String, Object>> getUserDirectoryConfigs() {
		List<Dictionary<String, Object>> res = new ArrayList<>();
		File nodeBaseDir = KernelUtils.getOsgiInstancePath(KernelConstants.DIR_NODE).toFile();
		List<String> uris = new ArrayList<>();

		// node roles
		String nodeRolesUri = getFrameworkProp(NodeConstants.ROLES_URI);
		String baseNodeRoleDn = NodeConstants.ROLES_BASEDN;
		if (nodeRolesUri == null) {
			File nodeRolesFile = new File(nodeBaseDir, baseNodeRoleDn + ".ldif");
			if (!nodeRolesFile.exists())
				try {
					FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(baseNodeRoleDn + ".ldif"),
							nodeRolesFile);
				} catch (IOException e) {
					throw new CmsException("Cannot copy demo resource", e);
				}
			nodeRolesUri = nodeRolesFile.toURI().toString();
		}
		uris.add(nodeRolesUri);

		// Business roles
		String userAdminUris = getFrameworkProp(NodeConstants.USERADMIN_URIS);
		if (userAdminUris == null) {
			String kerberosDomain = Activator.getCmsSecurity().getKerberosDomain();
			if (kerberosDomain != null) {
				userAdminUris = "ipa:///" + kerberosDomain;
			} else {
				String demoBaseDn = "dc=example,dc=com";
				File businessRolesFile = new File(nodeBaseDir, demoBaseDn + ".ldif");
				if (!businessRolesFile.exists())
					try {
						FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(demoBaseDn + ".ldif"),
								businessRolesFile);
					} catch (IOException e) {
						throw new CmsException("Cannot copy demo resource", e);
					}
				userAdminUris = businessRolesFile.toURI().toString();
				log.warn("## DEV Using dummy base DN " + demoBaseDn);
				// TODO downgrade security level
			}
		}
		for (String userAdminUri : userAdminUris.split(" "))
			uris.add(userAdminUri);

		// Interprets URIs
		for (String uri : uris) {
			URI u;
			try {
				u = new URI(uri);
				if (u.getPath() == null)
					throw new CmsException("URI " + uri + " must have a path in order to determine base DN");
				if (u.getScheme() == null) {
					if (uri.startsWith("/") || uri.startsWith("./") || uri.startsWith("../"))
						u = new File(uri).getCanonicalFile().toURI();
					else if (!uri.contains("/")) {
						u = KernelUtils.getOsgiInstanceUri(KernelConstants.DIR_NODE + '/' + uri);
						// u = new URI(nodeBaseDir.toURI() + uri);
					} else
						throw new CmsException("Cannot interpret " + uri + " as an uri");
				} else if (u.getScheme().equals("file")) {
					u = new File(u).getCanonicalFile().toURI();
				}
			} catch (Exception e) {
				throw new CmsException("Cannot interpret " + uri + " as an uri", e);
			}
			Dictionary<String, Object> properties = UserAdminConf.uriAsProperties(u.toString());
			res.add(properties);
		}

		return res;
	}

	/**
	 * Called before node initialisation, in order populate OSGi instance are
	 * with some files (typically LDIF, etc).
	 */
	static void prepareInstanceArea() {
		String nodeInit = getFrameworkProp(NodeConstants.NODE_INIT);
		if (nodeInit == null)
			nodeInit = "../../init";
		if (nodeInit.startsWith("http")) {
			// remoteFirstInit(nodeInit);
			return;
		}

		// TODO use java.nio.file
		File initDir;
		if (nodeInit.startsWith("."))
			initDir = KernelUtils.getExecutionDir(nodeInit);
		else
			initDir = new File(nodeInit);
		// TODO also uncompress archives
		if (initDir.exists())
			try {
				FileUtils.copyDirectory(initDir, KernelUtils.getOsgiInstanceDir(), new FileFilter() {

					@Override
					public boolean accept(File pathname) {
						if (pathname.getName().equals(".svn") || pathname.getName().equals(".git"))
							return false;
						return true;
					}
				});
				log.info("CMS initialized from " + initDir.getCanonicalPath());
			} catch (IOException e) {
				throw new CmsException("Cannot initialize from " + initDir, e);
			}
	}

}
