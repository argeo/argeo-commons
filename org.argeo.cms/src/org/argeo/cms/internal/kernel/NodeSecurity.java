package org.argeo.cms.internal.kernel;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.cms.internal.useradmin.SimpleJcrSecurityModel;
import org.argeo.cms.internal.useradmin.jackrabbit.JackrabbitUserAdminService;
import org.argeo.osgi.useradmin.AbstractLdapUserAdmin;
import org.argeo.osgi.useradmin.LdapUserAdmin;
import org.argeo.osgi.useradmin.LdifUserAdmin;
import org.argeo.security.OsAuthenticationToken;
import org.argeo.security.UserAdminService;
import org.argeo.security.core.InternalAuthentication;
import org.argeo.security.core.InternalAuthenticationProvider;
import org.argeo.security.core.OsAuthenticationProvider;
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

	private final OsAuthenticationProvider osAuth;
	private final InternalAuthenticationProvider internalAuth;
	private final AnonymousAuthenticationProvider anonymousAuth;
	private final JackrabbitUserAdminService userAdminService;
	private final AbstractLdapUserAdmin userAdmin;

	private ServiceRegistration<AuthenticationManager> authenticationManagerReg;
	private ServiceRegistration<UserAdminService> userAdminServiceReg;
	private ServiceRegistration<UserDetailsManager> userDetailsManagerReg;

	private ServiceRegistration<UserAdmin> userAdminReg;

	public NodeSecurity(BundleContext bundleContext, JackrabbitNode node)
			throws RepositoryException {
		this.bundleContext = bundleContext;

		osAuth = new OsAuthenticationProvider();
		internalAuth = new InternalAuthenticationProvider(
				Activator.getSystemKey());
		anonymousAuth = new AnonymousAuthenticationProvider(
				Activator.getSystemKey());

		// user admin
		userAdminService = new JackrabbitUserAdminService();
		userAdminService.setRepository(node);
		userAdminService.setSecurityModel(new SimpleJcrSecurityModel());
		userAdminService.init();

		String userAdminUri = KernelUtils
				.getFrameworkProp(KernelConstants.USERADMIN_URI);
		if (userAdminUri == null)
			userAdminUri = getClass().getResource("demo.ldif").toString();

		if (userAdminUri.startsWith("ldap"))
			userAdmin = new LdapUserAdmin(userAdminUri);
		else
			userAdmin = new LdifUserAdmin(userAdminUri);
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
		else if (authentication instanceof OsAuthenticationToken)
			auth = osAuth.authenticate(authentication);
		if (auth == null)
			throw new CmsException("Could not authenticate " + authentication);
		return auth;
	}
}
