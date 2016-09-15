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
package org.argeo.cms.auth;

import java.security.AccessController;
import java.security.Principal;
import java.security.acl.Group;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import org.argeo.cms.CmsException;
import org.argeo.eclipse.ui.specific.UiContext;
import org.argeo.node.NodeAuthenticated;
import org.osgi.service.useradmin.Authorization;

/** Static utilities */
public final class CurrentUser {
	/**
	 * @return the authenticated username or null if not authenticated /
	 *         anonymous
	 */
	public static String getUsername() {
		return getUsername(currentSubject());
	}

	public static String getDisplayName() {
		return getDisplayName(currentSubject());
	}

	public static boolean isAnonymous() {
		return isAnonymous(currentSubject());
	}

	public static boolean isAnonymous(Subject subject) {
		String username = getUsername(subject);
		return username == null
				|| username.equalsIgnoreCase(AuthConstants.ROLE_ANONYMOUS);
	}

	private static Subject currentSubject() {
		NodeAuthenticated cmsView = getNodeAuthenticated();
		if (cmsView != null)
			return cmsView.getSubject();
		Subject subject = Subject.getSubject(AccessController.getContext());
		if (subject != null)
			return subject;
		throw new CmsException("Cannot find related subject");
	}

	/**
	 * The node authenticated component (typically a CMS view) related to this
	 * display, or null if none is available from this call.
	 */
	public static NodeAuthenticated getNodeAuthenticated() {
		return UiContext.getData(NodeAuthenticated.KEY);
	}

	public final static String getUsername(Subject subject) {
		if (subject.getPrincipals(X500Principal.class).size() != 1)
			return null;
		Principal principal = subject.getPrincipals(X500Principal.class)
				.iterator().next();
		return principal.getName();
	}

	public final static String getDisplayName(Subject subject) {
		return getAuthorization(subject).toString();
	}

	private static Authorization getAuthorization(Subject subject) {
		return subject.getPrivateCredentials(Authorization.class).iterator()
				.next();
	}

	public final static Set<String> roles() {
		return roles(currentSubject());
	}

	public final static Set<String> roles(Subject subject) {
		Set<String> roles = new HashSet<String>();
		X500Principal userPrincipal = subject
				.getPrincipals(X500Principal.class).iterator().next();
		roles.add(userPrincipal.getName());
		for (Principal group : subject.getPrincipals(Group.class)) {
			roles.add(group.getName());
		}
		return roles;
	}

	private CurrentUser() {
	}
}
