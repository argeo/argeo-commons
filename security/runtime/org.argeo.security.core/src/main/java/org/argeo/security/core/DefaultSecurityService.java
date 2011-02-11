/*
 * Copyright (C) 2010 Mathieu Baudier <mbaudier@argeo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.argeo.security.core;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.argeo.ArgeoException;
import org.argeo.security.ArgeoSecurity;
import org.argeo.security.ArgeoSecurityDao;
import org.argeo.security.ArgeoSecurityService;
import org.argeo.security.ArgeoUser;
import org.argeo.security.SimpleArgeoUser;
import org.argeo.security.UserNature;
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

	public ArgeoUser getCurrentUser() {
		ArgeoUser argeoUser = ArgeoUserDetails.securityContextUser();
		if (argeoUser == null)
			return null;
		if (argeoUser.getRoles().contains(securityDao.getDefaultRole()))
			argeoUser.getRoles().remove(securityDao.getDefaultRole());
		return argeoUser;
	}

	public ArgeoSecurityDao getSecurityDao() {
		return securityDao;
	}

	public void newRole(String role) {
		securityDao.createRole(role, argeoSecurity.getSuperUsername());
	}

	public void updateUserPassword(String username, String password) {
		SimpleArgeoUser user = new SimpleArgeoUser(
				securityDao.getUser(username));
		user.setPassword(securityDao.encodePassword(password));
		securityDao.updateUser(user);
	}

	public void updateCurrentUserPassword(String oldPassword, String newPassword) {
		SimpleArgeoUser user = new SimpleArgeoUser(getCurrentUser());
		if (!securityDao.isPasswordValid(user.getPassword(), oldPassword))
			throw new ArgeoException("Old password is not correct.");
		user.setPassword(securityDao.encodePassword(newPassword));
		securityDao.updateUser(user);
	}

	public void newUser(ArgeoUser user) {
		argeoSecurity.beforeCreate(user);
		// normalize password
		if (user instanceof SimpleArgeoUser) {
			if (user.getPassword() == null || user.getPassword().equals(""))
				((SimpleArgeoUser) user).setPassword(securityDao
						.encodePassword(user.getUsername()));
			else if (!user.getPassword().startsWith("{"))
				((SimpleArgeoUser) user).setPassword(securityDao
						.encodePassword(user.getPassword()));
		}
		securityDao.createUser(user);
	}

	public ArgeoUser getUser(String username) {
		return securityDao.getUser(username);
	}

	public Boolean userExists(String username) {
		return securityDao.userExists(username);
	}

	public void updateUser(ArgeoUser user) {
		String password = user.getPassword();
		if (password == null)
			password = securityDao.getUserWithPassword(user.getUsername())
					.getPassword();
		if (!password.startsWith("{"))
			password = securityDao.encodePassword(user.getPassword());
		SimpleArgeoUser simpleArgeoUser = new SimpleArgeoUser(user);
		simpleArgeoUser.setPassword(password);
		securityDao.updateUser(simpleArgeoUser);
	}

	public void deleteUser(String username) {
		securityDao.deleteUser(username);

	}

	public void deleteRole(String role) {
		securityDao.deleteRole(role);
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

	public Set<ArgeoUser> listUsersInRole(String role) {
		Set<ArgeoUser> lst = securityDao.listUsersInRole(role);
		Iterator<ArgeoUser> it = lst.iterator();
		while (it.hasNext()) {
			if (it.next().getUsername()
					.equals(argeoSecurity.getSuperUsername())) {
				it.remove();
				break;
			}
		}
		return lst;
	}

	public void updateCurrentUserNatures(Map<String, UserNature> userNatures) {
		// TODO Auto-generated method stub

	}

	public Set<ArgeoUser> listUsers() {
		return securityDao.listUsers();
	}

	public Set<String> listEditableRoles() {
		// TODO Auto-generated method stub
		return securityDao.listEditableRoles();
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
