package org.argeo.cms.auth;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Base64;
import java.util.function.Supplier;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsSession;
import org.argeo.cms.internal.http.CmsAuthenticator;
import org.argeo.cms.internal.runtime.CmsContextImpl;
import org.argeo.util.http.HttpHeader;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

/** Remote authentication utilities. */
public class RemoteAuthUtils {
	private final static CmsLog log = CmsLog.getLog(RemoteAuthUtils.class);

	static final String REMOTE_USER = "org.osgi.service.http.authentication.remote.user";
	private final static Oid KERBEROS_OID;
//	private final static Oid KERB_V5_OID, KRB5_PRINCIPAL_NAME_OID;
	static {
		try {
			KERBEROS_OID = new Oid("1.3.6.1.5.5.2");
//			KERB_V5_OID = new Oid("1.2.840.113554.1.2.2");
//			KRB5_PRINCIPAL_NAME_OID = new Oid("1.2.840.113554.1.2.2.1");
		} catch (GSSException e) {
			throw new IllegalStateException("Cannot create Kerberos OID", e);
		}
	}

	/**
	 * Execute this supplier, using the CMS class loader as context classloader.
	 * Useful to log in to JCR.
	 */
	public final static <T> T doAs(Supplier<T> supplier, RemoteAuthRequest req) {
		ClassLoader currentContextCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(RemoteAuthUtils.class.getClassLoader());
		try {
			return Subject.doAs(
					Subject.getSubject((AccessControlContext) req.getAttribute(AccessControlContext.class.getName())),
					new PrivilegedAction<T>() {

						@Override
						public T run() {
							return supplier.get();
						}

					});
		} finally {
			Thread.currentThread().setContextClassLoader(currentContextCl);
		}
	}

	public final static void configureRequestSecurity(RemoteAuthRequest req) {
		if (req.getAttribute(AccessControlContext.class.getName()) != null)
			throw new IllegalStateException("Request already authenticated.");
		AccessControlContext acc = AccessController.getContext();
		req.setAttribute(REMOTE_USER, CurrentUser.getUsername());
		req.setAttribute(AccessControlContext.class.getName(), acc);
	}

	public final static void clearRequestSecurity(RemoteAuthRequest req) {
		if (req.getAttribute(AccessControlContext.class.getName()) == null)
			throw new IllegalStateException("Cannot clear non-authenticated request.");
		req.setAttribute(REMOTE_USER, null);
		req.setAttribute(AccessControlContext.class.getName(), null);
	}

	public static CmsSession getCmsSession(RemoteAuthRequest req) {
		Subject subject = Subject
				.getSubject((AccessControlContext) req.getAttribute(AccessControlContext.class.getName()));
		CmsSession cmsSession = CmsContextImpl.getCmsContext().getCmsSession(subject);
		return cmsSession;
	}

	public static String getGssToken(Subject subject, String service, String server) {
		if (subject.getPrivateCredentials(KerberosTicket.class).isEmpty())
			throw new IllegalArgumentException("Subject " + subject + " is not GSS authenticated.");
		return Subject.doAs(subject, (PrivilegedAction<String>) () -> {
			// !! different format than Kerberos
			String serverPrinc = service + "@" + server;
			GSSContext context = null;
			String tokenStr = null;

			try {
				// Get service's principal name
				GSSManager manager = GSSManager.getInstance();
				// GSSName serverName = manager.createName(serverPrinc,
				// GSSName.NT_HOSTBASED_SERVICE, KERBEROS_OID);
				GSSName serverName = manager.createName(serverPrinc, GSSName.NT_HOSTBASED_SERVICE);

				// Get the context for authentication
				context = manager.createContext(serverName, KERBEROS_OID, null, GSSContext.DEFAULT_LIFETIME);
				// context.requestMutualAuth(true); // Request mutual authentication
				// context.requestConf(true); // Request confidentiality
				context.requestCredDeleg(true);

				byte[] token = new byte[0];

				// token is ignored on the first call
				token = context.initSecContext(token, 0, token.length);

				// Send a token to the server if one was generated by
				// initSecContext
				if (token != null) {
					tokenStr = Base64.getEncoder().encodeToString(token);
					// complete=true;
				}
				return tokenStr;

			} catch (GSSException e) {
				throw new IllegalStateException("Cannot authenticate to " + serverPrinc, e);
			}
		});
	}

	public static LoginContext anonymousLogin(RemoteAuthRequest remoteAuthRequest,
			RemoteAuthResponse remoteAuthResponse) {
		// anonymous
		ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(RemoteAuthUtils.class.getClassLoader());
			LoginContext lc = CmsAuth.ANONYMOUS
					.newLoginContext(new RemoteAuthCallbackHandler(remoteAuthRequest, remoteAuthResponse));
			lc.login();
			return lc;
		} catch (LoginException e1) {
			if (log.isDebugEnabled())
				log.error("Cannot log in as anonymous", e1);
			return null;
		} finally {
			Thread.currentThread().setContextClassLoader(currentContextClassLoader);
		}
	}

	public static int askForWwwAuth(RemoteAuthResponse remoteAuthResponse, String realm, boolean forceBasic) {
		// response.setHeader(HttpUtils.HEADER_WWW_AUTHENTICATE, "basic
		// realm=\"" + httpAuthRealm + "\"");
		if (SpnegoLoginModule.hasAcceptorCredentials() && !forceBasic)// SPNEGO
			remoteAuthResponse.setHeader(HttpHeader.WWW_AUTHENTICATE.getName(), HttpHeader.NEGOTIATE);
		else
			remoteAuthResponse.setHeader(HttpHeader.WWW_AUTHENTICATE.getName(),
					HttpHeader.BASIC + " " + HttpHeader.REALM + "=\"" + realm + "\"");

		// response.setDateHeader("Date", System.currentTimeMillis());
		// response.setDateHeader("Expires", System.currentTimeMillis() + (24 *
		// 60 * 60 * 1000));
		// response.setHeader("Accept-Ranges", "bytes");
		// response.setHeader("Connection", "Keep-Alive");
		// response.setHeader("Keep-Alive", "timeout=5, max=97");
		// response.setContentType("text/html; charset=UTF-8");

		return 401;
	}

}
