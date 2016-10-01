package org.argeo.cms.internal.kernel;

import static org.argeo.cms.internal.kernel.KernelUtils.getOsgiInstanceDir;

import java.io.File;
import java.net.URL;
import java.security.KeyStore;
import java.util.Arrays;

import javax.security.auth.Subject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;

/** Low-level kernel security */
@Deprecated
class NodeSecurity implements KernelConstants {
	private final static Log log = LogFactory.getLog(NodeSecurity.class);

	public final static int HARDENED = 3;
	public final static int STAGING = 2;
	public final static int DEV = 1;

	private final boolean firstInit;

	private  Subject kernelSubject;
	private int securityLevel = STAGING;

	private final File keyStoreFile;

	public NodeSecurity() {
		// Configure JAAS first
		URL url = getClass().getClassLoader().getResource(KernelConstants.JAAS_CONFIG);
		System.setProperty("java.security.auth.login.config", url.toExternalForm());
		// log.debug("JASS config: " + url.toExternalForm());
		// disable Jetty autostart
		// System.setProperty("org.eclipse.equinox.http.jetty.autostart",
		// "false");

		firstInit = !new File(getOsgiInstanceDir(), DIR_NODE).exists();

		this.keyStoreFile = new File(KernelUtils.getOsgiInstanceDir(), "node.p12");
		createKeyStoreIfNeeded();
//		if (keyStoreFile.exists())
//			this.kernelSubject = logInHardenedKernel();
//		else
//			this.kernelSubject = logInKernel();
	}

//	private Subject logInKernel() {
//		final Subject kernelSubject = new Subject();
//		try {
//			LoginContext kernelLc = new LoginContext(KernelConstants.LOGIN_CONTEXT_KERNEL, kernelSubject);
//			kernelLc.login();
//		} catch (LoginException e) {
//			throw new CmsException("Cannot log in kernel", e);
//		}
//		return kernelSubject;
//	}
//
//	private Subject logInHardenedKernel() {
//		final Subject kernelSubject = new Subject();
//		createKeyStoreIfNeeded();
//
//		CallbackHandler cbHandler = new CallbackHandler() {
//
//			@Override
//			public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
//				// alias
////				((NameCallback) callbacks[1]).setName(AuthConstants.ROLE_KERNEL);
//				// store pwd
//				((PasswordCallback) callbacks[2]).setPassword("changeit".toCharArray());
//				// key pwd
//				((PasswordCallback) callbacks[3]).setPassword("changeit".toCharArray());
//			}
//		};
//		try {
//			LoginContext kernelLc = new LoginContext(KernelConstants.LOGIN_CONTEXT_HARDENED_KERNEL, kernelSubject,
//					cbHandler);
//			kernelLc.login();
//		} catch (LoginException e) {
//			throw new CmsException("Cannot log in kernel", e);
//		}
//		return kernelSubject;
//	}

//	void destroy() {
//		// Logout kernel
//		try {
//			LoginContext kernelLc = new LoginContext(KernelConstants.LOGIN_CONTEXT_KERNEL, kernelSubject);
//			kernelLc.logout();
//		} catch (LoginException e) {
//			throw new CmsException("Cannot log out kernel", e);
//		}
//
//		// Security.removeProvider(SECURITY_PROVIDER);
//	}

	public Subject getKernelSubject() {
		return kernelSubject;
	}

	public synchronized int getSecurityLevel() {
		return securityLevel;
	}

	public boolean isFirstInit() {
		return firstInit;
	}

	public void setSecurityLevel(int newValue) {
		if (newValue != STAGING || newValue != DEV)
			throw new CmsException("Invalid value for security level " + newValue);
		if (newValue >= securityLevel)
			throw new CmsException(
					"Impossible to increase security level (from " + securityLevel + " to " + newValue + ")");
		securityLevel = newValue;
	}

	private void createKeyStoreIfNeeded() {
		// for (Provider provider : Security.getProviders())
		// System.out.println(provider.getName());

		char[] ksPwd = "changeit".toCharArray();
		char[] keyPwd = Arrays.copyOf(ksPwd, ksPwd.length);
		if (!keyStoreFile.exists()) {
			try {
				keyStoreFile.getParentFile().mkdirs();
				KeyStore keyStore = PkiUtils.getKeyStore(keyStoreFile, ksPwd);
//				PkiUtils.generateSelfSignedCertificate(keyStore, new X500Principal(AuthConstants.ROLE_KERNEL), 1024,
//						keyPwd);
				PkiUtils.saveKeyStore(keyStoreFile, ksPwd, keyStore);
				if (log.isDebugEnabled())
					log.debug("Created keystore " + keyStoreFile);
			} catch (Exception e) {
				if (keyStoreFile.length() == 0)
					keyStoreFile.delete();
				log.error("Cannot create keystore " + keyStoreFile, e);
			}
		}
	}

	File getHttpServerKeyStore() {
		return keyStoreFile;
	}

	// private final static String SECURITY_PROVIDER = "BC";// Bouncy Castle
	// private final static Log log;
	// static {
	// log = LogFactory.getLog(NodeSecurity.class);
	// // Make Bouncy Castle the default provider
	// Provider provider = new BouncyCastleProvider();
	// int position = Security.insertProviderAt(provider, 1);
	// if (position == -1)
	// log.error("Provider " + provider.getName()
	// + " already installed and could not be set as default");
	// Provider defaultProvider = Security.getProviders()[0];
	// if (!defaultProvider.getName().equals(SECURITY_PROVIDER))
	// log.error("Provider name is " + defaultProvider.getName()
	// + " but it should be " + SECURITY_PROVIDER);
	// }
}
