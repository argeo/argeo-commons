package org.argeo.cms.internal.kernel;

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
import javax.transaction.TransactionManager;

import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.params.DefaultHttpParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.params.HttpParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.cms.internal.http.client.HttpCredentialProvider;
import org.argeo.cms.internal.http.client.SpnegoAuthScheme;
import org.argeo.naming.DnsBrowser;
import org.argeo.node.NodeConstants;
import org.argeo.osgi.useradmin.AbstractUserDirectory;
import org.argeo.osgi.useradmin.AggregatingUserAdmin;
import org.argeo.osgi.useradmin.LdapUserAdmin;
import org.argeo.osgi.useradmin.LdifUserAdmin;
import org.argeo.osgi.useradmin.OsUserDirectory;
import org.argeo.osgi.useradmin.UserAdminConf;
import org.argeo.osgi.useradmin.UserDirectory;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Aggregates multiple {@link UserDirectory} and integrates them with system
 * roles.
 */
class NodeUserAdmin extends AggregatingUserAdmin implements ManagedServiceFactory, KernelConstants {
	private final static Log log = LogFactory.getLog(NodeUserAdmin.class);
	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

	// OSGi
	private Map<String, LdapName> pidToBaseDn = new HashMap<>();
	private Map<String, ServiceRegistration<UserDirectory>> pidToServiceRegs = new HashMap<>();
	private ServiceRegistration<UserAdmin> userAdminReg;

	// JTA
	private final ServiceTracker<TransactionManager, TransactionManager> tmTracker;
	// private final String cacheName = UserDirectory.class.getName();

	// GSS API
	private Path nodeKeyTab = KernelUtils.getOsgiInstancePath(KernelConstants.NODE_KEY_TAB_PATH);
	private GSSCredential acceptorCredentials;

	private boolean singleUser = false;
	private boolean systemRolesAvailable = false;

	public NodeUserAdmin(String systemRolesBaseDn) {
		super(systemRolesBaseDn);
		tmTracker = new ServiceTracker<>(bc, TransactionManager.class, null);
		tmTracker.open();
	}

	@Override
	public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
		String uri = (String) properties.get(UserAdminConf.uri.name());
		URI u;
		try {
			if (uri == null) {
				String baseDn = (String) properties.get(UserAdminConf.baseDn.name());
				u = KernelUtils.getOsgiInstanceUri(KernelConstants.DIR_NODE + '/' + baseDn + ".ldif");
			} else
				u = new URI(uri);
		} catch (URISyntaxException e) {
			throw new CmsException("Badly formatted URI " + uri, e);
		}

		// Create
		AbstractUserDirectory userDirectory;
		if (UserAdminConf.SCHEME_LDAP.equals(u.getScheme())) {
			userDirectory = new LdapUserAdmin(properties);
		} else if (UserAdminConf.SCHEME_FILE.equals(u.getScheme())) {
			userDirectory = new LdifUserAdmin(u, properties);
		} else if (UserAdminConf.SCHEME_OS.equals(u.getScheme())) {
			userDirectory = new OsUserDirectory(u, properties);
			singleUser = true;
		} else {
			throw new CmsException("Unsupported scheme " + u.getScheme());
		}
		Object realm = userDirectory.getProperties().get(UserAdminConf.realm.name());
		addUserDirectory(userDirectory);

		// OSGi
		LdapName baseDn = userDirectory.getBaseDn();
		Dictionary<String, Object> regProps = new Hashtable<>();
		regProps.put(Constants.SERVICE_PID, pid);
		if (isSystemRolesBaseDn(baseDn))
			regProps.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
		regProps.put(UserAdminConf.baseDn.name(), baseDn);
		ServiceRegistration<UserDirectory> reg = bc.registerService(UserDirectory.class, userDirectory, regProps);
		pidToBaseDn.put(pid, baseDn);
		pidToServiceRegs.put(pid, reg);

		if (log.isDebugEnabled())
			log.debug("User directory " + userDirectory.getBaseDn() + " [" + u.getScheme() + "] enabled."
					+ (realm != null ? " " + realm + " realm." : ""));

		if (isSystemRolesBaseDn(baseDn))
			systemRolesAvailable = true;

		// start publishing only when system roles are available
		if (systemRolesAvailable) {
			// The list of baseDns is published as properties
			// TODO clients should rather reference USerDirectory services
			if (userAdminReg != null)
				userAdminReg.unregister();
			// register self as main user admin
			Dictionary<String, Object> userAdminregProps = currentState();
			userAdminregProps.put(NodeConstants.CN, NodeConstants.DEFAULT);
			userAdminregProps.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
			userAdminReg = bc.registerService(UserAdmin.class, this, userAdminregProps);
		}
	}

	@Override
	public void deleted(String pid) {
		assert pidToServiceRegs.get(pid) != null;
		assert pidToBaseDn.get(pid) != null;
		pidToServiceRegs.remove(pid).unregister();
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
			sysRoles.add(NodeConstants.ROLE_ANONYMOUS);
		} else {
			sysRoles.add(NodeConstants.ROLE_USER);
		}
	}

	protected void postAdd(AbstractUserDirectory userDirectory) {
		// JTA
		TransactionManager tm = tmTracker.getService();
		if (tm == null)
			throw new CmsException("A JTA transaction manager must be available.");
		userDirectory.setTransactionManager(tm);
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
						LoginContext nodeLc = new LoginContext(NodeConstants.LOGIN_CONTEXT_NODE, callbackHandler);
						nodeLc.login();
						acceptorCredentials = logInAsAcceptor(nodeLc.getSubject(), servicePrincipal);
					} catch (LoginException e) {
						throw new CmsException("Cannot log in kernel", e);
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
				return NodeHttp.DEFAULT_SERVICE + "/" + hostname + "@" + kerberosDomain;
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
			throw new CmsException("Cannot create acceptor credentials for " + krb5Principal, gsse);
		}
	}

	public GSSCredential getAcceptorCredentials() {
		return acceptorCredentials;
	}

	public boolean isSingleUser() {
		return singleUser;
	}

	public final static Oid KERBEROS_OID;
	static {
		try {
			KERBEROS_OID = new Oid("1.3.6.1.5.5.2");
		} catch (GSSException e) {
			throw new IllegalStateException("Cannot create Kerberos OID", e);
		}
	}

}
