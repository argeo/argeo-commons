package org.argeo.security.core;

import org.argeo.ArgeoException;
import org.argeo.security.ArgeoSecurity;
import org.argeo.security.ArgeoSecurityDao;
import org.argeo.security.ArgeoSecurityService;
import org.argeo.security.ArgeoUser;
import org.argeo.security.SimpleArgeoUser;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;

public class DefaultSecurityService implements ArgeoSecurityService {
	private ArgeoSecurity argeoSecurity = new DefaultArgeoSecurity();
	private ArgeoSecurityDao securityDao;
	private AuthenticationManager authenticationManager;

	private String systemAuthenticationKey;

	public ArgeoSecurityDao getSecurityDao() {
		return securityDao;
	}

	public void newRole(String role) {
		securityDao.createRole(role, argeoSecurity.getSuperUsername());
	}

	public void updateUserPassword(String username, String password) {
		SimpleArgeoUser user = new SimpleArgeoUser(securityDao
				.getUser(username));
		user.setPassword(password);
		securityDao.update(user);
	}

	public void updateCurrentUserPassword(String oldPassword, String newPassword) {
		SimpleArgeoUser user = new SimpleArgeoUser(securityDao.getCurrentUser());
		if (!user.getPassword().equals(oldPassword))
			throw new ArgeoException("Old password is not correct.");
		user.setPassword(newPassword);
		securityDao.update(user);
	}

	public void newUser(ArgeoUser user) {
		user.getUserNatures().clear();
		argeoSecurity.beforeCreate(user);
		securityDao.create(user);
	}

	public void updateUser(ArgeoUser user) {
		String password = securityDao.getUserWithPassword(user.getUsername())
				.getPassword();
		SimpleArgeoUser simpleArgeoUser = new SimpleArgeoUser(user);
		simpleArgeoUser.setPassword(password);
		securityDao.update(simpleArgeoUser);
	}

	public TaskExecutor createSystemAuthenticatedTaskExecutor() {
		return new SimpleAsyncTaskExecutor() {
			private static final long serialVersionUID = -8126773862193265020L;

			@Override
			public Thread createThread(Runnable runnable) {
				return super
						.createThread(wrapWithSystemAuthentication(runnable));
			}

		};
	}

	/**
	 * Wraps another runnable, adding security context <br/>
	 * TODO: secure the call to this method with Java Security
	 */
	public Runnable wrapWithSystemAuthentication(final Runnable runnable) {
		return new Runnable() {

			public void run() {
				SecurityContext securityContext = SecurityContextHolder
						.getContext();
				Authentication auth = authenticationManager
						.authenticate(new InternalAuthentication(
								systemAuthenticationKey));
				securityContext.setAuthentication(auth);

				runnable.run();
			}
		};
	}

	public void setArgeoSecurity(ArgeoSecurity argeoSecurity) {
		this.argeoSecurity = argeoSecurity;
	}

	public void setSecurityDao(ArgeoSecurityDao dao) {
		this.securityDao = dao;
	}

	public void setAuthenticationManager(
			AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	public void setSystemAuthenticationKey(String systemAuthenticationKey) {
		this.systemAuthenticationKey = systemAuthenticationKey;
	}

}
