package org.argeo.cms.internal.kernel;

import java.io.File;
import java.io.IOException;

import javax.jcr.RepositoryException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.cms.KernelHeader;
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
import org.osgi.service.useradmin.Role;
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
	private final NodeUserAdmin userAdmin;

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

		userAdmin = new NodeUserAdmin();

		String baseDn = "dc=example,dc=com";
		String userAdminUri = KernelUtils
				.getFrameworkProp(KernelConstants.USERADMIN_URI);
		if (userAdminUri == null)
			userAdminUri = getClass().getResource(baseDn + ".ldif").toString();

		AbstractLdapUserAdmin businessRoles;
		if (userAdminUri.startsWith("ldap"))
			businessRoles = new LdapUserAdmin(userAdminUri);
		else {
			businessRoles = new LdifUserAdmin(userAdminUri);
		}
		businessRoles.init();
		userAdmin.addUserAdmin(baseDn, businessRoles);

		File osgiInstanceDir = KernelUtils.getOsgiInstanceDir();
		File homeDir = new File(osgiInstanceDir, "node");

		String baseNodeRoleDn = KernelHeader.ROLES_BASEDN;
		File nodeRolesFile = new File(homeDir, baseNodeRoleDn + ".ldif");
		try {
			FileUtils.copyInputStreamToFile(
					getClass().getResourceAsStream("demo.ldif"), nodeRolesFile);
		} catch (IOException e) {
			throw new CmsException("Cannot copy demo resource", e);
		}
		LdifUserAdmin nodeRoles = new LdifUserAdmin(nodeRolesFile.toURI()
				.toString());
		nodeRoles.setExternalRoles(userAdmin);
		nodeRoles.init();
		// nodeRoles.createRole(KernelHeader.ROLE_ADMIN, Role.GROUP);
		userAdmin.addUserAdmin(baseNodeRoleDn, nodeRoles);

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

		// userAdmin.destroy();
		userAdminReg.unregister();
	}

	@Override
	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
//		throw new UnsupportedOperationException(
//				"Authentication manager is deprectaed and should not be used.");
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
