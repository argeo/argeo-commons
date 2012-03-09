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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;

import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.NodeAuthenticationToken;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.providers.AuthenticationProvider;

/** Connects to a JCR repository and delegates authentication to it. */
public class RemoteJcrAuthenticationProvider implements AuthenticationProvider,
		ArgeoNames {
	private RepositoryFactory repositoryFactory;

	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
		NodeAuthenticationToken siteAuth = (NodeAuthenticationToken) authentication;
		String url = siteAuth.getUrl();
		if (url == null)
			return null;
		Session session;
		Node userProfile;

		try {
			SimpleCredentials sp = new SimpleCredentials(siteAuth.getName(),
					siteAuth.getCredentials().toString().toCharArray());
			// get repository
			Repository repository = getRepository(url, sp);
			if (repository == null)
				return null;

			String workspace = siteAuth.getSecurityWorkspace();
			session = repository.login(sp, workspace);
			Node userHome = JcrUtils.getUserHome(session);
			if (userHome == null || !userHome.hasNode(ArgeoNames.ARGEO_PROFILE))
				throw new ArgeoException("No profile for user "
						+ siteAuth.getName() + " in security workspace "
						+ siteAuth.getSecurityWorkspace() + " of "
						+ siteAuth.getUrl());
			userProfile = userHome.getNode(ArgeoNames.ARGEO_PROFILE);
		} catch (RepositoryException e) {
			throw new BadCredentialsException(
					"Cannot authenticate " + siteAuth, e);
		}

		try {
			JcrUserDetails.checkAccountStatus(userProfile);
			// retrieve remote roles
			List<GrantedAuthority> authoritiesList = new ArrayList<GrantedAuthority>();
			if (userProfile.hasProperty(ArgeoNames.ARGEO_REMOTE_ROLES)) {
				Value[] roles = userProfile.getProperty(
						ArgeoNames.ARGEO_REMOTE_ROLES).getValues();
				for (int i = 0; i < roles.length; i++)
					authoritiesList.add(new GrantedAuthorityImpl(roles[i]
							.getString()));
			}

			// create authenticated objects
			GrantedAuthority[] authorities = authoritiesList
					.toArray(new GrantedAuthority[authoritiesList.size()]);
			JcrUserDetails userDetails = new JcrUserDetails(userProfile,
					siteAuth.getCredentials().toString(), authorities);
			NodeAuthenticationToken authenticated = new NodeAuthenticationToken(
					siteAuth, authorities);
			authenticated.setDetails(userDetails);
			return authenticated;
		} catch (RepositoryException e) {
			throw new ArgeoException(
					"Unexpected exception when authenticating to " + url, e);
		}
	}

	protected Repository getRepository(String url, Credentials credentials)
			throws RepositoryException {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(ArgeoJcrConstants.JCR_REPOSITORY_URI, url);
		return repositoryFactory.getRepository(parameters);
	}

	@SuppressWarnings("rawtypes")
	public boolean supports(Class authentication) {
		return NodeAuthenticationToken.class.isAssignableFrom(authentication);
	}

	public void setRepositoryFactory(RepositoryFactory repositoryFactory) {
		this.repositoryFactory = repositoryFactory;
	}

}
