package org.argeo.cms.internal.auth;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.x500.X500Principal;

import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsSession;
import org.argeo.api.uuid.UuidIdentified;
import org.argeo.cms.internal.runtime.CmsContextImpl;
import org.osgi.service.useradmin.Authorization;

/** Default CMS session implementation. */
public class CmsSessionImpl implements CmsSession, Serializable, UuidIdentified {
	private static final long serialVersionUID = 1867719354246307225L;
	private final static CmsLog log = CmsLog.getLog(CmsSessionImpl.class);

	private transient Subject subject;
	private final UUID uuid;
	private final String localSessionId;
	private Authorization authorization;
//	private final LdapName userDn;
	private final String userDn;
	private final boolean anonymous;

	private final ZonedDateTime creationTime;
	private ZonedDateTime end;
	private final Locale locale;

	private Map<String, Object> views = new HashMap<>();

	private List<Consumer<CmsSession>> onCloseCallbacks = Collections.synchronizedList(new ArrayList<>());

	public CmsSessionImpl(UUID uuid, Subject initialSubject, Authorization authorization, Locale locale,
			String localSessionId) {
		Objects.requireNonNull(uuid);

		this.creationTime = ZonedDateTime.now();
		this.locale = locale;
		this.subject = initialSubject;
		this.localSessionId = localSessionId;
		this.authorization = authorization;
		if (authorization.getName() != null) {
			this.userDn = authorization.getName();
			this.anonymous = false;
		} else {
			this.userDn = CmsConstants.ROLE_ANONYMOUS;
			this.anonymous = true;
		}
		this.uuid = uuid;
	}

	public void close() {
		end = ZonedDateTime.now();
		CmsContextImpl.getCmsContext().unregisterCmsSession(this);
//		serviceRegistration.unregister();

		for (Consumer<CmsSession> onClose : onCloseCallbacks) {
			onClose.accept(this);
		}

		try {
			LoginContext lc;
			if (isAnonymous()) {
				lc = CmsAuth.ANONYMOUS.newLoginContext(getSubject());
			} else {
				lc = CmsAuth.USER.newLoginContext(getSubject());
			}
			lc.logout();
		} catch (LoginException e) {
			log.warn("Could not logout " + getSubject() + ": " + e);
		} finally {
			subject = null;
		}
		log.debug("Closed " + this);
	}

	@Override
	public void addOnCloseCallback(Consumer<CmsSession> onClose) {
		onCloseCallbacks.add(onClose);
	}

	public Subject getSubject() {
		return subject;
	}

//	public Set<SecretKey> getSecretKeys() {
//		checkValid();
//		return getSubject().getPrivateCredentials(SecretKey.class);
//	}

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
	public UUID uuid() {
		return uuid;
	}

	@Override
	public String getUserDn() {
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

	/*
	 * OBJECT METHODS
	 */

	@Override
	public boolean equals(Object o) {
		return UuidIdentified.equals(this, o);
	}

	@Override
	public int hashCode() {
		return UuidIdentified.hashCode(this);
	}

	@Override
	public String toString() {
		return "CMS Session " + userDn + " localId=" + localSessionId + ", uuid=" + uuid;
	}
}
