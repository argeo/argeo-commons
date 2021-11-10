package org.argeo.cms.internal.auth;

import java.io.Serializable;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.api.NodeConstants;
import org.argeo.api.security.NodeSecurityUtils;
import org.argeo.cms.auth.CmsSession;
import org.argeo.jcr.JcrUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.useradmin.Authorization;

/** Default CMS session implementation. */
public class CmsSessionImpl implements CmsSession, Serializable {
	private static final long serialVersionUID = 1867719354246307225L;
	private final static BundleContext bc = FrameworkUtil.getBundle(CmsSessionImpl.class).getBundleContext();
	private final static Log log = LogFactory.getLog(CmsSessionImpl.class);

	// private final Subject initialSubject;
	private transient AccessControlContext accessControlContext;
	private final UUID uuid;
	private final String localSessionId;
	private Authorization authorization;
	private final LdapName userDn;
	private final boolean anonymous;

	private final ZonedDateTime creationTime;
	private ZonedDateTime end;
	private final Locale locale;

	private ServiceRegistration<CmsSession> serviceRegistration;

	private Map<String, Session> dataSessions = new HashMap<>();
	private Set<String> dataSessionsInUse = new HashSet<>();
	private Set<Session> additionalDataSessions = new HashSet<>();

	private Map<String, Object> views = new HashMap<>();

	public CmsSessionImpl(Subject initialSubject, Authorization authorization, Locale locale, String localSessionId) {
		this.creationTime = ZonedDateTime.now();
		this.locale = locale;
		this.accessControlContext = Subject.doAs(initialSubject, new PrivilegedAction<AccessControlContext>() {

			@Override
			public AccessControlContext run() {
				return AccessController.getContext();
			}

		});
		// this.initialSubject = initialSubject;
		this.localSessionId = localSessionId;
		this.authorization = authorization;
		if (authorization.getName() != null)
			try {
				this.userDn = new LdapName(authorization.getName());
				this.anonymous = false;
			} catch (InvalidNameException e) {
				throw new IllegalArgumentException("Invalid user name " + authorization.getName(), e);
			}
		else {
			this.userDn = NodeSecurityUtils.ROLE_ANONYMOUS_NAME;
			this.anonymous = true;
		}
		this.uuid = UUID.randomUUID();
		// register as service
		Hashtable<String, String> props = new Hashtable<>();
		props.put(CmsSession.USER_DN, userDn.toString());
		props.put(CmsSession.SESSION_UUID, uuid.toString());
		props.put(CmsSession.SESSION_LOCAL_ID, localSessionId);
		serviceRegistration = bc.registerService(CmsSession.class, this, props);
	}

