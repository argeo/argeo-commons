package org.argeo.api.cms;

import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

import javax.security.auth.Subject;

/** An authenticated user session. */
public interface CmsSession {
	final static String USER_DN = "DN";
	final static String SESSION_UUID = "entryUUID";
	final static String SESSION_LOCAL_ID = "uniqueIdentifier";

	UUID getUuid();

	String getUserRole();

	String getUserDn();

	String getLocalId();

	String getDisplayName();
//	Authorization getAuthorization();

	Subject getSubject();

	boolean isAnonymous();

	ZonedDateTime getCreationTime();

	ZonedDateTime getEnd();

	Locale getLocale();

	boolean isValid();

	void registerView(String uid, Object view);

	void addOnCloseCallback(Consumer<CmsSession> onClose);
}
