package org.argeo.cms.internal.runtime;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.params.DefaultHttpParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.params.HttpParams;
import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.internal.http.client.HttpCredentialProvider;
import org.argeo.cms.internal.http.client.SpnegoAuthScheme;
import org.argeo.osgi.useradmin.AggregatingUserAdmin;
import org.argeo.osgi.useradmin.LdapUserAdmin;
import org.argeo.osgi.useradmin.LdifUserAdmin;
import org.argeo.osgi.useradmin.OsUserDirectory;
import org.argeo.osgi.useradmin.UserAdminConf;
import org.argeo.osgi.useradmin.UserDirectory;
import org.argeo.util.naming.dns.DnsBrowser;
import org.argeo.util.transaction.WorkControl;
import org.argeo.util.transaction.WorkTransaction;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;

/**
 * Aggregates multiple {@link UserDirectory} and integrates them with system
 * roles.
 */
public class CmsUserAdmin extends AggregatingUserAdmin {
	private final static CmsLog log = CmsLog.getLog(CmsUserAdmin.class);

	// GSS API
	private Path nodeKeyTab = KernelUtils.getOsgiInstancePath(KernelConstants.NODE_KEY_TAB_PATH);
	private GSSCredential acceptorCredentials;

	private boolean singleUser = false;

	private WorkControl transactionManager;
	private WorkTransaction userTransaction;

	public CmsUserAdmin() {
		super(CmsConstants.ROLES_BASEDN, CmsConstants.TOKENS_BASEDN);
	}

	public void start() {
	}

	public void stop() {
	}

	public UserDirectory enableUserDirectory(Dictionary<String, ?> properties) {
		String uri = (String) properties.get(UserAdminConf.uri.name());
		Object realm = properties.get(UserAdminConf.realm.name());
		URI u;
		try {
			if (uri == null) {
				String baseDn = (String) properties.get(UserAdminConf.baseDn.name());
				u = KernelUtils.getOsgiInstanceUri(KernelConstants.DIR_NODE + '/' + baseDn + ".ldif");
			} else if (realm != null) {
				u = null;
			} else {
				u = new URI(uri);
			}
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Badly formatted URI " + uri, e);
		}

		// Create
		UserDirectory userDirectory;
		if (realm != null || UserAdminConf.SCHEME_LDAP.equals(u.getScheme())
				|| UserAdminConf.SCHEME_LDAPS.equals(u.getScheme())) {
			userDirectory = new LdapUserAdmin(properties);
		} else if (UserAdminConf.SCHEME_FILE.equals(u.getScheme())) {
			userDirectory = new LdifUserAdmin(u, properties);
		} else if (UserAdminConf.SCHEME_OS.equals(u.getScheme())) {
			userDirectory = new OsUserDirectory(u, properties);
			singleUser = true;
		} else {
			throw new IllegalArgumentException("Unsupported scheme " + u.getScheme());
		}
		String basePath = userDirectory.getContext();

		addUserDirectory(userDirectory);
		if (isSystemRolesBaseDn(basePath)) {
			addStandardSystemRoles();
		}
		if (log.isDebugEnabled()) {
			log.debug("User directory " + userDirectory.getContext() + (u != null ? " [" + u.getScheme() + "]" : "")
					+ " enabled." + (realm != null ? " " + realm + " realm." : ""));
		}
		return userDirectory;
	}

	protected void addStandardSystemRoles() {
		// we assume UserTransaction is already available (TODO make it more robust)
		try {
			userTransaction.begin();
			Role adminRole = getRole(CmsConstants.ROLE_ADMIN);
			if (adminRole == null) {
				adminRole = createRole(CmsConstants.ROLE_ADMIN, Role.GROUP);
			}
			if (getRole(CmsConstants.ROLE_USER_ADMIN) == null) {
				Group userAdminRole = (Group) createRole(CmsConstants.ROLE_USER_ADMIN, Role.GROUP);
				userAdminRole.addMember(adminRole);
			}
			userTransaction.commit();
		} catch (Exception e) {
			try {
				userTransaction.rollback();
			} catch (Exception e1) {
				// silent
			}
			throw new IllegalStateException("Cannot add standard system roles", e);
		}
	}

	@Override
	protected void addAbstractSystemRoles(Authorization rawAuthorization, Set<String> sysRoles) {
		if (rawAuthorization.getName() == null) {
			sysRoles.add(CmsConstants.ROLE_ANONYMOUS);
		} else {
			sysRoles.add(CmsConstants.ROLE_USER);
		}
	}

