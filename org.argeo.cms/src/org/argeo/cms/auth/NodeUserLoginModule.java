package org.argeo.cms.auth;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.osgi.service.useradmin.Authorization;

public class NodeUserLoginModule implements LoginModule, AuthConstants {
	private Subject subject;
	private Map<String, Object> sharedState = null;

//	private final static LdapName ROLE_ADMIN_NAME, ROLE_ANONYMOUS_NAME, ROLE_USER_NAME;
//	private final static List<LdapName> RESERVED_ROLES;
//	private final static X500Principal ROLE_ANONYMOUS_PRINCIPAL;
//	static {
//		try {
//			// ROLE_KERNEL_NAME = new LdapName(AuthConstants.ROLE_KERNEL);
//			ROLE_ADMIN_NAME = new LdapName(NodeConstants.ROLE_ADMIN);
//			ROLE_USER_NAME = new LdapName(NodeConstants.ROLE_USER);
//			ROLE_ANONYMOUS_NAME = new LdapName(NodeConstants.ROLE_ANONYMOUS);
//			RESERVED_ROLES = Collections.unmodifiableList(Arrays.asList(new LdapName[] { ROLE_ADMIN_NAME,
//					ROLE_ANONYMOUS_NAME, ROLE_USER_NAME, new LdapName(AuthConstants.ROLE_GROUP_ADMIN),
//					new LdapName(NodeConstants.ROLE_USER_ADMIN) }));
//			ROLE_ANONYMOUS_PRINCIPAL = new X500Principal(ROLE_ANONYMOUS_NAME.toString());
//		} catch (InvalidNameException e) {
//			throw new Error("Cannot initialize login module class", e);
//		}
//	}

	// private Authorization authorization;

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		this.subject = subject;
		this.sharedState = (Map<String, Object>) sharedState;
	}

	@Override
	public boolean login() throws LoginException {
		// if (authorization == null)
		// throw new FailedLoginException("No authorization available");
		// Iterator<Authorization> auth = subject.getPrivateCredentials(
		// Authorization.class).iterator();
		// if (!auth.hasNext())
		// throw new FailedLoginException("No authorization available");
		// authorization = auth.next();
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		Authorization authorization = (Authorization) sharedState.get(SHARED_STATE_AUTHORIZATION);
		if (authorization == null)
			throw new LoginException("Authorization should not be null");
		CmsAuthUtils.addAuthentication(subject, authorization);
		return true;
		// // required for display name:
		// subject.getPrivateCredentials().add(authorization);
		//
		// Set<Principal> principals = subject.getPrincipals();
		// try {
		// String authName = authorization.getName();
		//
		// // determine user's principal
		// final LdapName name;
		// final Principal userPrincipal;
		// if (authName == null) {
		// name = ROLE_ANONYMOUS_NAME;
		// userPrincipal = ROLE_ANONYMOUS_PRINCIPAL;
		// principals.add(userPrincipal);
		// principals.add(new AnonymousPrincipal());
		// } else {
		// name = new LdapName(authName);
		// checkUserName(name);
		// userPrincipal = new X500Principal(name.toString());
		// principals.add(userPrincipal);
		// principals.add(new ImpliedByPrincipal(ROLE_USER_NAME,
		// userPrincipal));
		// }
		//
		// // Add roles provided by authorization
		// for (String role : authorization.getRoles()) {
		// LdapName roleName = new LdapName(role);
		// if (roleName.equals(name)) {
		// // skip
		// } else {
		// checkImpliedPrincipalName(roleName);
		// principals.add(new ImpliedByPrincipal(roleName.toString(),
		// userPrincipal));
		// if (roleName.equals(ROLE_ADMIN_NAME))
		// principals.add(new AdminPrincipal(SecurityConstants.ADMIN_ID));
		// }
		// }
		//
		// return true;
		// } catch (InvalidNameException e) {
		// throw new CmsException("Cannot commit", e);
		// }
	}

	@Override
	public boolean abort() throws LoginException {
		cleanUp();
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		if (subject == null)
			throw new LoginException("Subject should not be null");
		// Clean up principals
		CmsAuthUtils.cleanUp(subject);
		// Clean up private credentials
		subject.getPrivateCredentials().clear();
		cleanUp();
		return true;
	}

	private void cleanUp() {
		subject = null;
		// authorization = null;
	}

//	private void checkUserName(LdapName name) {
//		if (RESERVED_ROLES.contains(name))
//			throw new CmsException(name + " is a reserved name");
//	}
//
//	private void checkImpliedPrincipalName(LdapName roleName) {
//		if (ROLE_USER_NAME.equals(roleName) || ROLE_ANONYMOUS_NAME.equals(roleName))
//			throw new CmsException(roleName + " cannot be listed as role");
//	}
}
