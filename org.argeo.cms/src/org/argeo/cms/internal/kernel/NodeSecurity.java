package org.argeo.cms.internal.kernel;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.ProviderNotFoundException;
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.cms.KernelHeader;
import org.argeo.osgi.useradmin.AbstractUserDirectory;
import org.argeo.osgi.useradmin.LdapUserAdmin;
import org.argeo.osgi.useradmin.LdifUserAdmin;
import org.argeo.security.crypto.PkiUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.useradmin.UserAdmin;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/** Authentication and user management. */
class NodeSecurity implements AuthenticationManager {
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

	// private final OsAuthenticationProvider osAuth;
	// private final InternalAuthenticationProvider internalAuth;
	// private final AnonymousAuthenticationProvider anonymousAuth;
	// private final JackrabbitUserAdminService userAdminService;

	private ServiceRegistration<AuthenticationManager> authenticationManagerReg;
	// private ServiceRegistration<UserAdminService> userAdminServiceReg;
	// private ServiceRegistration<UserDetailsManager> userDetailsManagerReg;

	private ServiceRegistration<UserAdmin> userAdminReg;

	public NodeSecurity(BundleContext bundleContext) {
		// Configure JAAS first
		URL url = getClass().getClassLoader().getResource(
				KernelConstants.JAAS_CONFIG);
		System.setProperty("java.security.auth.login.config",
				url.toExternalForm());

		this.bundleContext = bundleContext;
		this.kernelSubject = logKernel();

		// osAuth = new OsAuthenticationProvider();
		// internalAuth = new InternalAuthenticationProvider(
		// Activator.getSystemKey());
		// anonymousAuth = new AnonymousAuthenticationProvider(
		// Activator.getSystemKey());

		// user admin
		// userAdminService = new JackrabbitUserAdminService();
		// userAdminService.setRepository(node);
		// userAdminService.setSecurityModel(new SimpleJcrSecurityModel());
		// userAdminService.init();

		userAdmin = new NodeUserAdmin();

		File osgiInstanceDir = KernelUtils.getOsgiInstanceDir();
		File homeDir = new File(osgiInstanceDir, "node");
		homeDir.mkdirs();

		String userAdminUri = KernelUtils
				.getFrameworkProp(KernelConstants.USERADMIN_URI);
		String baseDn = "dc=example,dc=com";
		if (userAdminUri == null) {
			File businessRolesFile = new File(homeDir, baseDn + ".ldif");
			// userAdminUri = getClass().getResource(baseDn +
			// ".ldif").toString();
			if (!businessRolesFile.exists())
				try {
					FileUtils.copyInputStreamToFile(getClass()
							.getResourceAsStream(baseDn + ".ldif"),
							businessRolesFile);
				} catch (IOException e) {
					throw new CmsException("Cannot copy demo resource", e);
				}
			userAdminUri = businessRolesFile.toURI().toString();
		}

		AbstractUserDirectory businessRoles;
		if (userAdminUri.startsWith("ldap"))
			businessRoles = new LdapUserAdmin(userAdminUri);
		else {
			businessRoles = new LdifUserAdmin(userAdminUri);
		}
		businessRoles.init();
		userAdmin.addUserAdmin(baseDn, businessRoles);

		String baseNodeRoleDn = KernelHeader.ROLES_BASEDN;
		File nodeRolesFile = new File(homeDir, baseNodeRoleDn + ".ldif");
		if (!nodeRolesFile.exists())
			try {
				FileUtils.copyInputStreamToFile(
						getClass().getResourceAsStream("demo.ldif"),
						nodeRolesFile);
			} catch (IOException e) {
				throw new CmsException("Cannot copy demo resource", e);
			}
		LdifUserAdmin nodeRoles = new LdifUserAdmin(nodeRolesFile.toURI()
				.toString(), false);
		nodeRoles.setExternalRoles(userAdmin);
		nodeRoles.init();
		// nodeRoles.createRole(KernelHeader.ROLE_ADMIN, Role.GROUP);
		userAdmin.addUserAdmin(baseNodeRoleDn, nodeRoles);

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
		authenticationManagerReg = bundleContext.registerService(
				AuthenticationManager.class, this, null);
		// userAdminServiceReg = bundleContext.registerService(
		// UserAdminService.class, userAdminService, null);
		// userDetailsManagerReg = bundleContext.registerService(
		// UserDetailsManager.class, userAdminService, null);
		userAdminReg = bundleContext.registerService(UserAdmin.class,
				userAdmin, null);
	}

	void destroy() {
		// try {
		// userAdminService.destroy();
		// } catch (RepositoryException e) {
		// log.error("Error while destroying Jackrabbit useradmin");
		// }
		// userDetailsManagerReg.unregister();
		// userAdminServiceReg.unregister();
		authenticationManagerReg.unregister();

		// userAdmin.destroy();
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

	@Override
	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
		log.error("Authentication manager is deprectaed and should not be used.");
		// Authentication auth = null;
		// if (authentication instanceof InternalAuthentication)
		// auth = internalAuth.authenticate(authentication);
		// else if (authentication instanceof AnonymousAuthenticationToken)
		// auth = anonymousAuth.authenticate(authentication);
		// else if (authentication instanceof
		// UsernamePasswordAuthenticationToken)
		// auth = userAdminService.authenticate(authentication);
		// else if (authentication instanceof OsAuthenticationToken)
		// auth = osAuth.authenticate(authentication);
		// if (auth == null)
		// throw new CmsException("Could not authenticate " + authentication);
		throw new ProviderNotFoundException(
				"Authentication manager is deprectaed and should not be used.");
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
