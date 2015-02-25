package org.argeo.cms;

import static org.argeo.cms.internal.kernel.KernelConstants.SPRING_SECURITY_CONTEXT_KEY;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.cms.auth.ArgeoLoginContext;
import org.argeo.security.NodeAuthenticationToken;
import org.eclipse.rap.rwt.RWT;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Gateway for user login, can also generate the related UI. */
public class CmsLogin {
	private final static Log log = LogFactory.getLog(CmsLogin.class);
	private AuthenticationManager authenticationManager;

	// private String systemKey = KernelConstants.DEFAULT_SECURITY_KEY;

	public void logInAsAnonymous() {
		Subject subject = new Subject();
		final LoginContext loginContext;
		try {
			loginContext = new ArgeoLoginContext(
					KernelHeader.LOGIN_CONTEXT_ANONYMOUS, subject);
			loginContext.login();
		} catch (LoginException e1) {
			throw new ArgeoException("Cannot authenticate anonymous", e1);
		}
	}

	public void logInWithPassword(String username, char[] password) {
		NodeAuthenticationToken token = new NodeAuthenticationToken(username,
				password);
		Authentication authentication = authenticationManager
				.authenticate(token);
		SecurityContextHolder.getContext().setAuthentication(authentication);
		HttpSession httpSession = RWT.getRequest().getSession();
		httpSession.setAttribute(SPRING_SECURITY_CONTEXT_KEY,
				SecurityContextHolder.getContext());
		if (log.isDebugEnabled())
			log.debug("Authenticated as " + authentication);
	}

	public void setAuthenticationManager(
			AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	// public void setSystemKey(String systemKey) {
	// this.systemKey = systemKey;
	// }

}
