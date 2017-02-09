package org.argeo.cms.auth;

import java.util.UUID;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.naming.ldap.LdapName;

import org.argeo.naming.LdapAttrs;
import org.osgi.service.useradmin.Authorization;

public interface CmsSession {
	public final static String USER_DN = LdapAttrs.DN;
	public final static String SESSION_UUID = LdapAttrs.entryUUID.name();
	public final static String SESSION_LOCAL_ID = LdapAttrs.uniqueIdentifier.name();

	// public String getId();

	public UUID getUuid();

	public LdapName getUserDn();

	public String getLocalId();

	public Authorization getAuthorization();

	public Session getDataSession(String cn, String workspace, Repository repository);

	public void releaseDataSession(String cn, Session session);

	// public void addHttpSession(HttpServletRequest request);

	// public void cleanUp();
}
