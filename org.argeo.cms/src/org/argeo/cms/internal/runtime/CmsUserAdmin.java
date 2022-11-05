package org.argeo.cms.internal.runtime;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
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

import org.argeo.api.cms.CmsAuth;
import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsState;
import org.argeo.api.cms.transaction.WorkControl;
import org.argeo.api.cms.transaction.WorkTransaction;
import org.argeo.cms.CmsDeployProperty;
import org.argeo.cms.dns.DnsBrowser;
import org.argeo.cms.osgi.useradmin.AggregatingUserAdmin;
import org.argeo.cms.osgi.useradmin.DirectoryUserAdmin;
import org.argeo.cms.osgi.useradmin.UserDirectory;
import org.argeo.cms.runtime.DirectoryConf;
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

	private CmsState cmsState;

	public CmsUserAdmin() {
		super(CmsConstants.SYSTEM_ROLES_BASEDN, CmsConstants.TOKENS_BASEDN);
	}

	public void start() {
		super.start();
		List<Dictionary<String, Object>> configs = getUserDirectoryConfigs();
		for (Dictionary<String, Object> config : configs) {
			enableUserDirectory(config);
//			if (userDirectory.getRealm().isPresent())
//				loadIpaJaasConfiguration();
		}
		log.debug(() -> "CMS user admin available");
	}

	public void stop() {
//		for (UserDirectory userDirectory : getUserDirectories()) {
//			removeUserDirectory(userDirectory);
//		}
		super.stop();
	}

	protected List<Dictionary<String, Object>> getUserDirectoryConfigs() {
		List<Dictionary<String, Object>> res = new ArrayList<>();
		Path nodeBase = cmsState.getDataPath(KernelConstants.DIR_PRIVATE);
		List<String> uris = new ArrayList<>();

		// node roles
		String nodeRolesUri = null;// getFrameworkProp(CmsConstants.ROLES_URI);
		String baseNodeRoleDn = CmsConstants.SYSTEM_ROLES_BASEDN;
		if (nodeRolesUri == null && nodeBase != null) {
			nodeRolesUri = baseNodeRoleDn + ".ldif";
			Path nodeRolesFile = nodeBase.resolve(nodeRolesUri);
			if (!Files.exists(nodeRolesFile))
				try {
					Files.copy(CmsUserAdmin.class.getResourceAsStream(baseNodeRoleDn + ".ldif"), nodeRolesFile);
				} catch (IOException e) {
					throw new RuntimeException("Cannot copy demo resource", e);
				}
			// nodeRolesUri = nodeRolesFile.toURI().toString();
		}
		if (nodeRolesUri != null)
			uris.add(nodeRolesUri);

		// node tokens
		String nodeTokensUri = null;// getFrameworkProp(CmsConstants.TOKENS_URI);
		String baseNodeTokensDn = CmsConstants.TOKENS_BASEDN;
		if (nodeTokensUri == null && nodeBase != null) {
			nodeTokensUri = baseNodeTokensDn + ".ldif";
			Path nodeTokensFile = nodeBase.resolve(nodeTokensUri);
			if (!Files.exists(nodeTokensFile))
				try {
					Files.copy(CmsUserAdmin.class.getResourceAsStream(baseNodeTokensDn + ".ldif"), nodeTokensFile);
				} catch (IOException e) {
					throw new RuntimeException("Cannot copy demo resource", e);
				}
			// nodeRolesUri = nodeRolesFile.toURI().toString();
		}
		if (nodeTokensUri != null)
			uris.add(nodeTokensUri);

		// Business roles
//		String userAdminUris = getFrameworkProp(CmsConstants.USERADMIN_URIS);
		List<String> userAdminUris = CmsStateImpl.getDeployProperties(cmsState, CmsDeployProperty.DIRECTORY);// getFrameworkProp(CmsConstants.USERADMIN_URIS);
		for (String userAdminUri : userAdminUris) {
			if (userAdminUri == null)
				continue;
//			if (!userAdminUri.trim().equals(""))
			uris.add(userAdminUri);
		}

		if (uris.size() == 0 && nodeBase != null) {
			// TODO put this somewhere else
			String demoBaseDn = "dc=example,dc=com";
			String userAdminUri = demoBaseDn + ".ldif";
			Path businessRolesFile = nodeBase.resolve(userAdminUri);
			Path systemRolesFile = nodeBase.resolve("ou=roles,ou=node.ldif");
			if (!Files.exists(businessRolesFile))
				try {
					Files.copy(CmsUserAdmin.class.getResourceAsStream(demoBaseDn + ".ldif"), businessRolesFile);
					if (!Files.exists(systemRolesFile))
						Files.copy(CmsUserAdmin.class.getResourceAsStream("example-ou=roles,ou=node.ldif"),
								systemRolesFile);
				} catch (IOException e) {
					throw new RuntimeException("Cannot copy demo resources", e);
				}
			// userAdminUris = businessRolesFile.toURI().toString();
			log.warn("## DEV Using dummy base DN " + demoBaseDn);
			// TODO downgrade security level
		}

		// Interprets URIs
		for (String uri : uris) {
			URI u;
			try {
				u = new URI(uri);
				if (u.getPath() == null)
					throw new IllegalArgumentException(
							"URI " + uri + " must have a path in order to determine base DN");
				if (u.getScheme() == null) {
					if (uri.startsWith("/") || uri.startsWith("./") || uri.startsWith("../"))
						u = Paths.get(uri).toRealPath().toUri();
					else if (!uri.contains("/")) {
						// u = KernelUtils.getOsgiInstanceUri(KernelConstants.DIR_NODE + '/' + uri);
						u = new URI(uri);
					} else
						throw new IllegalArgumentException("Cannot interpret " + uri + " as an uri");
				} else if (u.getScheme().equals(DirectoryConf.SCHEME_FILE)) {
					u = Paths.get(u).toRealPath().toUri();
				}
			} catch (Exception e) {
				throw new RuntimeException("Cannot interpret " + uri + " as an uri", e);
			}

			try {
				Dictionary<String, Object> properties = DirectoryConf.uriAsProperties(u.toString());
				res.add(properties);
			} catch (Exception e) {
				log.error("Cannot load user directory " + u, e);
			}
		}

		return res;
	}

	public UserDirectory enableUserDirectory(Dictionary<String, ?> properties) {
		String uri = (String) properties.get(DirectoryConf.uri.name());
		Object realm = properties.get(DirectoryConf.realm.name());
		URI u;
		try {
			if (uri == null) {
				String baseDn = (String) properties.get(DirectoryConf.baseDn.name());
				u = KernelUtils.getOsgiInstanceUri(KernelConstants.DIR_PRIVATE + '/' + baseDn + ".ldif");
			} else if (realm != null) {
				u = null;
			} else {
				u = new URI(uri);
			}
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Badly formatted URI " + uri, e);
		}

		// Create
		UserDirectory userDirectory = new DirectoryUserAdmin(u, properties);
//		if (realm != null || DirectoryConf.SCHEME_LDAP.equals(u.getScheme())
//				|| DirectoryConf.SCHEME_LDAPS.equals(u.getScheme())) {
//			userDirectory = new LdapUserAdmin(properties);
//		} else if (DirectoryConf.SCHEME_FILE.equals(u.getScheme())) {
//			userDirectory = new LdifUserAdmin(u, properties);
//		} else if (DirectoryConf.SCHEME_OS.equals(u.getScheme())) {
//			userDirectory = new OsUserDirectory(u, properties);
//			singleUser = true;
//		} else {
//			throw new IllegalArgumentException("Unsupported scheme " + u.getScheme());
//		}
		String basePath = userDirectory.getBase();

		addUserDirectory(userDirectory);
		if (isSystemRolesBaseDn(basePath)) {
			addStandardSystemRoles();
		}
		if (log.isDebugEnabled()) {
			log.debug("User directory " + userDirectory.getBase() + (u != null ? " [" + u.getScheme() + "]" : "")
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
			loadIpaJaasConfiguration();
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
						LoginContext nodeLc = CmsAuth.NODE.newLoginContext(callbackHandler);
						nodeLc.login();
						acceptorCredentials = logInAsAcceptor(nodeLc.getSubject(), servicePrincipal);
					} catch (LoginException e) {
						throw new IllegalStateException("Cannot log in kernel", e);
					}
				}
			}

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

	private void loadIpaJaasConfiguration() {
		if (CmsStateImpl.getDeployProperty(cmsState, CmsDeployProperty.JAVA_LOGIN_CONFIG) == null) {
			String jaasConfig = KernelConstants.JAAS_CONFIG_IPA;
			URL url = getClass().getClassLoader().getResource(jaasConfig);
			KernelUtils.setJaasConfiguration(url);
			log.debug("Set IPA JAAS configuration.");
		}
	}

	protected String getKerberosServicePrincipal(String realm) {
		if (!Files.exists(nodeKeyTab))
			return null;
		List<String> dns = CmsStateImpl.getDeployProperties(cmsState, CmsDeployProperty.DNS);
		String hostname = CmsStateImpl.getDeployProperty(cmsState, CmsDeployProperty.HOST);
		try (DnsBrowser dnsBrowser = new DnsBrowser(dns)) {
			hostname = hostname != null ? hostname : InetAddress.getLocalHost().getHostName();
			String dnsZone = hostname.substring(hostname.indexOf('.') + 1);
			String ipv4fromDns = dnsBrowser.getRecord(hostname, "A");
			String ipv6fromDns = dnsBrowser.getRecord(hostname, "AAAA");
			if (ipv4fromDns == null && ipv6fromDns == null)
				throw new IllegalStateException("hostname " + hostname + " is not registered in DNS");
			// boolean consistentIp = localhost.getHostAddress().equals(ipfromDns);
			String kerberosDomain = dnsBrowser.getRecord("_kerberos." + dnsZone, "TXT");
			if (kerberosDomain != null && kerberosDomain.equals(realm)) {
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

	public void setCmsState(CmsState cmsState) {
		this.cmsState = cmsState;
	}

}
