/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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
package org.argeo.security.jcr;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.OsAuthenticationToken;
import org.argeo.security.core.OsAuthenticationProvider;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.UserDetails;

/** Relies on OS to authenticate and additionally setup JCR */
public class OsJcrAuthenticationProvider extends OsAuthenticationProvider {
	private Repository repository;
	private String securityWorkspace = "security";
	private Session securitySession;
	private Session nodeSession;

	private UserDetails userDetails;

	public void init() {
		try {
			securitySession = repository.login(securityWorkspace);
			nodeSession = repository.login();
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot initialize", e);
		}
	}

	public void destroy() {
		JcrUtils.logoutQuietly(securitySession);
		JcrUtils.logoutQuietly(nodeSession);
	}

	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
		if (authentication instanceof UsernamePasswordAuthenticationToken) {
			// deal with remote access to internal server
			// FIXME very primitive and unsecure at this stage
			// consider using the keyring for username / password authentication
			// or certificate
			UsernamePasswordAuthenticationToken upat = (UsernamePasswordAuthenticationToken) authentication;
			if (!upat.getPrincipal().toString()
					.equals(System.getProperty("user.name")))
				throw new BadCredentialsException("Wrong credentials");
			UsernamePasswordAuthenticationToken authen = new UsernamePasswordAuthenticationToken(
					authentication.getPrincipal(),
					authentication.getCredentials(), getBaseAuthorities());
			authen.setDetails(userDetails);
			return authen;
		} else if (authentication instanceof OsAuthenticationToken) {
			OsAuthenticationToken authen = (OsAuthenticationToken) super
					.authenticate(authentication);
			try {
				// WARNING: at this stage we assume that the java properties
				// will have the same value
				String username = System.getProperty("user.name");
				Node userProfile = JcrUtils.createUserProfileIfNeeded(
						securitySession, username);
				JcrUserDetails.checkAccountStatus(userProfile);

				// each user should have a writable area in the default
				// workspace of the node
				JcrUtils.createUserHomeIfNeeded(nodeSession, username);
				userDetails = new JcrUserDetails(userProfile, authen
						.getCredentials().toString(), getBaseAuthorities());
				authen.setDetails(userDetails);
				return authen;
			} catch (RepositoryException e) {
				JcrUtils.discardQuietly(securitySession);
				throw new ArgeoException(
						"Unexpected exception when synchronizing OS and JCR security ",
						e);
			} finally {
				JcrUtils.logoutQuietly(securitySession);
			}
		} else {
			throw new ArgeoException("Unsupported authentication "
					+ authentication.getClass());
		}
	}

	public void setSecurityWorkspace(String securityWorkspace) {
		this.securityWorkspace = securityWorkspace;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	@SuppressWarnings("rawtypes")
	public boolean supports(Class authentication) {
		return OsAuthenticationToken.class.isAssignableFrom(authentication)
				|| UsernamePasswordAuthenticationToken.class
						.isAssignableFrom(authentication);
	}

}
