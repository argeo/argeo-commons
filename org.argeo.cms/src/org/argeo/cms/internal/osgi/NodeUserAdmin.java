package org.argeo.cms.internal.osgi;

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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.naming.ldap.LdapName;
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
import org.argeo.cms.internal.runtime.KernelConstants;
import org.argeo.cms.internal.runtime.KernelUtils;
import org.argeo.osgi.transaction.WorkControl;
import org.argeo.osgi.transaction.WorkTransaction;
import org.argeo.osgi.useradmin.AbstractUserDirectory;
import org.argeo.osgi.useradmin.AggregatingUserAdmin;
import org.argeo.osgi.useradmin.LdapUserAdmin;
import org.argeo.osgi.useradmin.LdifUserAdmin;
import org.argeo.osgi.useradmin.OsUserDirectory;
import org.argeo.osgi.useradmin.UserAdminConf;
import org.argeo.osgi.useradmin.UserDirectory;
import org.argeo.util.naming.DnsBrowser;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;

/**
 * Aggregates multiple {@link UserDirectory} and integrates them with system
 * roles.
 */
public class NodeUserAdmin extends AggregatingUserAdmin implements ManagedServiceFactory, KernelConstants {
	private final static CmsLog log = CmsLog.getLog(NodeUserAdmin.class);

	// OSGi
	private Map<String, LdapName> pidToBaseDn = new HashMap<>();
//	private Map<String, ServiceRegistration<UserDirectory>> pidToServiceRegs = new HashMap<>();
//	private ServiceRegistration<UserAdmin> userAdminReg;

	// JTA
//	private final ServiceTracker<WorkControl, WorkControl> tmTracker;
	// private final String cacheName = UserDirectory.class.getName();

	// GSS API
	private Path nodeKeyTab = KernelUtils.getOsgiInstancePath(KernelConstants.NODE_KEY_TAB_PATH);
	private GSSCredential acceptorCredentials;

	private boolean singleUser = false;
//	private boolean systemRolesAvailable = false;

//	CmsUserManagerImpl userManager;
	private WorkControl transactionManager;
	private WorkTransaction userTransaction;

	public NodeUserAdmin() {
		super(CmsConstants.ROLES_BASEDN, CmsConstants.TOKENS_BASEDN);
//		BundleContext bc = Activator.getBundleContext();
//		if (bc != null) {
//			tmTracker = new ServiceTracker<>(bc, WorkControl.class, null) {
//
//				@Override
//				public WorkControl addingService(ServiceReference<WorkControl> reference) {
//					WorkControl workControl = super.addingService(reference);
//					userManager = new CmsUserManagerImpl();
//					userManager.setUserAdmin(NodeUserAdmin.this);
//					// FIXME make it more robust
//					userManager.setUserTransaction((WorkTransaction) workControl);
//					bc.registerService(CmsUserManager.class, userManager, null);
//					return workControl;
//				}
//			};
//			tmTracker.open();
//		} else {
//			tmTracker = null;
//		}
	}

	public void start() {
	}

	public void stop() {
	}

	@Override
	public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
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
		AbstractUserDirectory userDirectory;
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
		LdapName baseDn = userDirectory.getBaseDn();

		// FIXME make updates more robust
		if (pidToBaseDn.containsValue(baseDn)) {
			if (log.isDebugEnabled())
				log.debug("Ignoring user directory update of " + baseDn);
			return;
		}

		addUserDirectory(userDirectory);

		// OSGi
		Hashtable<String, Object> regProps = new Hashtable<>();
		regProps.put(Constants.SERVICE_PID, pid);
		if (isSystemRolesBaseDn(baseDn))
			regProps.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
		regProps.put(UserAdminConf.baseDn.name(), baseDn);
		// ServiceRegistration<UserDirectory> reg =
		// bc.registerService(UserDirectory.class, userDirectory, regProps);
		CmsActivator.getBundleContext().registerService(UserDirectory.class, userDirectory, regProps);
//		userManager.addUserDirectory(userDirectory, regProps);
		pidToBaseDn.put(pid, baseDn);
		// pidToServiceRegs.put(pid, reg);

		if (log.isDebugEnabled()) {
			log.debug("User directory " + userDirectory.getBaseDn() + (u != null ? " [" + u.getScheme() + "]" : "")
					+ " enabled." + (realm != null ? " " + realm + " realm." : ""));
		}

		if (isSystemRolesBaseDn(baseDn)) {
			addStandardSystemRoles();

			// publishes itself as user admin only when system roles are available
			Dictionary<String, Object> userAdminregProps = new Hashtable<>();
			userAdminregProps.put(CmsConstants.CN, CmsConstants.DEFAULT);
			userAdminregProps.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
			CmsActivator.getBundleContext().registerService(UserAdmin.class, this, userAdminregProps);
		}

//		if (isSystemRolesBaseDn(baseDn))
//			systemRolesAvailable = true;
//
//		// start publishing only when system roles are available
//		if (systemRolesAvailable) {
//			// The list of baseDns is published as properties
//			// TODO clients should rather reference USerDirectory services
//			if (userAdminReg != null)
//				userAdminReg.unregister();
//			// register self as main user admin
//			Dictionary<String, Object> userAdminregProps = currentState();
//			userAdminregProps.put(NodeConstants.CN, NodeConstants.DEFAULT);
//			userAdminregProps.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
//			userAdminReg = bc.registerService(UserAdmin.class, this, userAdminregProps);
//		}
	}

	private void addStandardSystemRoles() {
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
	public void deleted(String pid) {
		// assert pidToServiceRegs.get(pid) != null;
		assert pidToBaseDn.get(pid) != null;
		// pidToServiceRegs.remove(pid).unregister();
		LdapName baseDn = pidToBaseDn.remove(pid);
		removeUserDirectory(baseDn);
	}

	@Override
	public String getName() {
		return "Node User Admin";
	}

	@Override
	protected void addAbstractSystemRoles(Authorization rawAuthorization, Set<String> sysRoles) {
		if (rawAuthorization.getName() == null) {
			sysRoles.add(CmsConstants.ROLE_ANONYMOUS);
		} else {
			sysRoles.add(CmsConstants.ROLE_USER);
		}
	}

	protected void postAdd(AbstractUserDirectory userDirectory) {
		// JTA
//		WorkControl tm = tmTracker != null ? tmTracker.getService() : null;
//		if (tm == null)
//			throw new IllegalStateException("A JTA transaction manager must be available.");
		userDirectory.setTransactionControl(transactionManager);
//		if (tmTracker.getService() instanceof BitronixTransactionManager)
//			EhCacheXAResourceProducer.registerXAResource(cacheName, userDirectory.getXaResource());

		Object realm = userDirectory.getProperties().get(UserAdminConf.realm.name());
		if (realm != null) {
			if (Files.exists(nodeKeyTab)) {
				String servicePrincipal = getKerberosServicePrincipal(realm.toString());
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

	protected void preDestroy(AbstractUserDirectory userDirectory) {
//		if (tmTracker.getService() instanceof BitronixTransactionManager)
//			EhCacheXAResourceProducer.unregisterXAResource(cacheName, userDirectory.getXaResource());

		Object realm = userDirectory.getProperties().get(UserAdminConf.realm.name());
		if (realm != null) {
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

	public final static Oid KERBEROS_OID;
	static {
		try {
			KERBEROS_OID = new Oid("1.3.6.1.5.5.2");
		} catch (GSSException e) {
			throw new IllegalStateException("Cannot create Kerberos OID", e);
		}
	}
}
