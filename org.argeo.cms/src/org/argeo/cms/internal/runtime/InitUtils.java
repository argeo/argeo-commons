package org.argeo.cms.internal.runtime;

import static org.argeo.cms.internal.runtime.KernelUtils.getFrameworkProp;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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
import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.internal.http.InternalHttpConstants;
import org.argeo.osgi.useradmin.UserAdminConf;

/**
 * Interprets framework properties in order to generate the initial deploy
 * configuration.
 */
public class InitUtils {
	private final static CmsLog log = CmsLog.getLog(InitUtils.class);

	/** Override the provided config with the framework properties */
	public static Dictionary<String, Object> getHttpServerConfig(Dictionary<String, Object> provided) {
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
				Path pemKeyPath = KernelUtils.getOsgiInstancePath(KernelConstants.DEFAULT_PEM_KEY_PATH);
				Path pemCertPath = KernelUtils.getOsgiInstancePath(KernelConstants.DEFAULT_PEM_CERT_PATH);
				String keyStorePasswordStr = getFrameworkProp(
						InternalHttpConstants.JETTY_PROPERTY_PREFIX + InternalHttpConstants.SSL_PASSWORD);
				char[] keyStorePassword;
				if (keyStorePasswordStr == null)
					keyStorePassword = "changeit".toCharArray();
				else
					keyStorePassword = keyStorePasswordStr.toCharArray();

				// if PEM files both exists, update the PKCS12 file
				if (Files.exists(pemCertPath) && Files.exists(pemKeyPath)) {
					// TODO check certificate update time? monitor changes?
					KeyStore keyStore = PkiUtils.getKeyStore(keyStorePath, keyStorePassword, PkiUtils.PKCS12);
					try (Reader key = Files.newBufferedReader(pemKeyPath, StandardCharsets.US_ASCII);
							Reader cert = Files.newBufferedReader(pemCertPath, StandardCharsets.US_ASCII);) {
						PkiUtils.loadPem(keyStore, key, keyStorePassword, cert);
						PkiUtils.saveKeyStore(keyStorePath, keyStorePassword, keyStore);
						if (log.isDebugEnabled())
							log.debug("PEM certificate stored in " + keyStorePath);
					} catch (IOException e) {
						log.error("Cannot read PEM files " + pemKeyPath + " and " + pemCertPath, e);
					}
				}

				if (!Files.exists(keyStorePath))
					createSelfSignedKeyStore(keyStorePath, keyStorePassword, PkiUtils.PKCS12);
				props.put(InternalHttpConstants.SSL_KEYSTORETYPE, PkiUtils.PKCS12);
				props.put(InternalHttpConstants.SSL_KEYSTORE, keyStorePath.toString());
				props.put(InternalHttpConstants.SSL_PASSWORD, new String(keyStorePassword));

//				props.put(InternalHttpConstants.SSL_KEYSTORETYPE, "PKCS11");
//				props.put(InternalHttpConstants.SSL_KEYSTORE, "../../nssdb");
//				props.put(InternalHttpConstants.SSL_PASSWORD, keyStorePassword);

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

			props.put(CmsConstants.CN, CmsConstants.DEFAULT);
		}
		return props;
	}

	public static List<Dictionary<String, Object>> getUserDirectoryConfigs() {
		List<Dictionary<String, Object>> res = new ArrayList<>();
		File nodeBaseDir = KernelUtils.getOsgiInstancePath(KernelConstants.DIR_NODE).toFile();
		List<String> uris = new ArrayList<>();

		// node roles
		String nodeRolesUri = getFrameworkProp(CmsConstants.ROLES_URI);
		String baseNodeRoleDn = CmsConstants.ROLES_BASEDN;
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
		String nodeTokensUri = getFrameworkProp(CmsConstants.TOKENS_URI);
		String baseNodeTokensDn = CmsConstants.TOKENS_BASEDN;
		if (nodeTokensUri == null) {
			nodeTokensUri = baseNodeTokensDn + ".ldif";
			File nodeTokensFile = new File(nodeBaseDir, nodeTokensUri);
			if (!nodeTokensFile.exists())
				try {
					FileUtils.copyInputStreamToFile(InitUtils.class.getResourceAsStream(baseNodeTokensDn + ".ldif"),
							nodeTokensFile);
				} catch (IOException e) {
					throw new RuntimeException("Cannot copy demo resource", e);
				}
			// nodeRolesUri = nodeRolesFile.toURI().toString();
		}
		uris.add(nodeTokensUri);

		// Business roles
		String userAdminUris = getFrameworkProp(CmsConstants.USERADMIN_URIS);
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
	public static void prepareFirstInitInstanceArea() {
		String nodeInits = getFrameworkProp(CmsConstants.NODE_INIT);
		if (nodeInits == null)
			nodeInits = "../../init";

		for (String nodeInit : nodeInits.split(",")) {

			if (nodeInit.startsWith("http")) {
				// TODO reconnect it
				// registerRemoteInit(nodeInit);
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

	private static void createSelfSignedKeyStore(Path keyStorePath, char[] keyStorePassword, String keyStoreType) {
		// for (Provider provider : Security.getProviders())
		// System.out.println(provider.getName());
//		File keyStoreFile = keyStorePath.toFile();
		char[] keyPwd = Arrays.copyOf(keyStorePassword, keyStorePassword.length);
		if (!Files.exists(keyStorePath)) {
			try {
				Files.createDirectories(keyStorePath.getParent());
				KeyStore keyStore = PkiUtils.getKeyStore(keyStorePath, keyStorePassword, keyStoreType);
				PkiUtils.generateSelfSignedCertificate(keyStore,
						new X500Principal("CN=" + InetAddress.getLocalHost().getHostName() + ",OU=UNSECURE,O=UNSECURE"),
						1024, keyPwd);
				PkiUtils.saveKeyStore(keyStorePath, keyStorePassword, keyStore);
				if (log.isDebugEnabled())
					log.debug("Created self-signed unsecure keystore " + keyStorePath);
			} catch (Exception e) {
				try {
					if (Files.size(keyStorePath) == 0)
						Files.delete(keyStorePath);
				} catch (IOException e1) {
					// silent
				}
				log.error("Cannot create keystore " + keyStorePath, e);
			}
		} else {
			throw new IllegalStateException("Keystore " + keyStorePath + " already exists");
		}
	}

}