package org.argeo.cms.internal.runtime;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;

import javax.security.auth.login.Configuration;

import org.apache.commons.io.FileUtils;
import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsState;
import org.argeo.api.uuid.UuidFactory;
import org.argeo.cms.CmsDeployProperty;
import org.argeo.cms.auth.ident.IdentClient;

/**
 * Implementation of a {@link CmsState}, initialising the required services.
 */
public class CmsStateImpl implements CmsState {
	private final static CmsLog log = CmsLog.getLog(CmsStateImpl.class);

	// REFERENCES
	private Long availableSince;

	private UUID uuid;
//	private final boolean cleanState;
//	private String hostname;

	private UuidFactory uuidFactory;

	private final Map<CmsDeployProperty, String> deployPropertyDefaults;

	public CmsStateImpl() {
		Map<CmsDeployProperty, String> deployPropertyDefaults = new HashMap<>();
		deployPropertyDefaults.put(CmsDeployProperty.NODE_INIT, "../../init");
		deployPropertyDefaults.put(CmsDeployProperty.LOCALE, Locale.getDefault().toString());

		// certificates
		deployPropertyDefaults.put(CmsDeployProperty.SSL_KEYSTORETYPE, PkiUtils.PKCS12);
		deployPropertyDefaults.put(CmsDeployProperty.SSL_PASSWORD, PkiUtils.DEFAULT_KEYSTORE_PASSWORD);
		Path keyStorePath = getDataPath(PkiUtils.DEFAULT_KEYSTORE_PATH);
		deployPropertyDefaults.put(CmsDeployProperty.SSL_KEYSTORE, keyStorePath.toAbsolutePath().toString());

		Path trustStorePath = getDataPath(PkiUtils.DEFAULT_TRUSTSTORE_PATH);
		deployPropertyDefaults.put(CmsDeployProperty.SSL_TRUSTSTORETYPE, PkiUtils.PKCS12);
		deployPropertyDefaults.put(CmsDeployProperty.SSL_TRUSTSTOREPASSWORD, PkiUtils.DEFAULT_KEYSTORE_PASSWORD);
		deployPropertyDefaults.put(CmsDeployProperty.SSL_TRUSTSTORE, trustStorePath.toAbsolutePath().toString());

		this.deployPropertyDefaults = Collections.unmodifiableMap(deployPropertyDefaults);
	}

	public void start() {
//		Runtime.getRuntime().addShutdownHook(new CmsShutdown());

		try {
			initSecurity();
//			initArgeoLogger();

			if (log.isTraceEnabled())
				log.trace("CMS State started");

//			String stateUuidStr = KernelUtils.getFrameworkProp(Constants.FRAMEWORK_UUID);
//			this.uuid = UUID.fromString(stateUuidStr);
			this.uuid = uuidFactory.timeUUID();
//		this.cleanState = stateUuid.equals(frameworkUuid);
//			try {
//				this.hostname = InetAddress.getLocalHost().getHostName();
//			} catch (UnknownHostException e) {
//				log.error("Cannot set hostname: " + e);
//			}

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

//		initI18n();
//		initServices();
			if (!Files.exists(getDataPath(CmsConstants.NODE))) {// first init
				firstInit();
			}

		} catch (RuntimeException | IOException e) {
			log.error("## FATAL: CMS activator failed", e);
		}
	}

	private void initSecurity() {
		if (getDeployProperty(CmsDeployProperty.JAVA_LOGIN_CONFIG) == null) {
			String jaasConfig = KernelConstants.JAAS_CONFIG;
			URL url = getClass().getResource(jaasConfig);
			// System.setProperty(KernelConstants.JAAS_CONFIG_PROP,
			// url.toExternalForm());
			KernelUtils.setJaasConfiguration(url);
		}
		// explicitly load JAAS configuration
		Configuration.getConfiguration();

		boolean initSsl = getDeployProperty(CmsDeployProperty.HTTPS_PORT) != null;
		if (initSsl) {
			initCertificates();
		}
	}

	private void initCertificates() {
		// server certificate
		Path keyStorePath = Paths.get(getDeployProperty(CmsDeployProperty.SSL_KEYSTORE));
		Path pemKeyPath = getDataPath(PkiUtils.DEFAULT_PEM_KEY_PATH);
		Path pemCertPath = getDataPath(PkiUtils.DEFAULT_PEM_CERT_PATH);
		char[] keyStorePassword = getDeployProperty(CmsDeployProperty.SSL_PASSWORD).toCharArray();

		// Keystore
		// if PEM files both exists, update the PKCS12 file
		if (Files.exists(pemCertPath) && Files.exists(pemKeyPath)) {
			// TODO check certificate update time? monitor changes?
			KeyStore keyStore = PkiUtils.getKeyStore(keyStorePath, keyStorePassword,
					getDeployProperty(CmsDeployProperty.SSL_KEYSTORETYPE));
			try (Reader key = Files.newBufferedReader(pemKeyPath, StandardCharsets.US_ASCII);
					Reader cert = Files.newBufferedReader(pemCertPath, StandardCharsets.US_ASCII);) {
				PkiUtils.loadPem(keyStore, key, keyStorePassword, cert);
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
		Path ipaCaCertPath = Paths.get(PkiUtils.IPA_PEM_CA_CERT_PATH);
		if (Files.exists(ipaCaCertPath)) {
			KeyStore trustStore = PkiUtils.getKeyStore(trustStorePath, trustStorePassword,
					getDeployProperty(CmsDeployProperty.SSL_TRUSTSTORETYPE));
			try (Reader cert = Files.newBufferedReader(ipaCaCertPath, StandardCharsets.US_ASCII);) {
				PkiUtils.loadPem(trustStore, null, trustStorePassword, cert);
				Files.createDirectories(keyStorePath.getParent());
				PkiUtils.saveKeyStore(trustStorePath, trustStorePassword, trustStore);
				if (log.isDebugEnabled())
					log.debug("IPA CA certificate stored in " + trustStorePath);
			} catch (IOException e) {
				log.error("Cannot trust CA certificate", e);
			}
		}

		if (!Files.exists(keyStorePath))
			PkiUtils.createSelfSignedKeyStore(keyStorePath, keyStorePassword, PkiUtils.PKCS12);
//		props.put(JettyHttpConstants.SSL_KEYSTORETYPE, PkiUtils.PKCS12);
//		props.put(JettyHttpConstants.SSL_KEYSTORE, keyStorePath.toString());
//		props.put(JettyHttpConstants.SSL_PASSWORD, new String(keyStorePassword));

//		props.put(InternalHttpConstants.SSL_KEYSTORETYPE, "PKCS11");
//		props.put(InternalHttpConstants.SSL_KEYSTORE, "../../nssdb");
//		props.put(InternalHttpConstants.SSL_PASSWORD, keyStorePassword);

	}

	public void stop() {
		if (log.isDebugEnabled())
			log.debug("CMS stopping...  (" + this.uuid + ")");

		long duration = ((System.currentTimeMillis() - availableSince) / 1000) / 60;
		log.info("## ARGEO CMS STOPPED after " + (duration / 60) + "h " + (duration % 60) + "min uptime ##");
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

	/**
	 * Called before node initialisation, in order populate OSGi instance are with
	 * some files (typically LDIF, etc).
	 */
	public static void prepareFirstInitInstanceArea(List<String> nodeInits) {

		for (String nodeInit : nodeInits) {
			if(nodeInit==null)
				continue;

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
						// TODO use NIO utilities
						FileUtils.copyDirectory(initDir, KernelUtils.getOsgiInstancePath("").toFile(),
								new FileFilter() {

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
