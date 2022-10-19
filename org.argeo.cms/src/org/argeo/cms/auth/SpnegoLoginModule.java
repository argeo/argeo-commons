package org.argeo.cms.auth;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.argeo.api.cms.CmsLog;
import org.argeo.cms.internal.runtime.CmsContextImpl;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;

import com.sun.security.jgss.GSSUtil;

/** SPNEGO login */
public class SpnegoLoginModule implements LoginModule {
	private final static CmsLog log = CmsLog.getLog(SpnegoLoginModule.class);

	private Subject subject;
	private Map<String, Object> sharedState = null;

	private GSSContext gssContext = null;

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		this.subject = subject;
		this.sharedState = (Map<String, Object>) sharedState;
	}

	@Override
	public boolean login() throws LoginException {
		byte[] spnegoToken = (byte[]) sharedState.get(CmsAuthUtils.SHARED_STATE_SPNEGO_TOKEN);
		if (spnegoToken == null)
			return false;
		gssContext = checkToken(spnegoToken);
		if (gssContext == null)
			return false;
		else {
			if (!sharedState.containsKey(CmsAuthUtils.SHARED_STATE_NAME)) {
				try {
					if (gssContext.getCredDelegState()) {
						// commit will succeeed only if we have credential delegation
						GSSName name = gssContext.getSrcName();
						String username = name.toString();
						sharedState.put(CmsAuthUtils.SHARED_STATE_NAME, username);
					}
				} catch (GSSException e) {
					throw new IllegalStateException("Cannot retrieve SPNEGO name", e);
				}
			}
			return true;
		}
	}

	@Override
	public boolean commit() throws LoginException {
		if (gssContext == null)
			return false;

		try {
			Subject gssSubject;
			if (gssContext.getCredDelegState())
				gssSubject = (Subject) GSSUtil.createSubject(gssContext.getSrcName(), gssContext.getDelegCred());
			else
				gssSubject = (Subject) GSSUtil.createSubject(gssContext.getSrcName(), null);
			// without credential delegation we won't have access to the Kerberos key
			subject.getPrincipals().addAll(gssSubject.getPrincipals());
			subject.getPrivateCredentials().addAll(gssSubject.getPrivateCredentials());
			return true;
		} catch (Exception e) {
			throw new LoginException("Cannot commit SPNEGO " + e);
		}

	}

	@Override
	public boolean abort() throws LoginException {
		if (gssContext != null) {
			try {
				gssContext.dispose();
			} catch (GSSException e) {
				if (log.isTraceEnabled())
					log.warn("Could not abort", e);
			}
			gssContext = null;
		}
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		if (gssContext != null) {
			try {
				gssContext.dispose();
			} catch (GSSException e) {
				if (log.isTraceEnabled())
					log.warn("Could not abort", e);
			}
			gssContext = null;
		}
		return true;
	}

	private GSSContext checkToken(byte[] authToken) {
		GSSManager manager = GSSManager.getInstance();
		try {
			GSSContext gContext = manager.createContext(CmsContextImpl.getCmsContext().getAcceptorCredentials());
			if (gContext == null) {
				log.debug("SpnegoUserRealm: failed to establish GSSContext");
			} else {
				if (gContext.isEstablished())
					return gContext;
				byte[] outToken = gContext.acceptSecContext(authToken, 0, authToken.length);
				if (outToken != null)
					sharedState.put(CmsAuthUtils.SHARED_STATE_SPNEGO_OUT_TOKEN, outToken);
				if (gContext.isEstablished())
					return gContext;
			}

		} catch (GSSException gsse) {
			log.warn(gsse, gsse);
		}
		return null;

	}

}
