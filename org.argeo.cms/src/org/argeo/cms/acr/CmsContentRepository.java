package org.argeo.cms.acr;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.argeo.api.acr.ContentSession;
import org.argeo.api.acr.spi.ProvidedRepository;
import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsSession;
import org.argeo.api.cms.CmsState;
import org.argeo.api.cms.DataAdminPrincipal;
import org.argeo.api.uuid.UuidFactory;
import org.argeo.cms.CurrentUser;
import org.argeo.cms.internal.runtime.CmsContextImpl;
import org.argeo.cms.util.CurrentSubject;

/**
 * Multi-session {@link ProvidedRepository}, integrated with a CMS.
 */
public class CmsContentRepository extends AbstractContentRepository {
	public final static String RUN_BASE = "/run";
	public final static String DIRECTORY_BASE = "/directory";

	private Map<CmsSession, CmsContentSession> userSessions = Collections.synchronizedMap(new HashMap<>());

	private CmsState cmsState;
	private UuidFactory uuidFactory;

	/*
	 * REPOSITORY
	 */

	@Override
	public ContentSession get() {
		return get(CmsContextImpl.getCmsContext().getDefaultLocale());
	}

	@Override
	public ContentSession get(Locale locale) {
		Subject subject = CurrentSubject.current();
		if (subject == null)
			throw new IllegalStateException("Caller must be authenticated");
		if (!CmsSession.hasCmsSession(subject)) {
			if (DataAdminPrincipal.isDataAdmin(subject)) {
				// TODO open multiple data admin sessions?
				return getSystemSession();
			}
			throw new IllegalStateException("Caller must be authenticated");
		}

		CmsSession cmsSession = CurrentUser.getCmsSession();
		CmsContentSession contentSession = userSessions.get(cmsSession);
		if (contentSession == null) {
			final CmsContentSession newContentSession = new CmsContentSession(this, cmsSession.uuid(),
					cmsSession.getSubject(), locale, uuidFactory);
			cmsSession.addOnCloseCallback((c) -> {
				newContentSession.close();
				userSessions.remove(cmsSession);
			});
			contentSession = newContentSession;
		}
		return contentSession;
	}

	@Override
	protected CmsContentSession newSystemSession() {
		LoginContext loginContext;
		try {
			loginContext = new LoginContext(CmsAuth.DATA_ADMIN.getLoginContextName());
			loginContext.login();
		} catch (LoginException e1) {
			throw new RuntimeException("Could not login as data admin", e1);
		} finally {
		}
		return new CmsContentSession(this, getCmsState().getUuid(), loginContext.getSubject(), Locale.getDefault(),
				uuidFactory);
	}

	protected CmsState getCmsState() {
		return cmsState;
	}

	public void setCmsState(CmsState cmsState) {
		this.cmsState = cmsState;
	}

	public void setUuidFactory(UuidFactory uuidFactory) {
		this.uuidFactory = uuidFactory;
	}

}
