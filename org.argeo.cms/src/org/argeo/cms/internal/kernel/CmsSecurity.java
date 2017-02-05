package org.argeo.cms.internal.kernel;

import java.io.File;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.naming.DnsBrowser;
import org.argeo.node.NodeConstants;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

/** Low-level kernel security */
class CmsSecurity implements KernelConstants {
	private final static Log log = LogFactory.getLog(CmsSecurity.class);
	// http://java.sun.com/javase/6/docs/technotes/guides/security/jgss/jgss-features.html
	private final static Oid KERBEROS_OID;
	static {
		try {
			KERBEROS_OID = new Oid("1.3.6.1.5.5.2");
		} catch (GSSException e) {
			throw new IllegalStateException("Cannot create Kerberos OID", e);
		}
	}

	public final static int DEPLOYED = 30;
	public final static int STANDALONE = 20;
	public final static int DEV = 10;
	public final static int UNKNOWN = 0;

	private String hostname;

	private final int securityLevel;
	private Subject nodeSubject;

	// IPA
	private String kerberosDomain;
	private String service = null;
	private GSSCredential acceptorCredentials;

	private Path nodeKeyTab = KernelUtils.getOsgiInstancePath("node/krb5.keytab");
	private File keyStoreFile;

	public CmsSecurity() {
		if (!DeployConfig.isInitialized()) // first init
			FirstInit.prepareInstanceArea();

		securityLevel = evaluateSecurityLevel();
		// Configure JAAS first
		if (System.getProperty(JAAS_CONFIG_PROP) == null) {
			String jaasConfig = securityLevel < DEPLOYED ? JAAS_CONFIG : JAAS_CONFIG_IPA;
			URL url = getClass().getClassLoader().getResource(jaasConfig);
			System.setProperty(JAAS_CONFIG_PROP, url.toExternalForm());
		}
		// explicitly load JAAS configuration
		Configuration.getConfiguration();
		nodeSubject = logInKernel();

		// firstInit = !new File(getOsgiInstanceDir(), DIR_NODE).exists();

		// this.keyStoreFile = new File(KernelUtils.getOsgiInstanceDir(),
		// "node.p12");
		// createKeyStoreIfNeeded();
	}

	private int evaluateSecurityLevel() {
		int res = UNKNOWN;
		try (DnsBrowser dnsBrowser = new DnsBrowser()) {
			InetAddress localhost = InetAddress.getLocalHost();
			hostname = localhost.getHostName();
			String dnsZone = hostname.substring(hostname.indexOf('.') + 1);
			String ipfromDns = dnsBrowser.getRecord(hostname, localhost instanceof Inet6Address ? "AAAA" : "A");
			boolean consistentIp = localhost.getHostAddress().equals(ipfromDns);
			kerberosDomain = dnsBrowser.getRecord("_kerberos." + dnsZone, "TXT");
			if (consistentIp && kerberosDomain != null && Files.exists(nodeKeyTab)) {
				res = DEPLOYED;
			} else {
				res = STANDALONE;
				kerberosDomain = null;
				// FIXME make state more robust
			}
		} catch (UnknownHostException e) {
			hostname = "localhost";
			log.warn("Cannot determine hostname, using " + hostname + ":" + e.getMessage());
			res = STANDALONE;
		} catch (Exception e) {
			log.warn("Exception when evaluating security level, setting it to DEV", e);
			res = DEV;
		}

		if (res == UNKNOWN)
			throw new CmsException("Undefined security level");
		return res;
	}

	private Subject logInKernel() {
		final Subject nodeSubject = new Subject();

		CallbackHandler callbackHandler;
		if (Files.exists(nodeKeyTab)) {
			service = NodeConstants.NODE_SERVICE;
			callbackHandler = new CallbackHandler() {

				@Override
				public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
					for (Callback callback : callbacks)
						if (callback instanceof NameCallback)
							((NameCallback) callback).setName(getKerberosServicePrincipal());

				}
			};
			try {
				LoginContext kernelLc = new LoginContext(NodeConstants.LOGIN_CONTEXT_NODE, nodeSubject,
						callbackHandler);
				kernelLc.login();
			} catch (LoginException e) {
				throw new CmsException("Cannot log in kernel", e);
			}
		} else {
			callbackHandler = null;
			// try {
			// callbackHandler = (CallbackHandler)
			// Class.forName("com.sun.security.auth.callback.TextCallbackHandler")
			// .newInstance();
			// } catch (ReflectiveOperationException e) {
			// throw new CmsException("Cannot create text callback handler", e);
			// }
			try {
				LoginContext kernelLc = new LoginContext(NodeConstants.LOGIN_CONTEXT_SINGLE_USER, nodeSubject);
				kernelLc.login();
			} catch (LoginException e) {
				throw new CmsException("Cannot log in kernel", e);
			}
		}

