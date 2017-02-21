package org.argeo.cms.auth;

import java.lang.reflect.Method;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.internal.kernel.Activator;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;

/** SPNEGO login */
public class SpnegoLoginModule implements LoginModule {
	private final static Log log = LogFactory.getLog(SpnegoLoginModule.class);

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
		else
			return true;
		// try {
		// String clientName = gssContext.getSrcName().toString();
		// String role = clientName.substring(clientName.indexOf('@') + 1);
		//
		// log.debug("SpnegoUserRealm: established a security context");
		// log.debug("Client Principal is: " + gssContext.getSrcName());
		// log.debug("Server Principal is: " + gssContext.getTargName());
		// log.debug("Client Default Role: " + role);
		// } catch (GSSException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}

	@Override
	public boolean commit() throws LoginException {
		if (gssContext == null)
			return false;

		try {
			Class<?> gssUtilsClass = Class.forName("com.sun.security.jgss.GSSUtil");
			Method createSubjectMethod = gssUtilsClass.getMethod("createSubject", GSSName.class, GSSCredential.class);
			Subject gssSubject;
			if (gssContext.getCredDelegState())
				gssSubject = (Subject) createSubjectMethod.invoke(null, gssContext.getSrcName(),
						gssContext.getDelegCred());
			else
				gssSubject = (Subject) createSubjectMethod.invoke(null, gssContext.getSrcName(), null);
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
			GSSContext gContext = manager.createContext(Activator.getAcceptorCredentials());

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
