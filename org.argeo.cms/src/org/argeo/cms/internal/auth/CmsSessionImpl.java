package org.argeo.cms.internal.auth;

import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.cms.auth.CmsSession;
import org.argeo.jcr.JcrUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.useradmin.Authorization;

public class CmsSessionImpl implements CmsSession {
	private final static BundleContext bc = FrameworkUtil.getBundle(CmsSessionImpl.class).getBundleContext();
	private final static Log log = LogFactory.getLog(CmsSessionImpl.class);

	private final Subject initialSubject;
	private final UUID uuid;
	private final String localSessionId;
	private final Authorization authorization;
	private final LdapName userDn;

	private ServiceRegistration<CmsSession> serviceRegistration;

	private Map<String, Session> dataSessions = new HashMap<>();
	private Set<String> dataSessionsInUse = new HashSet<>();
	private LinkedHashSet<Session> additionalDataSessions = new LinkedHashSet<>();

	public CmsSessionImpl(Subject initialSubject, Authorization authorization, String localSessionId) {
		this.initialSubject = initialSubject;
		this.localSessionId = localSessionId;
		this.authorization = authorization;
		try {
			this.userDn = new LdapName(authorization.getName());
		} catch (InvalidNameException e) {
			throw new CmsException("Invalid user name " + authorization.getName(), e);
		}
		this.uuid = UUID.randomUUID();
		// register as service
		Hashtable<String, String> props = new Hashtable<>();
		props.put(CmsSession.USER_DN, authorization.getName());
		props.put(CmsSession.SESSION_UUID, uuid.toString());
		props.put(CmsSession.SESSION_LOCAL_ID, localSessionId);
		serviceRegistration = bc.registerService(CmsSession.class, this, props);
	}

	public synchronized void cleanUp() {
		serviceRegistration.unregister();

		// TODO check data session in use ?
		for (String path : dataSessions.keySet())
			JcrUtils.logoutQuietly(dataSessions.get(path));
		for (Session session : additionalDataSessions)
			JcrUtils.logoutQuietly(session);
		notifyAll();
	}

	@Override
	public synchronized Session getDataSession(String cn, String workspace, Repository repository) {
		String path = cn + '/' + workspace;
		if (dataSessionsInUse.contains(path)) {
			try {
				wait(1000);
				if (dataSessionsInUse.contains(path)) {
					Session session = login(repository, workspace);
					additionalDataSessions.add(session);
					if (log.isTraceEnabled())
						log.trace("Additional data session " + path + " for " + userDn);
					return session;
				}
			} catch (InterruptedException e) {
				// silent
			}
		}

		Session session = null;
		if (dataSessions.containsKey(path)) {
			session = dataSessions.get(path);
		} else {
			session = login(repository, workspace);
			dataSessions.put(path, session);
			if (log.isTraceEnabled())
				log.trace("New data session " + path + " for " + userDn);
		}
		dataSessionsInUse.add(path);
		return session;
	}

	private Session login(Repository repository, String workspace) {
		try {
			return Subject.doAs(initialSubject, new PrivilegedExceptionAction<Session>() {
				@Override
				public Session run() throws Exception {
					return repository.login(workspace);
				}
			});
		} catch (Exception e) {
			throw new CmsException("Cannot log in " + userDn + " to JCR", e);
		}
	}

	@Override
	public synchronized void releaseDataSession(String cn, Session session) {
		if (additionalDataSessions.contains(session)) {
			JcrUtils.logoutQuietly(session);
			additionalDataSessions.remove(session);
			return;
		}
		String path = cn + '/' + session.getWorkspace().getName();
		if (!dataSessionsInUse.contains(path))
			log.warn("Data session " + path + " was not in use for " + userDn);
		dataSessionsInUse.remove(path);
		Session registeredSession = dataSessions.get(path);
		if (session != registeredSession)
			log.warn("Data session " + path + " not consistent for " + userDn);
		notifyAll();
	}

	@Override
	public Authorization getAuthorization() {
		return authorization;
	}

	@Override
	public UUID getUuid() {
		return uuid;
	}

	public Subject getInitialSubject() {
		return initialSubject;
	}

	public String getLocalSessionId() {
		return localSessionId;
	}

	public ServiceRegistration<CmsSession> getServiceRegistration() {
		return serviceRegistration;
	}

	@Override
	public LdapName getUserDn() {
		return userDn;
	}

	@Override
	public String getLocalId() {
		return localSessionId;
	}

	public String toString() {
		return "CMS Session #" + localSessionId;
	}

	public static CmsSession getByLocalId(String localId) {
		Collection<ServiceReference<CmsSession>> sr;
		try {
			sr = bc.getServiceReferences(CmsSession.class, "(" + CmsSession.SESSION_LOCAL_ID + "=" + localId + ")");
		} catch (InvalidSyntaxException e) {
			throw new CmsException("Cannot get CMS session for id " + localId, e);
		}
		ServiceReference<CmsSession> cmsSessionRef;
		if (sr.size() == 1) {
			cmsSessionRef = sr.iterator().next();
			return bc.getService(cmsSessionRef);
		} else if (sr.size() == 0) {
			return null;
		} else
			throw new CmsException(sr.size() + " CMS sessions registered for " + localId);

	}

	public static CmsSession getByUuid(String uuid) {
		Collection<ServiceReference<CmsSession>> sr;
		try {
			sr = bc.getServiceReferences(CmsSession.class, "(" + CmsSession.SESSION_UUID + "=" + uuid + ")");
		} catch (InvalidSyntaxException e) {
			throw new CmsException("Cannot get CMS session for uuid " + uuid, e);
		}
		ServiceReference<CmsSession> cmsSessionRef;
		if (sr.size() == 1) {
			cmsSessionRef = sr.iterator().next();
			return bc.getService(cmsSessionRef);
		} else if (sr.size() == 0) {
			return null;
		} else
			throw new CmsException(sr.size() + " CMS sessions registered for " + uuid);

	}
}
