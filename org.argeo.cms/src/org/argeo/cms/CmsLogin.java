package org.argeo.cms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.auth.ArgeoLoginContext;
import org.springframework.security.authentication.AuthenticationManager;

/**
 * Gateway for user login, can also generate the related UI.
 * 
 * @deprecated Use {@link ArgeoLoginContext} instead
 */
@Deprecated
public class CmsLogin {
	private final static Log log = LogFactory.getLog(CmsLogin.class);

	public CmsLogin() {
		log.warn("org.argeo.cms.CmsLogin is deprecated and will be removed soon.");
	}

	// private AuthenticationManager authenticationManager;
	//
	// public void logInAsAnonymous() {
	// Subject subject = new Subject();
	// final LoginContext loginContext;
	// try {
	// loginContext = new ArgeoLoginContext(
	// KernelHeader.LOGIN_CONTEXT_ANONYMOUS, subject);
	// loginContext.login();
	// } catch (LoginException e1) {
	// throw new ArgeoException("Cannot authenticate anonymous", e1);
	// }
	// }
	//
	// public void logInWithPassword(String username, char[] password) {
	// NodeAuthenticationToken token = new NodeAuthenticationToken(username,
	// password);
	// Authentication authentication = authenticationManager
	// .authenticate(token);
	// SecurityContextHolder.getContext().setAuthentication(authentication);
	// if (log.isDebugEnabled())
	// log.debug("Authenticated as " + authentication);
	// }
	//
	public void setAuthenticationManager(
			AuthenticationManager authenticationManager) {
		// this.authenticationManager = authenticationManager;
	}
}