	public void close() {
		end = ZonedDateTime.now();
		serviceRegistration.unregister();

		synchronized (this) {
			// TODO check data session in use ?
			for (String path : dataSessions.keySet())
				JcrUtils.logoutQuietly(dataSessions.get(path));
			for (Session session : additionalDataSessions)
				JcrUtils.logoutQuietly(session);
		}

		try {
			LoginContext lc;
			if (isAnonymous()) {
				lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_ANONYMOUS, getSubject());
			} else {
				lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER, getSubject());
			}
			lc.logout();
		} catch (LoginException e) {
			log.warn("Could not logout " + getSubject() + ": " + e);
		} finally {
			accessControlContext = null;
		}
		log.debug("Closed " + this);
	}

	private Subject getSubject() {
		return Subject.getSubject(accessControlContext);
	}

	public Set<SecretKey> getSecretKeys() {
		checkValid();
		return getSubject().getPrivateCredentials(SecretKey.class);
	}

	public Session newDataSession(String cn, String workspace, Repository repository) {
		checkValid();
		return login(repository, workspace);
	}

	public synchronized Session getDataSession(String cn, String workspace, Repository repository) {
		checkValid();
		// FIXME make it more robust
		if (workspace == null)
			workspace = NodeConstants.SYS_WORKSPACE;
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
			return Subject.doAs(getSubject(), new PrivilegedExceptionAction<Session>() {
				@Override
				public Session run() throws Exception {
					return repository.login(workspace);
				}
			});
		} catch (PrivilegedActionException e) {
			throw new IllegalStateException("Cannot log in " + userDn + " to JCR", e);
		}
	}

	public synchronized void releaseDataSession(String cn, Session session) {
		if (additionalDataSessions.contains(session)) {
			JcrUtils.logoutQuietly(session);
			additionalDataSessions.remove(session);
			if (log.isTraceEnabled())
				log.trace("Remove additional data session " + session);
			return;
		}
		String path = cn + '/' + session.getWorkspace().getName();
		if (!dataSessionsInUse.contains(path))
			log.warn("Data session " + path + " was not in use for " + userDn);
		dataSessionsInUse.remove(path);
		Session registeredSession = dataSessions.get(path);
		if (session != registeredSession)
			log.warn("Data session " + path + " not consistent for " + userDn);
		if (log.isTraceEnabled())
			log.trace("Released data session " + session + " for " + path);
		notifyAll();
	}

	@Override
	public boolean isValid() {
		return !isClosed();
	}

	private void checkValid() {
		if (!isValid())
			throw new IllegalStateException("CMS session " + uuid + " is not valid since " + end);
	}

	final protected boolean isClosed() {
		return getEnd() != null;
	}

	@Override
	public Authorization getAuthorization() {
		checkValid();
		return authorization;
	}

	@Override
	public UUID getUuid() {
		return uuid;
	}

	@Override
	public LdapName getUserDn() {
		return userDn;
	}

	@Override
	public String getUserRole() {
		return new X500Principal(authorization.getName()).getName();
	}

	@Override
	public String getLocalId() {
		return localSessionId;
	}

	@Override
	public boolean isAnonymous() {
		return anonymous;
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	@Override
	public ZonedDateTime getCreationTime() {
		return creationTime;
	}

	@Override
	public ZonedDateTime getEnd() {
		return end;
	}

	@Override
	public void registerView(String uid, Object view) {
		checkValid();
		if (views.containsKey(uid))
			throw new IllegalArgumentException("View " + uid + " is already registered.");
		views.put(uid, view);
	}

	public String toString() {
		return "CMS Session " + userDn + " local=" + localSessionId + ", uuid=" + uuid;
	}

	public static CmsSessionImpl getByLocalId(String localId) {
		Collection<ServiceReference<CmsSession>> sr;
		try {
			sr = bc.getServiceReferences(CmsSession.class, "(" + CmsSession.SESSION_LOCAL_ID + "=" + localId + ")");
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Cannot get CMS session for id " + localId, e);
		}
		ServiceReference<CmsSession> cmsSessionRef;
		if (sr.size() == 1) {
			cmsSessionRef = sr.iterator().next();
			return (CmsSessionImpl) bc.getService(cmsSessionRef);
		} else if (sr.size() == 0) {
			return null;
		} else
			throw new IllegalStateException(sr.size() + " CMS sessions registered for " + localId);

	}

	public static CmsSessionImpl getByUuid(Object uuid) {
		Collection<ServiceReference<CmsSession>> sr;
		try {
			sr = bc.getServiceReferences(CmsSession.class, "(" + CmsSession.SESSION_UUID + "=" + uuid + ")");
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Cannot get CMS session for uuid " + uuid, e);
		}
		ServiceReference<CmsSession> cmsSessionRef;
		if (sr.size() == 1) {
			cmsSessionRef = sr.iterator().next();
			return (CmsSessionImpl) bc.getService(cmsSessionRef);
		} else if (sr.size() == 0) {
			return null;
		} else
			throw new IllegalStateException(sr.size() + " CMS sessions registered for " + uuid);

	}

	public static void closeInvalidSessions() {
		Collection<ServiceReference<CmsSession>> srs;
		try {
			srs = bc.getServiceReferences(CmsSession.class, null);
			for (ServiceReference<CmsSession> sr : srs) {
				CmsSession cmsSession = bc.getService(sr);
				if (!cmsSession.isValid()) {
					((CmsSessionImpl) cmsSession).close();
					if (log.isDebugEnabled())
						log.debug("Closed expired CMS session " + cmsSession);
				}
			}
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Cannot get CMS sessions", e);
		}
	}
}
