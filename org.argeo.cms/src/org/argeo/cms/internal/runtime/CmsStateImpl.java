package org.argeo.cms.internal.runtime;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.security.auth.login.Configuration;

import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsState;
import org.argeo.api.uuid.UuidFactory;
import org.argeo.cms.CmsDeployProperty;
import org.argeo.cms.auth.ident.IdentClient;
import org.argeo.cms.util.FsUtils;
import org.argeo.cms.util.OS;

/**
 * Implementation of a {@link CmsState}, initialising the required services.
 */
public class CmsStateImpl implements CmsState {
	private final static CmsLog log = CmsLog.getLog(CmsStateImpl.class);

	// REFERENCES
	private Long availableSince;

	private UUID uuid;
//	private final boolean cleanState;
	private String hostname;

	private UuidFactory uuidFactory;

	private final Map<CmsDeployProperty, String> deployPropertyDefaults;

	public CmsStateImpl() {
		this.deployPropertyDefaults = Collections.unmodifiableMap(createDeployPropertiesDefaults());
	}

	protected Map<CmsDeployProperty, String> createDeployPropertiesDefaults() {
		Map<CmsDeployProperty, String> deployPropertyDefaults = new HashMap<>();
		deployPropertyDefaults.put(CmsDeployProperty.NODE_INIT, "../../init");
		deployPropertyDefaults.put(CmsDeployProperty.LOCALE, Locale.getDefault().toString());

		// certificates
		deployPropertyDefaults.put(CmsDeployProperty.SSL_KEYSTORETYPE, KernelConstants.PKCS12);
		deployPropertyDefaults.put(CmsDeployProperty.SSL_PASSWORD, KernelConstants.DEFAULT_KEYSTORE_PASSWORD);
		Path keyStorePath = getDataPath(KernelConstants.DEFAULT_KEYSTORE_PATH);
		if (keyStorePath != null) {
			deployPropertyDefaults.put(CmsDeployProperty.SSL_KEYSTORE, keyStorePath.toAbsolutePath().toString());
		}

		Path trustStorePath = getDataPath(KernelConstants.DEFAULT_TRUSTSTORE_PATH);
		if (trustStorePath != null) {
			deployPropertyDefaults.put(CmsDeployProperty.SSL_TRUSTSTORE, trustStorePath.toAbsolutePath().toString());
		}
		deployPropertyDefaults.put(CmsDeployProperty.SSL_TRUSTSTORETYPE, KernelConstants.PKCS12);
		deployPropertyDefaults.put(CmsDeployProperty.SSL_TRUSTSTOREPASSWORD, KernelConstants.DEFAULT_KEYSTORE_PASSWORD);

		// SSH
		Path authorizedKeysPath = getDataPath(KernelConstants.NODE_SSHD_AUTHORIZED_KEYS_PATH);
		if (authorizedKeysPath != null) {
			deployPropertyDefaults.put(CmsDeployProperty.SSHD_AUTHORIZEDKEYS,
					authorizedKeysPath.toAbsolutePath().toString());
		}
		return deployPropertyDefaults;
	}

