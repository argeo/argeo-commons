package org.argeo.cms.internal.kernel;

import javax.jcr.RepositoryException;
import javax.security.auth.spi.LoginModule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.security.UserAdminService;
import org.argeo.security.core.InternalAuthentication;
import org.argeo.security.core.InternalAuthenticationProvider;
import org.argeo.security.core.ThreadedLoginModule;
import org.argeo.security.jcr.SimpleJcrSecurityModel;
import org.argeo.security.jcr.jackrabbit.JackrabbitUserAdminService;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.security.authentication.AnonymousAuthenticationProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.provisioning.UserDetailsManager;

/** Authentication and user management. */
class NodeSecurity implements AuthenticationManager {
	private final static Log log = LogFactory.getLog(NodeSecurity.class);

	private final BundleContext bundleContext;

	private final InternalAuthenticationProvider internalAuth;
	private final AnonymousAuthenticationProvider anonymousAuth;
	private final JackrabbitUserAdminService jackrabbitUserAdmin;
	private Login loginModule;

	private ServiceRegistration<AuthenticationManager> authenticationManagerReg;
	private ServiceRegistration<UserAdminService> userAdminReg;
	private ServiceRegistration<UserDetailsManager> userDetailsManagerReg;
	private ServiceRegistration<LoginModule> loginModuleReg;

	public NodeSecurity(BundleContext bundleContext, JackrabbitNode node)
			throws RepositoryException {
		this.bundleContext = bundleContext;

		internalAuth = new InternalAuthenticationProvider(
				KernelConstants.DEFAULT_SECURITY_KEY);
		anonymousAuth = new AnonymousAuthenticationProvider(
				KernelConstants.DEFAULT_SECURITY_KEY);

		// user admin
		jackrabbitUserAdmin = new JackrabbitUserAdminService();
		jackrabbitUserAdmin.setRepository(node);
		jackrabbitUserAdmin.setSecurityModel(new SimpleJcrSecurityModel());
		jackrabbitUserAdmin.init();

		loginModule = new Login();
	}

	public void publish() {
		authenticationManagerReg = bundleContext.registerService(
				AuthenticationManager.class, this, null);
		userAdminReg = bundleContext.registerService(UserAdminService.class,
				jackrabbitUserAdmin, null);
		userDetailsManagerReg = bundleContext.registerService(
				UserDetailsManager.class, jackrabbitUserAdmin, null);
		// userAdminReg =
		// bundleContext.registerService(UserDetailsService.class,
		// jackrabbitUserAdmin, null);

		loginModuleReg = bundleContext.registerService(LoginModule.class,
				loginModule, null);
	}

	void destroy() {
		try {
			jackrabbitUserAdmin.destroy();
		} catch (RepositoryException e) {
			log.error("Error while destroying Jackrabbit useradmin");
		}
		userDetailsManagerReg.unregister();
		userAdminReg.unregister();
		authenticationManagerReg.unregister();
		loginModuleReg.unregister();
	}

	@Override
	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
		Authentication auth = null;
		if (authentication instanceof InternalAuthentication)
			auth = internalAuth.authenticate(authentication);
		else if (authentication instanceof AnonymousAuthenticationToken)
			auth = anonymousAuth.authenticate(authentication);
		else if (authentication instanceof UsernamePasswordAuthenticationToken)
			auth = jackrabbitUserAdmin.authenticate(authentication);
		if (auth == null)
			throw new CmsException("Could not authenticate " + authentication);
		return auth;
	}

	private class Login extends ThreadedLoginModule {

		@Override
		protected LoginModule createLoginModule() {
			SpringLoginModule springLoginModule = new SpringLoginModule();
			springLoginModule.setAuthenticationManager(NodeSecurity.this);
			if (Display.getCurrent() != null) {

			}
			return springLoginModule;
		}

	}
}
