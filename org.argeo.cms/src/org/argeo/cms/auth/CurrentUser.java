package org.argeo.cms.auth;

import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsSession;
import org.argeo.api.cms.CmsSessionId;
import org.argeo.cms.internal.auth.CmsSessionImpl;
import org.argeo.cms.internal.auth.ImpliedByPrincipal;
import org.argeo.cms.internal.runtime.CmsContextImpl;
import org.argeo.util.CurrentSubject;
import org.osgi.service.useradmin.Authorization;

/**
 * Programmatic access to the currently authenticated user, within a CMS
 * context.
 */
public final class CurrentUser {
	/*
	 * CURRENT USER API
	 */

	/**
	 * Technical username of the currently authenticated user.
	 * 
	 * @return the authenticated username or null if not authenticated / anonymous
	 */
	public static String getUsername() {
		return getUsername(currentSubject());
	}

	/**
	 * Human readable name of the currently authenticated user (typically first name
	 * and last name).
	 */
	public static String getDisplayName() {
		return getDisplayName(currentSubject());
	}

	/** Whether a user is currently authenticated. */
	public static boolean isAnonymous() {
		return isAnonymous(currentSubject());
	}

	/** Locale of the current user */
	public final static Locale locale() {
		return locale(currentSubject());
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

	/** Implies this {@link SystemRole} in this context. */
	public final static boolean implies(SystemRole role, String context) {
		return role.implied(currentSubject(), context);
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
			throw new IllegalArgumentException("Subject cannot be null");
		if (subject.getPrincipals(X500Principal.class).size() != 1)
			return CmsConstants.ROLE_ANONYMOUS;
		Principal principal = subject.getPrincipals(X500Principal.class).iterator().next();
		return principal.getName();
	}

	public final static String getDisplayName(Subject subject) {
		return getAuthorization(subject).toString();
	}

	public final static Set<String> roles(Subject subject) {
		Set<String> roles = new HashSet<String>();
		roles.add(getUsername(subject));
		for (Principal group : subject.getPrincipals(ImpliedByPrincipal.class)) {
			roles.add(group.getName());
		}
		return roles;
	}

	public final static Locale locale(Subject subject) {
		Set<Locale> locales = subject.getPublicCredentials(Locale.class);
		if (locales.isEmpty()) {
			Locale defaultLocale = CmsContextImpl.getCmsContext().getDefaultLocale();
			return defaultLocale;
		} else
			return locales.iterator().next();
	}

	/** Whether this user is currently authenticated. */
	public static boolean isAnonymous(Subject subject) {
		if (subject == null)
			return true;
		String username = getUsername(subject);
		return username == null || username.equalsIgnoreCase(CmsConstants.ROLE_ANONYMOUS);
	}

	public static CmsSession getCmsSession() {
		Subject subject = currentSubject();
		Iterator<CmsSessionId> it = subject.getPrivateCredentials(CmsSessionId.class).iterator();
		if (!it.hasNext())
			throw new IllegalStateException("No CMS session id available for " + subject);
		CmsSessionId cmsSessionId = it.next();
		if (it.hasNext())
			throw new IllegalStateException("More than one CMS session id available for " + subject);
		return CmsContextImpl.getCmsContext().getCmsSessionByUuid(cmsSessionId.getUuid());
	}

	/*
	 * HELPERS
	 */
	private static Subject currentSubject() {
		return CurrentSubject.current();
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
		CmsSessionImpl cmsSession = CmsContextImpl.getCmsContext().getCmsSessionByUuid(nodeSessionId);

		// FIXME logout all views
		// TODO check why it is sometimes null
		if (cmsSession != null)
			cmsSession.close();
		// if (log.isDebugEnabled())
		// log.debug("Logged out CMS session " + cmsSession.getUuid());
		return true;
	}

	/** singleton */
	private CurrentUser() {
	}
}
