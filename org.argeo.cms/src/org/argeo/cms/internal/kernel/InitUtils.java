package org.argeo.cms.internal.kernel;

import static org.argeo.cms.internal.kernel.KernelUtils.getFrameworkProp;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.cms.internal.http.HttpConstants;
import org.argeo.cms.internal.jcr.RepoConf;
import org.argeo.node.NodeConstants;
import org.argeo.osgi.useradmin.UserAdminConf;

/**
 * Interprets framework properties in order to generate the initial deploy
 * configuration.
 */
class InitUtils {
	private final static Log log = LogFactory.getLog(InitUtils.class);

	/** Override the provided config with the framework properties */
	static Dictionary<String, Object> getNodeRepositoryConfig(Dictionary<String, Object> provided) {
		Dictionary<String, Object> props = provided != null ? provided : new Hashtable<String, Object>();
		for (RepoConf repoConf : RepoConf.values()) {
			Object value = getFrameworkProp(NodeConstants.NODE_REPO_PROP_PREFIX + repoConf.name());
			if (value != null)
				props.put(repoConf.name(), value);
		}
		props.put(NodeConstants.CN, NodeConstants.NODE);
		return props;
	}

	static Dictionary<String, Object> getRepositoryConfig(String dataModelName, Dictionary<String, Object> provided) {
		if (dataModelName.equals(NodeConstants.NODE) || dataModelName.equals(NodeConstants.HOME))
			throw new IllegalArgumentException("Data model '" + dataModelName + "' is reserved.");
		Dictionary<String, Object> props = provided != null ? provided : new Hashtable<String, Object>();
		for (RepoConf repoConf : RepoConf.values()) {
			Object value = getFrameworkProp(
					NodeConstants.NODE_REPOS_PROP_PREFIX + dataModelName + '.' + repoConf.name());
			if (value != null)
				props.put(repoConf.name(), value);
		}
		if (props.size() != 0)
			props.put(NodeConstants.CN, dataModelName);
		return props;
	}

	/** Override the provided config with the framework properties */
	static Dictionary<String, Object> getHttpServerConfig(Dictionary<String, Object> provided) {
		String httpPort = getFrameworkProp("org.osgi.service.http.port");
		String httpsPort = getFrameworkProp("org.osgi.service.http.port.secure");
		/// TODO make it more generic
		String httpHost = getFrameworkProp(HttpConstants.JETTY_PROPERTY_PREFIX + HttpConstants.HTTP_HOST);
		String httpsHost = getFrameworkProp(HttpConstants.JETTY_PROPERTY_PREFIX + HttpConstants.HTTPS_HOST);
		String webSocketEnabled = getFrameworkProp(
				HttpConstants.JETTY_PROPERTY_PREFIX + HttpConstants.WEB_SOCKET_ENABLED);

		final Hashtable<String, Object> props = new Hashtable<String, Object>();
		// try {
		if (httpPort != null || httpsPort != null) {
			boolean httpEnabled = httpPort != null;
			props.put(HttpConstants.HTTP_ENABLED, httpEnabled);
			boolean httpsEnabled = httpsPort != null;
			props.put(HttpConstants.HTTPS_ENABLED, httpsEnabled);

			if (httpEnabled) {
				props.put(HttpConstants.HTTP_PORT, httpPort);
				if (httpHost != null)
					props.put(HttpConstants.HTTP_HOST, httpHost);
			}

			if (httpsEnabled) {
				props.put(HttpConstants.HTTPS_PORT, httpsPort);
				if (httpsHost != null)
					props.put(HttpConstants.HTTPS_HOST, httpsHost);

				// server certificate
				Path keyStorePath = KernelUtils.getOsgiInstancePath(KernelConstants.DEFAULT_KEYSTORE_PATH);
				String keyStorePassword = getFrameworkProp(
						HttpConstants.JETTY_PROPERTY_PREFIX + HttpConstants.SSL_PASSWORD);
				if (keyStorePassword == null)
					keyStorePassword = "changeit";
				if (!Files.exists(keyStorePath))
					createSelfSignedKeyStore(keyStorePath, keyStorePassword, PkiUtils.PKCS12);
				props.put(HttpConstants.SSL_KEYSTORETYPE, PkiUtils.PKCS12);
				props.put(HttpConstants.SSL_KEYSTORE, keyStorePath.toString());
				props.put(HttpConstants.SSL_PASSWORD, keyStorePassword);

				// client certificate authentication
				String wantClientAuth = getFrameworkProp(
						HttpConstants.JETTY_PROPERTY_PREFIX + HttpConstants.SSL_WANTCLIENTAUTH);
				if (wantClientAuth != null)
					props.put(HttpConstants.SSL_NEEDCLIENTAUTH, Boolean.parseBoolean(wantClientAuth));
				String needClientAuth = getFrameworkProp(
						HttpConstants.JETTY_PROPERTY_PREFIX + HttpConstants.SSL_NEEDCLIENTAUTH);
				if (needClientAuth != null)
					props.put(HttpConstants.SSL_NEEDCLIENTAUTH, Boolean.parseBoolean(needClientAuth));
			}

			// web socket
			if (webSocketEnabled != null && webSocketEnabled.equals("true"))
				props.put(HttpConstants.WEB_SOCKET_ENABLED, true);

			props.put(NodeConstants.CN, NodeConstants.DEFAULT);
		}
		return props;
	}