	@Override
	protected void postAdd(UserDirectory userDirectory) {
		userDirectory.setTransactionControl(transactionManager);

		Optional<String> realm = userDirectory.getRealm();
		if (realm.isPresent()) {
			if (Files.exists(nodeKeyTab)) {
				String servicePrincipal = getKerberosServicePrincipal(realm.get());
				if (servicePrincipal != null) {
					CallbackHandler callbackHandler = new CallbackHandler() {
						@Override
						public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
							for (Callback callback : callbacks)
								if (callback instanceof NameCallback)
									((NameCallback) callback).setName(servicePrincipal);

						}
					};
					try {
						LoginContext nodeLc = new LoginContext(CmsAuth.LOGIN_CONTEXT_NODE, callbackHandler);
						nodeLc.login();
						acceptorCredentials = logInAsAcceptor(nodeLc.getSubject(), servicePrincipal);
					} catch (LoginException e) {
						throw new IllegalStateException("Cannot log in kernel", e);
					}
				}
			}

			// Register client-side SPNEGO auth scheme
			AuthPolicy.registerAuthScheme(SpnegoAuthScheme.NAME, SpnegoAuthScheme.class);
			HttpParams params = DefaultHttpParams.getDefaultParams();
			ArrayList<String> schemes = new ArrayList<>();
			schemes.add(SpnegoAuthScheme.NAME);// SPNEGO preferred
			// schemes.add(AuthPolicy.BASIC);// incompatible with Basic
			params.setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, schemes);
			params.setParameter(CredentialsProvider.PROVIDER, new HttpCredentialProvider());
			params.setParameter(HttpMethodParams.COOKIE_POLICY, KernelConstants.COOKIE_POLICY_BROWSER_COMPATIBILITY);
			// params.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		}
	}

	@Override
	protected void preDestroy(UserDirectory userDirectory) {
		Optional<String> realm = userDirectory.getRealm();
		if (realm.isPresent()) {
			if (acceptorCredentials != null) {
				try {
					acceptorCredentials.dispose();
				} catch (GSSException e) {
					// silent
				}
				acceptorCredentials = null;
			}
		}
	}

	private String getKerberosServicePrincipal(String realm) {
		String hostname;
		try (DnsBrowser dnsBrowser = new DnsBrowser()) {
			InetAddress localhost = InetAddress.getLocalHost();
			hostname = localhost.getHostName();
			String dnsZone = hostname.substring(hostname.indexOf('.') + 1);
			String ipfromDns = dnsBrowser.getRecord(hostname, localhost instanceof Inet6Address ? "AAAA" : "A");
			boolean consistentIp = localhost.getHostAddress().equals(ipfromDns);
			String kerberosDomain = dnsBrowser.getRecord("_kerberos." + dnsZone, "TXT");
			if (consistentIp && kerberosDomain != null && kerberosDomain.equals(realm) && Files.exists(nodeKeyTab)) {
				return KernelConstants.DEFAULT_KERBEROS_SERVICE + "/" + hostname + "@" + kerberosDomain;
			} else
				return null;
		} catch (Exception e) {
			log.warn("Exception when determining kerberos principal", e);
			return null;
		}
	}

	private GSSCredential logInAsAcceptor(Subject subject, String servicePrincipal) {
		// not static because class is not supported by Android
		final Oid KERBEROS_OID;
		try {
			KERBEROS_OID = new Oid("1.3.6.1.5.5.2");
		} catch (GSSException e) {
			throw new IllegalStateException("Cannot create Kerberos OID", e);
		}
		// GSS
		Iterator<KerberosPrincipal> krb5It = subject.getPrincipals(KerberosPrincipal.class).iterator();
		if (!krb5It.hasNext())
			return null;
		KerberosPrincipal krb5Principal = null;
		while (krb5It.hasNext()) {
			KerberosPrincipal principal = krb5It.next();
			if (principal.getName().equals(servicePrincipal))
				krb5Principal = principal;
		}

		if (krb5Principal == null)
			return null;

		GSSManager manager = GSSManager.getInstance();
		try {
			GSSName gssName = manager.createName(krb5Principal.getName(), null);
			GSSCredential serverCredentials = Subject.doAs(subject, new PrivilegedExceptionAction<GSSCredential>() {

				@Override
				public GSSCredential run() throws GSSException {
					return manager.createCredential(gssName, GSSCredential.INDEFINITE_LIFETIME, KERBEROS_OID,
							GSSCredential.ACCEPT_ONLY);

				}

			});
			if (log.isDebugEnabled())
				log.debug("GSS acceptor configured for " + krb5Principal);
			return serverCredentials;
		} catch (Exception gsse) {
			throw new IllegalStateException("Cannot create acceptor credentials for " + krb5Principal, gsse);
		}
	}

	public GSSCredential getAcceptorCredentials() {
		return acceptorCredentials;
	}

	public boolean hasAcceptorCredentials() {
		return acceptorCredentials != null;
	}

	public boolean isSingleUser() {
		return singleUser;
	}

	public void setTransactionManager(WorkControl transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setUserTransaction(WorkTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}

	/*
	 * STATIC
	 */

}