	public void start() {
		Charset defaultCharset = Charset.defaultCharset();
		if (!StandardCharsets.UTF_8.equals(defaultCharset))
			log.error("Default JVM charset is " + defaultCharset + " and not " + StandardCharsets.UTF_8);
		try {
			// First init check
			Path privateBase = getDataPath(KernelConstants.DIR_PRIVATE);
			if (privateBase != null && !Files.exists(privateBase)) {// first init
				firstInit();
				Files.createDirectories(privateBase);
			}

			initSecurity();
//			initArgeoLogger();

			if (log.isTraceEnabled())
				log.trace("CMS State started");

			String frameworkUuid = KernelUtils.getFrameworkProp(KernelUtils.OSGI_FRAMEWORK_UUID);
			this.uuid = frameworkUuid != null ? UUID.fromString(frameworkUuid) : uuidFactory.timeUUID();

			// hostname
			this.hostname = getDeployProperty(CmsDeployProperty.HOST);
			// TODO verify we have access to the IP address
			if (hostname == null) {
				final String LOCALHOST_IP = "::1";
				ForkJoinTask<String> hostnameFJT = ForkJoinPool.commonPool().submit(() -> {
					try {
						String hostname = InetAddress.getLocalHost().getHostName();
						return hostname;
					} catch (UnknownHostException e) {
						throw new IllegalStateException("Cannot get local hostname", e);
					}
				});
				try {
					this.hostname = hostnameFJT.get(5, TimeUnit.SECONDS);
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					this.hostname = LOCALHOST_IP;
					log.warn("Could not get local hostname, using " + this.hostname);
				}
			}

			availableSince = System.currentTimeMillis();
			if (log.isDebugEnabled()) {
				// log.debug("## CMS starting... stateUuid=" + this.stateUuid + (cleanState ? "
				// (clean state) " : " "));
				StringJoiner sb = new StringJoiner("\n");
				CmsDeployProperty[] deployProperties = CmsDeployProperty.values();
				Arrays.sort(deployProperties, (o1, o2) -> o1.name().compareTo(o2.name()));
				for (CmsDeployProperty deployProperty : deployProperties) {
					List<String> values = getDeployProperties(deployProperty);
					for (int i = 0; i < values.size(); i++) {
						String value = values.get(i);
						if (value != null) {
							boolean isDefault = deployPropertyDefaults.containsKey(deployProperty)
									&& value.equals(deployPropertyDefaults.get(deployProperty));
							String line = deployProperty.getProperty() + (i == 0 ? "" : "." + i) + "=" + value
									+ (isDefault ? " (default)" : "");
							sb.add(line);
						}
					}
				}
				log.debug("## CMS starting... (" + uuid + ")\n" + sb + "\n");
			}

			if (log.isTraceEnabled()) {
				// print system properties
				StringJoiner sb = new StringJoiner("\n");
				for (Object key : new TreeMap<>(System.getProperties()).keySet()) {
					sb.add(key + "=" + System.getProperty(key.toString()));
				}
				log.trace("System properties:\n" + sb + "\n");

			}

		} catch (RuntimeException | IOException e) {
			log.error("## FATAL: CMS state failed", e);
		}
	}

	private void initSecurity() {
		// private directory permissions
		Path privateDir = getDataPath(KernelConstants.DIR_PRIVATE);
		if (privateDir != null) {
			// TODO rather check whether we can read and write
			Set<PosixFilePermission> posixPermissions = new HashSet<>();
			posixPermissions.add(PosixFilePermission.OWNER_READ);
			posixPermissions.add(PosixFilePermission.OWNER_WRITE);
			posixPermissions.add(PosixFilePermission.OWNER_EXECUTE);
			try {
				if (!Files.exists(privateDir))
					Files.createDirectories(privateDir);
				if (!OS.LOCAL.isMSWindows())
					Files.setPosixFilePermissions(privateDir, posixPermissions);
			} catch (IOException e) {
				log.error("Cannot set permissions on " + privateDir, e);
			}
		}

		if (getDeployProperty(CmsDeployProperty.JAVA_LOGIN_CONFIG) == null) {
			String jaasConfig = KernelConstants.JAAS_CONFIG;
			URL url = getClass().getResource(jaasConfig);
			// System.setProperty(KernelConstants.JAAS_CONFIG_PROP,
			// url.toExternalForm());
			KernelUtils.setJaasConfiguration(url);
		}
		// explicitly load JAAS configuration
		Configuration.getConfiguration();

		boolean initCertificates = (getDeployProperty(CmsDeployProperty.HTTPS_PORT) != null)
				|| (getDeployProperty(CmsDeployProperty.SSHD_PORT) != null);
		if (initCertificates) {
			initCertificates();
		}
	}

