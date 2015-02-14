package org.argeo.cms;

import static org.argeo.cms.internal.kernel.KernelConstants.SPRING_SECURITY_CONTEXT_KEY;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.internal.kernel.KernelConstants;
import org.eclipse.rap.rwt.RWT;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

/** Gateway for user login, can also generate the related UI. */
public class CmsLogin {
	private final static Log log = LogFactory.getLog(CmsLogin.class);
	private AuthenticationManager authenticationManager;
	private String systemKey = KernelConstants.DEFAULT_SECURITY_KEY;

	public void logInAsAnonymous() {
		// TODO Better deal with anonymous authentication
		try {
			List<SimpleGrantedAuthority> anonAuthorities = Collections
					.singletonList(new SimpleGrantedAuthority(
							KernelHeader.USERNAME_ANONYMOUS));
			UserDetails anonUser = new User("anonymous", "", true, true, true,
					true, anonAuthorities);
			AnonymousAuthenticationToken anonToken = new AnonymousAuthenticationToken(
					systemKey, anonUser, anonAuthorities);
			Authentication authentication = authenticationManager
					.authenticate(anonToken);
			SecurityContextHolder.getContext()
					.setAuthentication(authentication);
		} catch (Exception e) {
			throw new CmsException("Cannot authenticate", e);
		}
	}

	public void logInWithPassword(String username, char[] password) {
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
				username, password);
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

	public void setSystemKey(String systemKey) {
		this.systemKey = systemKey;
	}

}
