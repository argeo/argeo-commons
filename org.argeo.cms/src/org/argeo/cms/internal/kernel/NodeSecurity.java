package org.argeo.cms.internal.kernel;

import java.net.URL;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.cms.internal.useradmin.JcrUserAdmin;
import org.argeo.cms.internal.useradmin.SimpleJcrSecurityModel;
import org.argeo.cms.internal.useradmin.jackrabbit.JackrabbitUserAdminService;
import org.argeo.security.UserAdminService;
import org.argeo.security.core.InternalAuthentication;
import org.argeo.security.core.InternalAuthenticationProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.useradmin.UserAdmin;
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
	private final JackrabbitUserAdminService userAdminService;
	private final JcrUserAdmin userAdmin;

	private ServiceRegistration<AuthenticationManager> authenticationManagerReg;
	private ServiceRegistration<UserAdminService> userAdminServiceReg;
	private ServiceRegistration<UserDetailsManager> userDetailsManagerReg;

	private ServiceRegistration<UserAdmin> userAdminReg;

	public NodeSecurity(BundleContext bundleContext, JackrabbitNode node)
			throws RepositoryException {
		URL url = getClass().getClassLoader().getResource(
				KernelConstants.JAAS_CONFIG);
		System.setProperty("java.security.auth.login.config",
				url.toExternalForm());

		this.bundleContext = bundleContext;

		internalAuth = new InternalAuthenticationProvider(
				Activator.getSystemKey());
		anonymousAuth = new AnonymousAuthenticationProvider(
				Activator.getSystemKey());

		// user admin
		userAdminService = new JackrabbitUserAdminService();
		userAdminService.setRepository(node);
		userAdminService.setSecurityModel(new SimpleJcrSecurityModel());
		userAdminService.init();

		userAdmin = new JcrUserAdmin(bundleContext, node);
		userAdmin.setUserAdminService(userAdminService);
	}

	public void publish() {
		authenticationManagerReg = bundleContext.registerService(
				AuthenticationManager.class, this, null);
		userAdminServiceReg = bundleContext.registerService(
				UserAdminService.class, userAdminService, null);
		userDetailsManagerReg = bundleContext.registerService(
				UserDetailsManager.class, userAdminService, null);
		userAdminReg = bundleContext.registerService(UserAdmin.class,
				userAdmin, null);
	}

	void destroy() {
		try {
			userAdminService.destroy();
		} catch (RepositoryException e) {
			log.error("Error while destroying Jackrabbit useradmin");
		}
		userDetailsManagerReg.unregister();
		userAdminServiceReg.unregister();
		authenticationManagerReg.unregister();

		userAdmin.destroy();
		userAdminReg.unregister();
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
			auth = userAdminService.authenticate(authentication);
		if (auth == null)
			throw new CmsException("Could not authenticate " + authentication);
		return auth;
	}
}
