package org.argeo.cms.internal.kernel;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.ProviderNotFoundException;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.Hashtable;

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
import org.argeo.cms.KernelHeader;
import org.argeo.security.crypto.PkiUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.useradmin.UserAdmin;

/** Authentication and user management. */
class NodeSecurity {
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
		if (!defaultProvider.getName().equals(KernelHeader.SECURITY_PROVIDER))
			log.error("Provider name is " + defaultProvider.getName()
					+ " but it should be " + KernelHeader.SECURITY_PROVIDER);
	}

	private final BundleContext bundleContext;
	private final NodeUserAdmin userAdmin;
	private final Subject kernelSubject;

	private ServiceRegistration<UserAdmin> userAdminReg;

	public NodeSecurity(BundleContext bundleContext) {
		// Configure JAAS first
		URL url = getClass().getClassLoader().getResource(
				KernelConstants.JAAS_CONFIG);
		System.setProperty("java.security.auth.login.config",
				url.toExternalForm());

		this.bundleContext = bundleContext;
		this.kernelSubject = logKernel();
		userAdmin = new NodeUserAdmin();
	}

	private Subject logKernel() {
		final Subject kernelSubject = new Subject();
		createKeyStoreIfNeeded();

		CallbackHandler cbHandler = new CallbackHandler() {

			@Override
			public void handle(Callback[] callbacks) throws IOException,
					UnsupportedCallbackException {
				// alias
				((NameCallback) callbacks[1]).setName(KernelHeader.ROLE_KERNEL);
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

	public void publish() {
		userAdminReg = bundleContext.registerService(UserAdmin.class,
				userAdmin, userAdmin.currentState());
		}

	void destroy() {
		userAdmin.destroy();
		userAdminReg.unregister();

		// Logout kernel
		try {
			LoginContext kernelLc = new LoginContext(
					KernelConstants.LOGIN_CONTEXT_KERNEL, kernelSubject);
			kernelLc.logout();
		} catch (LoginException e) {
			throw new CmsException("Cannot log in kernel", e);
		}

		Security.removeProvider(KernelHeader.SECURITY_PROVIDER);
	}

	public NodeUserAdmin getUserAdmin() {
		return userAdmin;
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
						new X500Principal(KernelHeader.ROLE_KERNEL), keyPwd);
				PkiUtils.saveKeyStore(keyStoreFile, ksPwd, keyStore);
			} catch (Exception e) {
				throw new CmsException("Cannot create key store "
						+ keyStoreFile, e);
			}
		}
	}
}