		if (securityLevel >= DEPLOYED) {
			acceptorCredentials = logInAsAcceptor(nodeSubject);
		}
		return nodeSubject;
	}

	private String getKerberosServicePrincipal() {
		if (hostname == null || "locahost".equals(hostname) || kerberosDomain == null || service == null)
			throw new IllegalStateException("Cannot determine kerberos principal");
		return service + "/" + hostname + "@" + kerberosDomain;
	}

	private GSSCredential logInAsAcceptor(Subject nodeSubject) {
		// GSS
		Iterator<KerberosPrincipal> krb5It = nodeSubject.getPrincipals(KerberosPrincipal.class).iterator();
		if (!krb5It.hasNext())
			return null;
		KerberosPrincipal krb5Principal = null;
		while (krb5It.hasNext()) {
			KerberosPrincipal principal = krb5It.next();
			if (service == null && krb5Principal == null)// first as default
				krb5Principal = principal;
			if (service != null && principal.getName().equals(getKerberosServicePrincipal()))
				krb5Principal = principal;
		}

		if (krb5Principal == null)
			return null;

		GSSManager manager = GSSManager.getInstance();
		try {
			GSSName gssName = manager.createName(krb5Principal.getName(), null);
			GSSCredential serverCredentials = Subject.doAs(nodeSubject, new PrivilegedExceptionAction<GSSCredential>() {

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
	//
	// private Subject logInHardenedKernel() {
	// final Subject kernelSubject = new Subject();
	// createKeyStoreIfNeeded();
	//
	// CallbackHandler cbHandler = new CallbackHandler() {
	//
	// @Override
	// public void handle(Callback[] callbacks) throws IOException,
	// UnsupportedCallbackException {
	// // alias
	//// ((NameCallback) callbacks[1]).setName(AuthConstants.ROLE_KERNEL);
	// // store pwd
	// ((PasswordCallback) callbacks[2]).setPassword("changeit".toCharArray());
	// // key pwd
	// ((PasswordCallback) callbacks[3]).setPassword("changeit".toCharArray());
	// }
	// };
	// try {
	// LoginContext kernelLc = new
	// LoginContext(KernelConstants.LOGIN_CONTEXT_HARDENED_KERNEL,
	// kernelSubject,
	// cbHandler);
	// kernelLc.login();
	// } catch (LoginException e) {
	// throw new CmsException("Cannot log in kernel", e);
	// }
	// return kernelSubject;
	// }

	void destroy() {
		// Logout kernel
		try {
			LoginContext kernelLc = new LoginContext(NodeConstants.LOGIN_CONTEXT_NODE, nodeSubject);
			kernelLc.logout();
		} catch (LoginException e) {
			throw new CmsException("Cannot log out kernel", e);
		}

		// Security.removeProvider(SECURITY_PROVIDER);
	}

	File getNodeKeyStore() {
		return keyStoreFile;
	}

	public synchronized int getSecurityLevel() {
		return securityLevel;
	}

	public String getKerberosDomain() {
		return kerberosDomain;
	}

	public Subject getNodeSubject() {
		return nodeSubject;
	}

	public GSSCredential getServerCredentials() {
		return acceptorCredentials;
	}

	// public void setSecurityLevel(int newValue) {
	// if (newValue != STANDALONE || newValue != DEV)
	// throw new CmsException("Invalid value for security level " + newValue);
	// if (newValue >= securityLevel)
	// throw new CmsException(
	// "Impossible to increase security level (from " + securityLevel + " to " +
	// newValue + ")");
	// securityLevel = newValue;
	// }

	// private void createKeyStoreIfNeeded() {
	// // for (Provider provider : Security.getProviders())
	// // System.out.println(provider.getName());
	//
	// char[] ksPwd = "changeit".toCharArray();
	// char[] keyPwd = Arrays.copyOf(ksPwd, ksPwd.length);
	// if (!keyStoreFile.exists()) {
	// try {
	// keyStoreFile.getParentFile().mkdirs();
	// KeyStore keyStore = PkiUtils.getKeyStore(keyStoreFile, ksPwd);
	// // PkiUtils.generateSelfSignedCertificate(keyStore, new
	// // X500Principal(AuthConstants.ROLE_KERNEL), 1024,
	// // keyPwd);
	// PkiUtils.saveKeyStore(keyStoreFile, ksPwd, keyStore);
	// if (log.isDebugEnabled())
	// log.debug("Created keystore " + keyStoreFile);
	// } catch (Exception e) {
	// if (keyStoreFile.length() == 0)
	// keyStoreFile.delete();
	// log.error("Cannot create keystore " + keyStoreFile, e);
	// }
	// }
	// }

	// private final static String SECURITY_PROVIDER = "BC";// Bouncy Castle
	// private final static Log log;
	// static {
	// log = LogFactory.getLog(NodeSecurity.class);
	// // Make Bouncy Castle the default provider
	// Provider provider = new BouncyCastleProvider();
	// int position = Security.insertProviderAt(provider, 1);
	// if (position == -1)
	// log.error("Provider " + provider.getName()
	// + " already installed and could not be set as default");
	// Provider defaultProvider = Security.getProviders()[0];
	// if (!defaultProvider.getName().equals(SECURITY_PROVIDER))
	// log.error("Provider name is " + defaultProvider.getName()
	// + " but it should be " + SECURITY_PROVIDER);
	// }
}
