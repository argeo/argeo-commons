package org.argeo.cms.auth;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;

import org.argeo.naming.LdapAttrs;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.useradmin.Authorization;

/** An authenticated user session. */
public interface CmsSession {
	final static String USER_DN = LdapAttrs.DN;
	final static String SESSION_UUID = LdapAttrs.entryUUID.name();
	final static String SESSION_LOCAL_ID = LdapAttrs.uniqueIdentifier.name();

	UUID getUuid();

	LdapName getUserDn();

	String getLocalId();

	Authorization getAuthorization();

	boolean isAnonymous();

	ZonedDateTime getCreationTime();

	ZonedDateTime getEnd();

	Locale getLocale();

	boolean isValid();

	/** @return The {@link CmsSession} for this {@link Subject} or null. */
	static CmsSession getCmsSession(BundleContext bc, Subject subject) {
		if (subject.getPrivateCredentials(CmsSessionId.class).isEmpty())
			return null;
		CmsSessionId cmsSessionId = subject.getPrivateCredentials(CmsSessionId.class).iterator().next();
		String uuid = cmsSessionId.getUuid().toString();
		Collection<ServiceReference<CmsSession>> sr;
		try {
			sr = bc.getServiceReferences(CmsSession.class, "(" + CmsSession.SESSION_UUID + "=" + uuid + ")");
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Cannot get CMS session for uuid " + uuid, e);
		}
		ServiceReference<CmsSession> cmsSessionRef;
		if (sr.size() == 1) {
			cmsSessionRef = sr.iterator().next();
			return bc.getService(cmsSessionRef);
		} else if (sr.size() == 0) {
			return null;
		} else
			throw new IllegalStateException(sr.size() + " CMS sessions registered for " + uuid);
	}
}
