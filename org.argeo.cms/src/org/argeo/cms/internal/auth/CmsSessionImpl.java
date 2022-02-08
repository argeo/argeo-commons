package org.argeo.cms.internal.auth;

import java.io.Serializable;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import javax.crypto.SecretKey;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.x500.X500Principal;

import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsSession;
import org.argeo.cms.security.NodeSecurityUtils;
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
	private final static CmsLog log = CmsLog.getLog(CmsSessionImpl.class);

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

	private Map<String, Object> views = new HashMap<>();

	private List<Consumer<CmsSession>> onCloseCallbacks = Collections.synchronizedList(new ArrayList<>());

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

		for (Consumer<CmsSession> onClose : onCloseCallbacks) {
			onClose.accept(this);
		}

		try {
			LoginContext lc;
			if (isAnonymous()) {
				lc = new LoginContext(CmsAuth.LOGIN_CONTEXT_ANONYMOUS, getSubject());
			} else {
				lc = new LoginContext(CmsAuth.LOGIN_CONTEXT_USER, getSubject());
			}
			lc.logout();
		} catch (LoginException e) {
			log.warn("Could not logout " + getSubject() + ": " + e);
		} finally {
			accessControlContext = null;
		}
		log.debug("Closed " + this);
	}

	@Override
	public void addOnCloseCallback(Consumer<CmsSession> onClose) {
		onCloseCallbacks.add(onClose);
	}

	public Subject getSubject() {
		return Subject.getSubject(accessControlContext);
	}

	public Set<SecretKey> getSecretKeys() {
		checkValid();
		return getSubject().getPrivateCredentials(SecretKey.class);
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

	public Authorization getAuthorization() {
		checkValid();
		return authorization;
	}

	@Override
	public String getDisplayName() {
		return authorization.toString();
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
