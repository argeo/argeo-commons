package org.argeo.cms.internal.kernel;

import static org.argeo.cms.internal.kernel.KernelUtils.getFrameworkProp;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.api.NodeConstants;
import org.argeo.cms.internal.http.InternalHttpConstants;
import org.argeo.cms.internal.jcr.RepoConf;
import org.argeo.jackrabbit.client.ClientDavexRepositoryFactory;
import org.argeo.jcr.JcrException;
import org.argeo.naming.LdapAttrs;
import org.argeo.osgi.useradmin.UserAdminConf;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

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
			if (value != null) {
				props.put(repoConf.name(), value);
				if (log.isDebugEnabled())
					log.debug("Set node repo configuration " + repoConf.name() + " to " + value);
			}
		}
		props.put(NodeConstants.CN, NodeConstants.NODE_REPOSITORY);
		return props;
	}

	static Dictionary<String, Object> getRepositoryConfig(String dataModelName, Dictionary<String, Object> provided) {
		if (dataModelName.equals(NodeConstants.NODE_REPOSITORY) || dataModelName.equals(NodeConstants.EGO_REPOSITORY))
			throw new IllegalArgumentException("Data model '" + dataModelName + "' is reserved.");
		Dictionary<String, Object> props = provided != null ? provided : new Hashtable<String, Object>();
		for (RepoConf repoConf : RepoConf.values()) {
			Object value = getFrameworkProp(
					NodeConstants.NODE_REPOS_PROP_PREFIX + dataModelName + '.' + repoConf.name());
			if (value != null) {
				props.put(repoConf.name(), value);
				if (log.isDebugEnabled())
					log.debug("Set " + dataModelName + " repo configuration " + repoConf.name() + " to " + value);
			}
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
		String httpHost = getFrameworkProp(
				InternalHttpConstants.JETTY_PROPERTY_PREFIX + InternalHttpConstants.HTTP_HOST);
		String httpsHost = getFrameworkProp(
				InternalHttpConstants.JETTY_PROPERTY_PREFIX + InternalHttpConstants.HTTPS_HOST);
		String webSocketEnabled = getFrameworkProp(
				InternalHttpConstants.JETTY_PROPERTY_PREFIX + InternalHttpConstants.WEBSOCKET_ENABLED);

		final Hashtable<String, Object> props = new Hashtable<String, Object>();
		// try {
		if (httpPort != null || httpsPort != null) {
			boolean httpEnabled = httpPort != null;
			props.put(InternalHttpConstants.HTTP_ENABLED, httpEnabled);
			boolean httpsEnabled = httpsPort != null;
			props.put(InternalHttpConstants.HTTPS_ENABLED, httpsEnabled);

			if (httpEnabled) {
				props.put(InternalHttpConstants.HTTP_PORT, httpPort);
				if (httpHost != null)
					props.put(InternalHttpConstants.HTTP_HOST, httpHost);
			}

			if (httpsEnabled) {
				props.put(InternalHttpConstants.HTTPS_PORT, httpsPort);
				if (httpsHost != null)
					props.put(InternalHttpConstants.HTTPS_HOST, httpsHost);

				// server certificate
				Path keyStorePath = KernelUtils.getOsgiInstancePath(KernelConstants.DEFAULT_KEYSTORE_PATH);
				String keyStorePassword = getFrameworkProp(
						InternalHttpConstants.JETTY_PROPERTY_PREFIX + InternalHttpConstants.SSL_PASSWORD);
				if (keyStorePassword == null)
					keyStorePassword = "changeit";
				if (!Files.exists(keyStorePath))
					createSelfSignedKeyStore(keyStorePath, keyStorePassword, PkiUtils.PKCS12);
				props.put(InternalHttpConstants.SSL_KEYSTORETYPE, PkiUtils.PKCS12);
				props.put(InternalHttpConstants.SSL_KEYSTORE, keyStorePath.toString());
				props.put(InternalHttpConstants.SSL_PASSWORD, keyStorePassword);

				// client certificate authentication
				String wantClientAuth = getFrameworkProp(
						InternalHttpConstants.JETTY_PROPERTY_PREFIX + InternalHttpConstants.SSL_WANTCLIENTAUTH);
				if (wantClientAuth != null)
					props.put(InternalHttpConstants.SSL_WANTCLIENTAUTH, Boolean.parseBoolean(wantClientAuth));
				String needClientAuth = getFrameworkProp(
						InternalHttpConstants.JETTY_PROPERTY_PREFIX + InternalHttpConstants.SSL_NEEDCLIENTAUTH);
				if (needClientAuth != null)
					props.put(InternalHttpConstants.SSL_NEEDCLIENTAUTH, Boolean.parseBoolean(needClientAuth));
			}

			// web socket
			if (webSocketEnabled != null && webSocketEnabled.equals("true"))
				props.put(InternalHttpConstants.WEBSOCKET_ENABLED, true);

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
					throw new RuntimeException("Cannot copy demo resource", e);
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
					throw new RuntimeException("Cannot copy demo resource", e);
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
					throw new RuntimeException("Cannot copy demo resources", e);
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
					throw new IllegalArgumentException(
							"URI " + uri + " must have a path in order to determine base DN");
				if (u.getScheme() == null) {
					if (uri.startsWith("/") || uri.startsWith("./") || uri.startsWith("../"))
						u = new File(uri).getCanonicalFile().toURI();
					else if (!uri.contains("/")) {
						// u = KernelUtils.getOsgiInstanceUri(KernelConstants.DIR_NODE + '/' + uri);
						u = new URI(uri);
					} else
						throw new IllegalArgumentException("Cannot interpret " + uri + " as an uri");
				} else if (u.getScheme().equals(UserAdminConf.SCHEME_FILE)) {
					u = new File(u).getCanonicalFile().toURI();
				}
			} catch (Exception e) {
				throw new RuntimeException("Cannot interpret " + uri + " as an uri", e);
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
		String nodeInits = getFrameworkProp(NodeConstants.NODE_INIT);
		if (nodeInits == null)
			nodeInits = "../../init";

		for (String nodeInit : nodeInits.split(",")) {

			if (nodeInit.startsWith("http")) {
				registerRemoteInit(nodeInit);
			} else {

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
						throw new RuntimeException("Cannot initialize from " + initDir, e);
					}
			}
		}
	}

	private static void registerRemoteInit(String uri) {
		try {
			BundleContext bundleContext = KernelUtils.getBundleContext();
			Repository repository = createRemoteRepository(new URI(uri));
			Hashtable<String, Object> properties = new Hashtable<>();
			properties.put(NodeConstants.CN, NodeConstants.NODE_INIT);
			properties.put(LdapAttrs.labeledURI.name(), uri);
			properties.put(Constants.SERVICE_RANKING, -1000);
			bundleContext.registerService(Repository.class, repository, properties);
		} catch (RepositoryException e) {
			throw new JcrException(e);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private static Repository createRemoteRepository(URI uri) throws RepositoryException {
		RepositoryFactory repositoryFactory = new ClientDavexRepositoryFactory();
		Map<String, String> params = new HashMap<String, String>();
		params.put(ClientDavexRepositoryFactory.JACKRABBIT_DAVEX_URI, uri.toString());
		// TODO make it configurable
		params.put(ClientDavexRepositoryFactory.JACKRABBIT_REMOTE_DEFAULT_WORKSPACE, NodeConstants.SYS_WORKSPACE);
		return repositoryFactory.getRepository(params);
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
			throw new IllegalStateException("Keystore " + keyStorePath + " already exists");
		}
	}

}
