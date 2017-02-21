package org.argeo.cms.internal.http;

import java.io.IOException;
import java.net.URL;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.auth.HttpRequestCallbackHandler;
import org.argeo.node.NodeConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.http.HttpContext;

class DataHttpContext implements HttpContext {
	private final static Log log = LogFactory.getLog(DataHttpContext.class);

	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

	// FIXME Make it more unique
	private final String httpAuthRealm;
	private final boolean forceBasic;

	public DataHttpContext(String httpAuthrealm, boolean forceBasic) {
		this.httpAuthRealm = httpAuthrealm;
		this.forceBasic = forceBasic;
	}

	public DataHttpContext(String httpAuthrealm) {
		this(httpAuthrealm, false);
	}

	@Override
	public boolean handleSecurity(final HttpServletRequest request, HttpServletResponse response) throws IOException {

		if (log.isTraceEnabled())
			HttpUtils.logRequestHeaders(log, request);
		LoginContext lc;
		try {
			lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER, new HttpRequestCallbackHandler(request, response));
			lc.login();
			// return true;
		} catch (LoginException e) {
			// CallbackHandler token = extractHttpAuth(request, response);
			// String token = request.getHeader(HttpUtils.HEADER_AUTHORIZATION);
			// if (token != null) {
			// try {
			// lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER);
			// lc.login();
			// } catch (LoginException e1) {
			// throw new CmsException("Could not login", e1);
			// }
			// } else {
			lc = processUnauthorized(request, response);
			if (lc == null)
				return false;
			// }
		}

		// still required by open session in view
		// TODO remove it
		request.setAttribute(NodeConstants.LOGIN_CONTEXT_USER, lc);
		return true;
	}

	@Override
	public URL getResource(String name) {
		return bc.getBundle().getResource(name);
	}

	@Override
	public String getMimeType(String name) {
		return null;
	}

	protected LoginContext processUnauthorized(HttpServletRequest request, HttpServletResponse response) {
		// anonymous
		try {
			LoginContext lc = new LoginContext(NodeConstants.LOGIN_CONTEXT_USER);
			lc.login();
			return lc;
		} catch (LoginException e1) {
			if (log.isDebugEnabled())
				log.error("Cannot log in as anonymous", e1);
			return null;
		}
	}

	// protected CallbackHandler extractHttpAuth(final HttpServletRequest
	// httpRequest, HttpServletResponse httpResponse) {
	// String authHeader =
	// httpRequest.getHeader(HttpUtils.HEADER_AUTHORIZATION);
	// if (authHeader != null) {
	// StringTokenizer st = new StringTokenizer(authHeader);
	// if (st.hasMoreTokens()) {
	// String basic = st.nextToken();
	// if (basic.equalsIgnoreCase("Basic")) {
	// try {
	// // TODO manipulate char[]
	// String credentials = new String(Base64.decodeBase64(st.nextToken()),
	// "UTF-8");
	// // log.debug("Credentials: " + credentials);
	// int p = credentials.indexOf(":");
	// if (p != -1) {
	// final String login = credentials.substring(0, p).trim();
	// final char[] password = credentials.substring(p +
	// 1).trim().toCharArray();
	// return new CallbackHandler() {
	// public void handle(Callback[] callbacks) {
	// for (Callback cb : callbacks) {
	// if (cb instanceof NameCallback)
	// ((NameCallback) cb).setName(login);
	// else if (cb instanceof PasswordCallback)
	// ((PasswordCallback) cb).setPassword(password);
	// else if (cb instanceof HttpRequestCallback) {
	// ((HttpRequestCallback) cb).setRequest(httpRequest);
	// ((HttpRequestCallback) cb).setResponse(httpResponse);
	// }
	// }
	// }
	// };
	// } else {
	// throw new CmsException("Invalid authentication token");
	// }
	// } catch (Exception e) {
	// throw new CmsException("Couldn't retrieve authentication", e);
	// }
	// } else if (basic.equalsIgnoreCase("Negotiate")) {
	// // FIXME generalise
	// String _targetName;
	// try {
	// _targetName = NodeHttp.DEFAULT_SERVICE + "@"
	// + InetAddress.getLocalHost().getCanonicalHostName();
	// } catch (UnknownHostException e) {
	// throw new CmsException("Cannot determins target name", e);
	// }
	// String spnegoToken = st.nextToken();
	// byte[] authToken = Base64.decodeBase64(spnegoToken);
	// GSSManager manager = GSSManager.getInstance();
	// try {
	//// Oid krb5Oid = new Oid("1.3.6.1.5.5.2"); //
	// http://java.sun.com/javase/6/docs/technotes/guides/security/jgss/jgss-features.html
	//// GSSName gssName = manager.createName(_targetName,
	// GSSName.NT_HOSTBASED_SERVICE, krb5Oid);
	//// GSSCredential serverCreds = manager.createCredential(gssName,
	// GSSCredential.INDEFINITE_LIFETIME,
	//// krb5Oid, GSSCredential.ACCEPT_ONLY);
	// GSSCredential serverCreds = Activator.getAcceptorCredentials();
	// if(serverCreds==null)
	// throw new CmsException("No GSS server credentials available");
	// GSSContext gContext = manager.createContext(serverCreds);
	//
	// if (gContext == null) {
	// log.debug("SpnegoUserRealm: failed to establish GSSContext");
	// } else {
	// while (!gContext.isEstablished()) {
	// byte[] outToken = gContext.acceptSecContext(authToken, 0,
	// authToken.length);
	// String outTokenStr = Base64.encodeBase64String(outToken);
	// httpResponse.setHeader("WWW-Authenticate", "Negotiate " + outTokenStr);
	// }
	// if (gContext.isEstablished()) {
	// String clientName = gContext.getSrcName().toString();
	// String role = clientName.substring(clientName.indexOf('@') + 1);
	//
	// log.debug("SpnegoUserRealm: established a security context");
	// log.debug("Client Principal is: " + gContext.getSrcName());
	// log.debug("Server Principal is: " + gContext.getTargName());
	// log.debug("Client Default Role: " + role);
	//
	// // TODO log in
	// }
	// }
	//
	// } catch (GSSException gsse) {
	// log.warn(gsse, gsse);
	// }
	//
	// }
	// }
	// }
	// return null;
	// }

	protected void askForWwwAuth(HttpServletRequest request, HttpServletResponse response) {
		response.setStatus(401);
		// response.setHeader(HttpUtils.HEADER_WWW_AUTHENTICATE, "basic
		// realm=\"" + httpAuthRealm + "\"");
		if (org.argeo.cms.internal.kernel.Activator.getAcceptorCredentials() != null && !forceBasic)// SPNEGO
			response.setHeader(HttpUtils.HEADER_WWW_AUTHENTICATE, "Negotiate");
		else
			response.setHeader(HttpUtils.HEADER_WWW_AUTHENTICATE, "Basic realm=\"" + httpAuthRealm + "\"");

		// response.setDateHeader("Date", System.currentTimeMillis());
		// response.setDateHeader("Expires", System.currentTimeMillis() + (24 *
		// 60 * 60 * 1000));
		// response.setHeader("Accept-Ranges", "bytes");
		// response.setHeader("Connection", "Keep-Alive");
		// response.setHeader("Keep-Alive", "timeout=5, max=97");
		// response.setContentType("text/html; charset=UTF-8");

	}

}
