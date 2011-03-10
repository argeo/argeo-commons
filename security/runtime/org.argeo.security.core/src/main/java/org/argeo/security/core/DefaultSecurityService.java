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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.argeo.security.ArgeoSecurity;
import org.argeo.security.ArgeoSecurityDao;
import org.argeo.security.ArgeoSecurityService;
import org.argeo.security.ArgeoUser;
import org.argeo.security.SimpleArgeoUser;
import org.argeo.security.UserAdminService;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;

public class DefaultSecurityService extends DefaultCurrentUserService implements
		UserAdminService, ArgeoSecurityService {
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
		SimpleArgeoUser user = new SimpleArgeoUser(
				securityDao.getUser(username));
		user.setPassword(encodePassword(password));
		securityDao.updateUser(user);
	}

	public void newUser(ArgeoUser user) {
		argeoSecurity.beforeCreate(user);
		// normalize password
		if (user instanceof SimpleArgeoUser) {
			if (user.getPassword() == null || user.getPassword().equals(""))
				((SimpleArgeoUser) user).setPassword(encodePassword(user
						.getUsername()));
			else if (!user.getPassword().startsWith("{"))
				((SimpleArgeoUser) user).setPassword(encodePassword(user
						.getPassword()));
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
			password = encodePassword(user.getPassword());
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

	@Deprecated
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
	@Deprecated
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
		Set<ArgeoUser> lst = new HashSet<ArgeoUser>(
				securityDao.listUsersInRole(role));
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
		setCurrentUserDao(dao);
	}

	public void setAuthenticationManager(
			AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	public void setSystemAuthenticationKey(String systemAuthenticationKey) {
		this.systemAuthenticationKey = systemAuthenticationKey;
	}
}