	private void initCertificates() {
		// server certificate
		Path keyStorePath = Paths.get(getDeployProperty(CmsDeployProperty.SSL_KEYSTORE));
		Path pemKeyPath = getDataPath(KernelConstants.DEFAULT_PEM_KEY_PATH);
		Path pemCertPath = getDataPath(KernelConstants.DEFAULT_PEM_CERT_PATH);
		char[] keyStorePassword = getDeployProperty(CmsDeployProperty.SSL_PASSWORD).toCharArray();

		// Keystore
		// if PEM files both exists, update the PKCS12 file
		if (Files.exists(pemCertPath) && Files.exists(pemKeyPath)) {
			// TODO check certificate update time? monitor changes?
			KeyStore keyStore = PkiUtils.getKeyStore(keyStorePath, keyStorePassword,
					getDeployProperty(CmsDeployProperty.SSL_KEYSTORETYPE));
			try (Reader key = Files.newBufferedReader(pemKeyPath, StandardCharsets.US_ASCII);
					BufferedInputStream cert = new BufferedInputStream(Files.newInputStream(pemCertPath));) {
				PkiUtils.loadPrivateCertificatePem(keyStore, CmsConstants.NODE, key, keyStorePassword, cert);
				Files.createDirectories(keyStorePath.getParent());
				PkiUtils.saveKeyStore(keyStorePath, keyStorePassword, keyStore);
				if (log.isDebugEnabled())
					log.debug("PEM certificate stored in " + keyStorePath);
			} catch (IOException e) {
				log.error("Cannot read PEM files " + pemKeyPath + " and " + pemCertPath, e);
			}
		}

		// Truststore
		Path trustStorePath = Paths.get(getDeployProperty(CmsDeployProperty.SSL_TRUSTSTORE));
		char[] trustStorePassword = getDeployProperty(CmsDeployProperty.SSL_TRUSTSTOREPASSWORD).toCharArray();

		// IPA CA
		Path ipaCaCertPath = Paths.get(KernelConstants.IPA_PEM_CA_CERT_PATH);
		if (Files.exists(ipaCaCertPath)) {
			KeyStore trustStore = PkiUtils.getKeyStore(trustStorePath, trustStorePassword,
					getDeployProperty(CmsDeployProperty.SSL_TRUSTSTORETYPE));
			try (BufferedInputStream cert = new BufferedInputStream(Files.newInputStream(ipaCaCertPath));) {
				PkiUtils.loadTrustedCertificatePem(trustStore, trustStorePassword, cert);
				Files.createDirectories(keyStorePath.getParent());
				PkiUtils.saveKeyStore(trustStorePath, trustStorePassword, trustStore);
				if (log.isDebugEnabled())
					log.debug("IPA CA certificate stored in " + trustStorePath);
			} catch (IOException e) {
				log.error("Cannot trust CA certificate", e);
			}
		}
	}

	public void stop() {
		if (log.isDebugEnabled())
			log.debug("CMS stopping...  (" + this.uuid + ")");

		long duration = ((System.currentTimeMillis() - availableSince) / 1000) / 60;
		log.info("## ARGEO CMS " + uuid + " STOPPED after " + (duration / 60) + "h " + (duration % 60)
				+ "min uptime ##");
	}

	private void firstInit() throws IOException {
		log.info("## FIRST INIT ##");
		List<String> nodeInits = getDeployProperties(CmsDeployProperty.NODE_INIT);
//		if (nodeInits == null)
//			nodeInits = "../../init";
		CmsStateImpl.prepareFirstInitInstanceArea(nodeInits);
	}

	@Override
	public String getDeployProperty(String property) {
		CmsDeployProperty deployProperty = CmsDeployProperty.find(property);
		if (deployProperty == null) {
			// legacy
			if (property.startsWith("argeo.node.")) {
				return doGetDeployProperty(property);
			}
			if (property.equals("argeo.i18n.locales")) {
				String value = doGetDeployProperty(property);
				if (value != null) {
					log.warn("Property " + property + " was ignored (value=" + value + ")");

				}
				return null;
			}
			throw new IllegalArgumentException("Unsupported deploy property " + property);
		}
		int index = CmsDeployProperty.getPropertyIndex(property);
		return getDeployProperty(deployProperty, index);
	}

	@Override
	public List<String> getDeployProperties(String property) {
		CmsDeployProperty deployProperty = CmsDeployProperty.find(property);
		if (deployProperty == null)
			return new ArrayList<>();
		return getDeployProperties(deployProperty);
	}

	public static List<String> getDeployProperties(CmsState cmsState, CmsDeployProperty deployProperty) {
		return ((CmsStateImpl) cmsState).getDeployProperties(deployProperty);
	}

	public List<String> getDeployProperties(CmsDeployProperty deployProperty) {
		List<String> res = new ArrayList<>(deployProperty.getMaxCount());
		for (int i = 0; i < deployProperty.getMaxCount(); i++) {
			// String propertyName = i == 0 ? deployProperty.getProperty() :
			// deployProperty.getProperty() + "." + i;
			String value = getDeployProperty(deployProperty, i);
			res.add(i, value);
		}
		return res;
	}

	public static String getDeployProperty(CmsState cmsState, CmsDeployProperty deployProperty) {
		return ((CmsStateImpl) cmsState).getDeployProperty(deployProperty);
	}

