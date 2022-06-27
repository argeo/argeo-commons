package org.argeo.cms.internal.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;
import java.util.UUID;

import javax.security.auth.login.Configuration;

import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsState;
import org.argeo.api.uuid.UuidFactory;
import org.argeo.cms.auth.ident.IdentClient;
import org.argeo.util.directory.ldap.LdifParser;

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
			try {
				this.hostname = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				log.error("Cannot set hostname: " + e);
			}

			availableSince = System.currentTimeMillis();
			if (log.isDebugEnabled())
				// log.debug("## CMS starting... stateUuid=" + this.stateUuid + (cleanState ? "
				// (clean state) " : " "));
				log.debug("## CMS starting... (" + uuid + ")");


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
		if (System.getProperty(KernelConstants.JAAS_CONFIG_PROP) == null) {
			String jaasConfig = KernelConstants.JAAS_CONFIG;
			URL url = getClass().getResource(jaasConfig);
			// System.setProperty(KernelConstants.JAAS_CONFIG_PROP,
			// url.toExternalForm());
			KernelUtils.setJaasConfiguration(url);
		}
		// explicitly load JAAS configuration
		Configuration.getConfiguration();
	}

	public void stop() {
		if (log.isDebugEnabled())
			log.debug("CMS stopping...  (" + this.uuid + ")");

		long duration = ((System.currentTimeMillis() - availableSince) / 1000) / 60;
		log.info("## ARGEO CMS STOPPED after " + (duration / 60) + "h " + (duration % 60) + "min uptime ##");
	}

	private void firstInit() throws IOException {
		log.info("## FIRST INIT ##");
		// FirstInit firstInit = new FirstInit();
		InitUtils.prepareFirstInitInstanceArea();
	}

	@Override
	public String getDeployProperty(String key) {
		return KernelUtils.getFrameworkProp(key);
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
	public String getHostname() {
		return hostname;
	}

	@Override
	public UUID getUuid() {
		return uuid;
	}

	public void setUuidFactory(UuidFactory uuidFactory) {
		this.uuidFactory = uuidFactory;
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
