package org.argeo.cms.internal.kernel;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.cms.auth.AuthConstants;
import org.argeo.security.crypto.PkiUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/** Authentication and user management. */
class NodeSecurity {
	final static String SECURITY_PROVIDER = "BC";// Bouncy Castle

	private final static Log log;
	static {
		log = LogFactory.getLog(NodeSecurity.class);
		// Make Bouncy Castle the default provider
		Provider provider = new BouncyCastleProvider();
		int position = Security.insertProviderAt(provider, 1);
		if (position == -1)
			log.error("Provider " + provider.getName()
					+ " already installed and could not be set as default");
		Provider defaultProvider = Security.getProviders()[0];
		if (!defaultProvider.getName().equals(SECURITY_PROVIDER))
			log.error("Provider name is " + defaultProvider.getName()
					+ " but it should be " + SECURITY_PROVIDER);
	}

	private final Subject kernelSubject;

	public NodeSecurity() {
		// Configure JAAS first
		URL url = getClass().getClassLoader().getResource(
				KernelConstants.JAAS_CONFIG);
		System.setProperty("java.security.auth.login.config",
				url.toExternalForm());

		this.kernelSubject = logKernel();
	}

	private Subject logKernel() {
		final Subject kernelSubject = new Subject();
		createKeyStoreIfNeeded();

		CallbackHandler cbHandler = new CallbackHandler() {

			@Override
			public void handle(Callback[] callbacks) throws IOException,
					UnsupportedCallbackException {
				// alias
				((NameCallback) callbacks[1]).setName(AuthConstants.ROLE_KERNEL);
				// store pwd
				((PasswordCallback) callbacks[2]).setPassword("changeit"
						.toCharArray());
				// key pwd
				((PasswordCallback) callbacks[3]).setPassword("changeit"
						.toCharArray());
			}
		};
		try {
			LoginContext kernelLc = new LoginContext(
					KernelConstants.LOGIN_CONTEXT_KERNEL, kernelSubject,
					cbHandler);
			kernelLc.login();
		} catch (LoginException e) {
			throw new CmsException("Cannot log in kernel", e);
		}
		return kernelSubject;
	}

	void destroy() {
		// Logout kernel
		try {
			LoginContext kernelLc = new LoginContext(
					KernelConstants.LOGIN_CONTEXT_KERNEL, kernelSubject);
			kernelLc.logout();
		} catch (LoginException e) {
			throw new CmsException("Cannot log in kernel", e);
		}

		Security.removeProvider(SECURITY_PROVIDER);
	}

	public Subject getKernelSubject() {
		return kernelSubject;
	}

	private void createKeyStoreIfNeeded() {
		char[] ksPwd = "changeit".toCharArray();
		char[] keyPwd = Arrays.copyOf(ksPwd, ksPwd.length);
		File keyStoreFile = new File(KernelUtils.getOsgiInstanceDir(),
				"node.p12");
		if (!keyStoreFile.exists()) {
			try {
				keyStoreFile.getParentFile().mkdirs();
				KeyStore keyStore = PkiUtils.getKeyStore(keyStoreFile, ksPwd);
				PkiUtils.generateSelfSignedCertificate(keyStore,
						new X500Principal(AuthConstants.ROLE_KERNEL), keyPwd);
				PkiUtils.saveKeyStore(keyStoreFile, ksPwd, keyStore);
			} catch (Exception e) {
				throw new CmsException("Cannot create key store "
						+ keyStoreFile, e);
			}
		}
	}
}
