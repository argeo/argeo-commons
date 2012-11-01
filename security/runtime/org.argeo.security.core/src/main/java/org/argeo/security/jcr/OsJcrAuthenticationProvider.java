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
import org.argeo.security.SecurityUtils;
import org.argeo.security.core.OsAuthenticationProvider;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.UserDetails;

/** Relies on OS to authenticate and additionally setup JCR */
public class OsJcrAuthenticationProvider extends OsAuthenticationProvider {
	private Repository repository;
	private Session nodeSession;

	private UserDetails userDetails;
	private JcrSecurityModel jcrSecurityModel = new SimpleJcrSecurityModel();

	private final static String JVM_OSUSER = System.getProperty("user.name");

	public void init() {
		try {
			nodeSession = repository.login();
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot initialize", e);
		}
	}

	public void destroy() {
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
			if (!upat.getPrincipal().toString().equals(JVM_OSUSER))
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
				GrantedAuthority[] authorities = getBaseAuthorities();
				String username = JVM_OSUSER;
				Node userProfile = jcrSecurityModel.sync(nodeSession, username,
						SecurityUtils.authoritiesToStringList(authorities));
				JcrUserDetails.checkAccountStatus(userProfile);

				userDetails = new JcrUserDetails(userProfile, authen
						.getCredentials().toString(), authorities);
				authen.setDetails(userDetails);
				return authen;
			} catch (RepositoryException e) {
				JcrUtils.discardQuietly(nodeSession);
				throw new ArgeoException(
						"Unexpected exception when synchronizing OS and JCR security ",
						e);
			} finally {
				JcrUtils.logoutQuietly(nodeSession);
			}
		} else {
			throw new ArgeoException("Unsupported authentication "
					+ authentication.getClass());
		}
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setJcrSecurityModel(JcrSecurityModel jcrSecurityModel) {
		this.jcrSecurityModel = jcrSecurityModel;
	}

	@SuppressWarnings("rawtypes")
	public boolean supports(Class authentication) {
		return OsAuthenticationToken.class.isAssignableFrom(authentication)
				|| UsernamePasswordAuthenticationToken.class
						.isAssignableFrom(authentication);
	}
}