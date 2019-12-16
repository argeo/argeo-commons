package org.argeo.cms.auth;

import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.UUID;

import javax.naming.ldap.LdapName;

import org.argeo.naming.LdapAttrs;
import org.osgi.service.useradmin.Authorization;

/** An authenticated user session. */
public interface CmsSession {
	final static String USER_DN = LdapAttrs.DN;
	final static String SESSION_UUID = LdapAttrs.entryUUID.name();
	final static String SESSION_LOCAL_ID = LdapAttrs.uniqueIdentifier.name();

	// public String getId();

	UUID getUuid();

	LdapName getUserDn();

	String getLocalId();

	Authorization getAuthorization();

	boolean isAnonymous();

	ZonedDateTime getCreationTime();

	ZonedDateTime getEnd();

	Locale getLocale();

	boolean isValid();

	// public Session getDataSession(String cn, String workspace, Repository
	// repository);
	//
	// public void releaseDataSession(String cn, Session session);

	// public void addHttpSession(HttpServletRequest request);

	// public void cleanUp();
}
