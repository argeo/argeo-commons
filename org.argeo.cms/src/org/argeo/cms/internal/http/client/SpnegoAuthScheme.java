package org.argeo.cms.internal.http.client;

import java.net.URL;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Base64;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.AuthenticationException;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.auth.MalformedChallengeException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.DefaultHttpParams;
import org.apache.commons.httpclient.params.HttpParams;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

/** Implementation of the SPNEGO auth scheme. */
public class SpnegoAuthScheme implements AuthScheme {
//	private final static Log log = LogFactory.getLog(SpnegoAuthScheme.class);

	public static final String NAME = "Negotiate";
	private final static Oid KERBEROS_OID;
	static {
		try {
			KERBEROS_OID = new Oid("1.3.6.1.5.5.2");
		} catch (GSSException e) {
			throw new IllegalStateException("Cannot create Kerberos OID", e);
		}
	}

	private final static String DEFAULT_KERBEROS_SERVICE = "HTTP";

	private boolean complete = false;
	private String realm;

	@Override
	public void processChallenge(String challenge) throws MalformedChallengeException {
		// if(tokenStr!=null){
		// log.error("Received challenge while there is a token. Failing.");
		// complete = false;
		// }

	}

	@Override
	public String getSchemeName() {
		return NAME;
	}

	@Override
	public String getParameter(String name) {
		return null;
	}

	@Override
	public String getRealm() {
		return realm;
	}

	@Override
	public String getID() {
		return NAME;
	}

	@Override
	public boolean isConnectionBased() {
		return true;
	}

	@Override
	public boolean isComplete() {
		return complete;
	}

	@Override
	public String authenticate(Credentials credentials, String method, String uri) throws AuthenticationException {
		// log.debug("authenticate " + method + " " + uri);
		// return null;
		throw new UnsupportedOperationException();
	}

	@Override
	public String authenticate(Credentials credentials, HttpMethod method) throws AuthenticationException {
		GSSContext context = null;
		String tokenStr = null;
		String hostname;
		try {
			hostname = method.getURI().getHost();
		} catch (URIException e1) {
			throw new IllegalStateException("Cannot authenticate", e1);
		}
		String serverPrinc = DEFAULT_KERBEROS_SERVICE + "@" + hostname;

		try {
			// Get service's principal name
			GSSManager manager = GSSManager.getInstance();
			GSSName serverName = manager.createName(serverPrinc, GSSName.NT_HOSTBASED_SERVICE, KERBEROS_OID);

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
			return "Negotiate " + tokenStr;
		} catch (GSSException e) {
			complete = true;
			throw new AuthenticationException("Cannot authenticate to " + serverPrinc, e);
		}
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("usage: java " + SpnegoAuthScheme.class.getName() + " <url>");
			System.exit(1);
			return;
		}
		String url = args[0];

		URL jaasUrl = SpnegoAuthScheme.class.getResource("jaas.cfg");
		System.setProperty("java.security.auth.login.config", jaasUrl.toExternalForm());
		try {
			LoginContext lc = new LoginContext("SINGLE_USER");
			lc.login();

			AuthPolicy.registerAuthScheme(SpnegoAuthScheme.NAME, SpnegoAuthScheme.class);
			HttpParams params = DefaultHttpParams.getDefaultParams();
			ArrayList<String> schemes = new ArrayList<>();
			schemes.add(SpnegoAuthScheme.NAME);
			params.setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, schemes);
			params.setParameter(CredentialsProvider.PROVIDER, new HttpCredentialProvider());

			int responseCode = Subject.doAs(lc.getSubject(), new PrivilegedExceptionAction<Integer>() {
				public Integer run() throws Exception {
					HttpClient httpClient = new HttpClient();
					return httpClient.executeMethod(new GetMethod(url));
				}
			});
			System.out.println("Reponse code: " + responseCode);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
