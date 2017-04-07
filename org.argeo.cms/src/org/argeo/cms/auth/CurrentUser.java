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
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.acl.Group;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.cms.internal.http.WebCmsSessionImpl;
import org.argeo.eclipse.ui.specific.UiContext;
import org.argeo.node.NodeConstants;
import org.argeo.node.security.NodeAuthenticated;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.useradmin.Authorization;

/**
 * Programmatic access to the currently authenticated user, within a CMS
 * context.
 */
public final class CurrentUser {
	private final static Log log = LogFactory.getLog(CurrentUser.class);
	private final static BundleContext bc = FrameworkUtil.getBundle(CurrentUser.class).getBundleContext();
	/*
	 * CURRENT USER API
	 */

	/**
	 * Technical username of the currently authenticated user.
	 * 
	 * @return the authenticated username or null if not authenticated /
	 *         anonymous
	 */
	public static String getUsername() {
		return getUsername(currentSubject());
	}

	/**
	 * Human readable name of the currently authenticated user (typically first
	 * name and last name).
	 */
	public static String getDisplayName() {
		return getDisplayName(currentSubject());
	}

	/** Whether a user is currently authenticated. */
	public static boolean isAnonymous() {
		return isAnonymous(currentSubject());
	}

	/** Roles of the currently logged-in user */
	public final static Set<String> roles() {
		return roles(currentSubject());
	}

	/** Returns true if the current user is in the specified role */
	public static boolean isInRole(String role) {
		Set<String> roles = roles();
		return roles.contains(role);
	}

	/** Executes as the current user */
	public final static <T> T doAs(PrivilegedAction<T> action) {
		return Subject.doAs(currentSubject(), action);
	}

	/** Executes as the current user */
	public final static <T> T tryAs(PrivilegedExceptionAction<T> action) throws PrivilegedActionException {
		return Subject.doAs(currentSubject(), action);
	}

	/*
	 * WRAPPERS
	 */

	public final static String getUsername(Subject subject) {
		if (subject == null)
			throw new CmsException("Subject cannot be null");
		if (subject.getPrincipals(X500Principal.class).size() != 1)
			return NodeConstants.ROLE_ANONYMOUS;
		Principal principal = subject.getPrincipals(X500Principal.class).iterator().next();
		return principal.getName();
	}

	public final static String getDisplayName(Subject subject) {
		return getAuthorization(subject).toString();
	}

	public final static Set<String> roles(Subject subject) {
		Set<String> roles = new HashSet<String>();
		roles.add(getUsername(subject));
		for (Principal group : subject.getPrincipals(Group.class)) {
			roles.add(group.getName());
		}
		return roles;
	}

	/** Whether this user is currently authenticated. */
	public static boolean isAnonymous(Subject subject) {
		if (subject == null)
			return true;
		String username = getUsername(subject);
		return username == null || username.equalsIgnoreCase(NodeConstants.ROLE_ANONYMOUS);
	}
	/*
	 * HELPERS
	 */

	private static Subject currentSubject() {
		NodeAuthenticated cmsView = getNodeAuthenticated();
		if (cmsView != null)
			return cmsView.getLoginContext().getSubject();
		Subject subject = Subject.getSubject(AccessController.getContext());
		if (subject != null)
			return subject;
		throw new CmsException("Cannot find related subject");
	}

	/**
	 * The node authenticated component (typically a CMS view) related to this
	 * display, or null if none is available from this call. <b>Not API: Only
	 * for low-level access.</b>
	 */
	private static NodeAuthenticated getNodeAuthenticated() {
		return UiContext.getData(NodeAuthenticated.KEY);
	}

	private static Authorization getAuthorization(Subject subject) {
		return subject.getPrivateCredentials(Authorization.class).iterator().next();
	}

	public static boolean logoutCmsSession(Subject subject) {
		UUID nodeSessionId;
		if (subject.getPrivateCredentials(CmsSessionId.class).size() == 1)
			nodeSessionId = subject.getPrivateCredentials(CmsSessionId.class).iterator().next().getUuid();
		else
			return false;
		Collection<ServiceReference<CmsSession>> srs;
		try {
			srs = bc.getServiceReferences(CmsSession.class, "(" + CmsSession.SESSION_UUID + "=" + nodeSessionId + ")");
		} catch (InvalidSyntaxException e) {
			throw new CmsException("Cannot retrieve CMS session #" + nodeSessionId, e);
		}

		if (srs.size() == 0) {
			// if (log.isTraceEnabled())
			// log.warn("No CMS web session found for http session " +
			// nodeSessionId);
			return false;
		} else if (srs.size() > 1)
			throw new CmsException(srs.size() + " CMS web sessions found for http session " + nodeSessionId);

		WebCmsSessionImpl cmsSession = (WebCmsSessionImpl) bc.getService(srs.iterator().next());
		cmsSession.cleanUp();
//		subject.getPrivateCredentials().removeAll(subject.getPrivateCredentials(CmsSessionId.class));
		if (log.isDebugEnabled())
			log.debug("Logged out CMS session " + cmsSession.getUuid());
		return true;
	}

	private CurrentUser() {
	}
}
