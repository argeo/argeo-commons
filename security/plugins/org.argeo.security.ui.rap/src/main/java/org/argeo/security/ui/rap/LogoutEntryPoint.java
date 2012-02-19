package org.argeo.security.ui.rap;

import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.rwt.RWT;
import org.eclipse.rwt.lifecycle.IEntryPoint;
import org.eclipse.ui.PlatformUI;
import org.springframework.security.context.SecurityContextHolder;

/**
 * RAP entry point which logs out the currently authenticated user
 */
public class LogoutEntryPoint implements IEntryPoint {
	private final static Log log = LogFactory.getLog(LogoutEntryPoint.class);

	/**
	 * From org.springframework.security.context.
	 * HttpSessionContextIntegrationFilter
	 */
	protected static final String SPRING_SECURITY_CONTEXT_KEY = "SPRING_SECURITY_CONTEXT";

	@Override
	public int createUI() {
		// create display
		PlatformUI.createDisplay();

		final ILoginContext loginContext = SecureRapActivator
				.createLoginContext(SecureRapActivator.CONTEXT_SPRING);
		try {
			loginContext.logout();
		} catch (LoginException e) {
			e.printStackTrace();
		}

		RWT.getRequest().getSession()
				.removeAttribute(SPRING_SECURITY_CONTEXT_KEY);
		SecurityContextHolder.clearContext();
		RWT.getRequest().getSession().setMaxInactiveInterval(1);

		if (log.isDebugEnabled())
			log.debug("Logged out session " + RWT.getSessionStore().getId());
		return 0;
	}
}