	static List<Dictionary<String, Object>> getUserDirectoryConfigs() {
		List<Dictionary<String, Object>> res = new ArrayList<>();
		File nodeBaseDir = KernelUtils.getOsgiInstancePath(KernelConstants.DIR_NODE).toFile();
		List<String> uris = new ArrayList<>();

		// node roles
		String nodeRolesUri = getFrameworkProp(NodeConstants.ROLES_URI);
		String baseNodeRoleDn = NodeConstants.ROLES_BASEDN;
		if (nodeRolesUri == null) {
			nodeRolesUri = baseNodeRoleDn + ".ldif";
			File nodeRolesFile = new File(nodeBaseDir, nodeRolesUri);
			if (!nodeRolesFile.exists())
				try {
					FileUtils.copyInputStreamToFile(InitUtils.class.getResourceAsStream(baseNodeRoleDn + ".ldif"),
							nodeRolesFile);
				} catch (IOException e) {
					throw new CmsException("Cannot copy demo resource", e);
				}
			// nodeRolesUri = nodeRolesFile.toURI().toString();
		}
		uris.add(nodeRolesUri);

		// node tokens
		String nodeTokensUri = getFrameworkProp(NodeConstants.TOKENS_URI);
		String baseNodeTokensDn = NodeConstants.TOKENS_BASEDN;
		if (nodeTokensUri == null) {
			nodeTokensUri = baseNodeTokensDn + ".ldif";
			File nodeRolesFile = new File(nodeBaseDir, nodeRolesUri);
			if (!nodeRolesFile.exists())
				try {
					FileUtils.copyInputStreamToFile(InitUtils.class.getResourceAsStream(baseNodeTokensDn + ".ldif"),
							nodeRolesFile);
				} catch (IOException e) {
					throw new CmsException("Cannot copy demo resource", e);
				}
			// nodeRolesUri = nodeRolesFile.toURI().toString();
		}
		uris.add(nodeTokensUri);

		// Business roles
		String userAdminUris = getFrameworkProp(NodeConstants.USERADMIN_URIS);
		if (userAdminUris == null) {
			String demoBaseDn = "dc=example,dc=com";
			userAdminUris = demoBaseDn + ".ldif";
			File businessRolesFile = new File(nodeBaseDir, userAdminUris);
			File systemRolesFile = new File(nodeBaseDir, "ou=roles,ou=node.ldif");
			if (!businessRolesFile.exists())
				try {
					FileUtils.copyInputStreamToFile(InitUtils.class.getResourceAsStream(demoBaseDn + ".ldif"),
							businessRolesFile);
					if (!systemRolesFile.exists())
						FileUtils.copyInputStreamToFile(
								InitUtils.class.getResourceAsStream("example-ou=roles,ou=node.ldif"), systemRolesFile);
				} catch (IOException e) {
					throw new CmsException("Cannot copy demo resources", e);
				}
			// userAdminUris = businessRolesFile.toURI().toString();
			log.warn("## DEV Using dummy base DN " + demoBaseDn);
			// TODO downgrade security level
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
						// u = KernelUtils.getOsgiInstanceUri(KernelConstants.DIR_NODE + '/' + uri);
						u = new URI(uri);
					} else
						throw new CmsException("Cannot interpret " + uri + " as an uri");
				} else if (u.getScheme().equals(UserAdminConf.SCHEME_FILE)) {
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
	 * Called before node initialisation, in order populate OSGi instance are with
	 * some files (typically LDIF, etc).
	 */
	static void prepareFirstInitInstanceArea() {
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

	private static void createSelfSignedKeyStore(Path keyStorePath, String keyStorePassword, String keyStoreType) {
		// for (Provider provider : Security.getProviders())
		// System.out.println(provider.getName());
		File keyStoreFile = keyStorePath.toFile();
		char[] ksPwd = keyStorePassword.toCharArray();
		char[] keyPwd = Arrays.copyOf(ksPwd, ksPwd.length);
		if (!keyStoreFile.exists()) {
			try {
				keyStoreFile.getParentFile().mkdirs();
				KeyStore keyStore = PkiUtils.getKeyStore(keyStoreFile, ksPwd, keyStoreType);
				PkiUtils.generateSelfSignedCertificate(keyStore,
						new X500Principal("CN=" + InetAddress.getLocalHost().getHostName() + ",OU=UNSECURE,O=UNSECURE"),
						1024, keyPwd);
				PkiUtils.saveKeyStore(keyStoreFile, ksPwd, keyStore);
				if (log.isDebugEnabled())
					log.debug("Created self-signed unsecure keystore " + keyStoreFile);
			} catch (Exception e) {
				if (keyStoreFile.length() == 0)
					keyStoreFile.delete();
				log.error("Cannot create keystore " + keyStoreFile, e);
			}
		} else {
			throw new CmsException("Keystore " + keyStorePath + " already exists");
		}
	}

}