	public String getDeployProperty(CmsDeployProperty deployProperty) {
		String value = getDeployProperty(deployProperty, 0);
		return value;
	}

	public String getDeployProperty(CmsDeployProperty deployProperty, int index) {
		String propertyName = deployProperty.getProperty() + (index == 0 ? "" : "." + index);
		String value = doGetDeployProperty(propertyName);
		if (value == null && index == 0) {
			// try defaults
			if (deployPropertyDefaults.containsKey(deployProperty)) {
				value = deployPropertyDefaults.get(deployProperty);
				if (deployProperty.isSystemPropertyOnly())
					System.setProperty(deployProperty.getProperty(), value);
			}

			if (value == null) {
				// try legacy properties
				String legacyProperty = switch (deployProperty) {
				case DIRECTORY -> "argeo.node.useradmin.uris";
				case DB_URL -> "argeo.node.dburl";
				case DB_USER -> "argeo.node.dbuser";
				case DB_PASSWORD -> "argeo.node.dbpassword";
				case HTTP_PORT -> "org.osgi.service.http.port";
				case HTTPS_PORT -> "org.osgi.service.http.port.secure";
				case HOST -> "org.eclipse.equinox.http.jetty.http.host";
				case LOCALE -> "argeo.i18n.defaultLocale";

				default -> null;
				};
				if (legacyProperty != null) {
					value = doGetDeployProperty(legacyProperty);
					if (value != null) {
						log.warn("Retrieved deploy property " + deployProperty.getProperty()
								+ " through deprecated property " + legacyProperty);
					}
				}
			}
		}
		if (index == 0 && deployProperty.isSystemPropertyOnly()) {
			String systemPropertyValue = System.getProperty(deployProperty.getProperty());
			if (!Objects.equals(value, systemPropertyValue))
				throw new IllegalStateException(
						"Property " + deployProperty + " must be a ssystem property, but its value is " + value
								+ ", while the system property value is " + systemPropertyValue);
		}
		return value != null ? value.toString() : null;
	}

	protected String getLegacyProperty(String legacyProperty, CmsDeployProperty deployProperty) {
		String value = doGetDeployProperty(legacyProperty);
		if (value != null) {
			log.warn("Retrieved deploy property " + deployProperty.getProperty() + " through deprecated property "
					+ legacyProperty + ".");
		}
		return value;
	}

	protected String doGetDeployProperty(String property) {
		return KernelUtils.getFrameworkProp(property);
	}

	@Override
	public Path getDataPath(String relativePath) {
		return KernelUtils.getOsgiInstancePath(relativePath);
	}

	@Override
	public Path getStatePath(String relativePath) {
		return KernelUtils.getOsgiConfigurationPath(relativePath);
	}

	@Override
	public Long getAvailableSince() {
		return availableSince;
	}

	/*
	 * ACCESSORS
	 */
	@Override
	public UUID getUuid() {
		return uuid;
	}

	public void setUuidFactory(UuidFactory uuidFactory) {
		this.uuidFactory = uuidFactory;
	}

	public String getHostname() {
		return hostname;
	}

	/**
	 * Called before node initialisation, in order populate OSGi instance are with
	 * some files (typically LDIF, etc).
	 */
	public static void prepareFirstInitInstanceArea(List<String> nodeInits) {

		for (String nodeInit : nodeInits) {
			if (nodeInit == null)
				continue;

			if (nodeInit.startsWith("http")) {
				// TODO reconnect it
				// registerRemoteInit(nodeInit);
			} else {

				// TODO use java.nio.file
				Path initDir;
				if (nodeInit.startsWith("."))
					initDir = KernelUtils.getExecutionDir(nodeInit);
				else
					initDir = Paths.get(nodeInit);
				// TODO also uncompress archives
				if (Files.exists(initDir)) {
					Path dataPath = KernelUtils.getOsgiInstancePath("");
					FsUtils.copyDirectory(initDir, dataPath);
					log.info("CMS initialized from " + initDir);
				}
			}
		}
	}

	/*
	 * STATIC
	 */
	public static IdentClient getIdentClient(String remoteAddr) {
		if (!IdentClient.isDefaultAuthdPassphraseFileAvailable())
			return null;
		// TODO make passphrase more configurable
		return new IdentClient(remoteAddr);
	}
}
