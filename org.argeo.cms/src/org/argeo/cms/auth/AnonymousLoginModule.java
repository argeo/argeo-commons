package org.argeo.cms.auth;

import java.util.Locale;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.argeo.api.cms.CmsLog;
import org.argeo.cms.internal.runtime.CmsContextImpl;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.UserAdmin;

/** Anonymous CMS user */
public class AnonymousLoginModule implements LoginModule {
	private final static CmsLog log = CmsLog.getLog(AnonymousLoginModule.class);

	private Subject subject;
	private Map<String, Object> sharedState = null;

	// private state
//	private BundleContext bc;

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		this.subject = subject;
		this.sharedState = (Map<String, Object>) sharedState;
//		try {
//			bc = FrameworkUtil.getBundle(AnonymousLoginModule.class).getBundleContext();
//			assert bc != null;
//		} catch (Exception e) {
//			throw new IllegalStateException("Cannot initialize login module", e);
//		}
	}

	@Override
	public boolean login() throws LoginException {
		return true;
	}

	@Override
	public boolean commit() throws LoginException {
		UserAdmin userAdmin = CmsContextImpl.getCmsContext().getUserAdmin();
		Authorization authorization = userAdmin.getAuthorization(null);
		RemoteAuthRequest request = (RemoteAuthRequest) sharedState.get(CmsAuthUtils.SHARED_STATE_HTTP_REQUEST);
		Locale locale = Locale.getDefault();
		if (request != null)
			locale = request.getLocale();
		CmsAuthUtils.addAuthorization(subject, authorization);
		CmsAuthUtils.registerSessionAuthorization(request, subject, authorization, locale);
		if (log.isTraceEnabled())
			log.trace("Anonymous logged in to CMS: " + subject);
		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		if (log.isTraceEnabled())
			log.trace("Logging out anonymous from CMS... " + subject);
		CmsAuthUtils.cleanUp(subject);
		return true;
	}
}
