/*
 * Copyright (C) 2007-2012 Argeo GmbH
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
package org.argeo.security.ldap.jcr;

import java.util.Collection;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.UserJcrUtils;
import org.argeo.security.jcr.JcrUserDetails;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

/** @deprecated Read only mapping from LDAP to user details */
@Deprecated
public class JcrUserDetailsContextMapper implements UserDetailsContextMapper,
		ArgeoNames {
	/** Admin session on the security workspace */
	private Session securitySession;
	private Repository repository;
	private String securityWorkspace = "security";

	public void init() {
		try {
			securitySession = repository.login(securityWorkspace);
		} catch (RepositoryException e) {
			JcrUtils.logoutQuietly(securitySession);
			throw new ArgeoException(
					"Cannot initialize LDAP/JCR user details context mapper", e);
		}
	}

	public void destroy() {
		JcrUtils.logoutQuietly(securitySession);
	}

	/** Called during authentication in order to retrieve user details */
	public UserDetails mapUserFromContext(final DirContextOperations ctx,
			final String username,
			Collection<? extends GrantedAuthority> authorities) {
		if (ctx == null)
			throw new ArgeoException("No LDAP information for user " + username);
		Node userHome = UserJcrUtils.getUserHome(securitySession, username);
		if (userHome == null)
			throw new ArgeoException("No JCR information for user " + username);

		// password
		// SortedSet<?> passwordAttributes = ctx
		// .getAttributeSortedStringSet(passwordAttribute);
		// String password;
		// if (passwordAttributes == null || passwordAttributes.size() == 0) {
		// throw new ArgeoException("No password found for user " + username);
		// } else {
		// byte[] arr = (byte[]) passwordAttributes.first();
		// password = new String(arr);
		// // erase password
		// Arrays.fill(arr, (byte) 0);
		// }

		try {
			// we don't have access to password, so let's not pretend
			String password = UUID.randomUUID().toString();
			return new JcrUserDetails(userHome.getNode(ARGEO_PROFILE),
					password, authorities);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot retrieve user details for "
					+ username, e);
		}
	}

	public void mapUserToContext(UserDetails user, final DirContextAdapter ctx) {
		throw new UnsupportedOperationException("LDAP access is read-only");
	}

}
