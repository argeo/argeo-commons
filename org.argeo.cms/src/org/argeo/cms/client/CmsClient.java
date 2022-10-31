package org.argeo.cms.client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.WebSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.argeo.cms.auth.ConsoleCallbackHandler;
import org.argeo.cms.auth.RemoteAuthUtils;
import org.argeo.util.http.HttpHeader;

/** Utility to connect to a remote CMS node. */
public class CmsClient {
	public final static String CLIENT_LOGIN_CONTEXT = "CLIENT";

	private URI uri;

	private HttpClient httpClient;
	private String gssToken;

	public CmsClient(URI uri) {
		this.uri = uri;
	}

	public void login() {
		String server = uri.getHost();

		URL jaasUrl = CmsClient.class.getResource("jaas-client-ipa.cfg");
		System.setProperty("java.security.auth.login.config", jaasUrl.toExternalForm());
		try {
			LoginContext lc = new LoginContext(CLIENT_LOGIN_CONTEXT, new ConsoleCallbackHandler());
			lc.login();
			gssToken = RemoteAuthUtils.createGssToken(lc.getSubject(), "HTTP", server);
		} catch (LoginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {

		}
	}

	public String getAsString() {
		return getAsString(uri);
	}

	public String getAsString(URI uri) {
		uri = normalizeUri(uri);
		try {
			HttpClient httpClient = getHttpClient();

			HttpRequest request = HttpRequest.newBuilder().uri(uri) //
					.header(HttpHeader.AUTHORIZATION.getHeaderName(), HttpHeader.NEGOTIATE + " " + getGssToken()) //
					.build();
			BodyHandler<String> bodyHandler = BodyHandlers.ofString();
			HttpResponse<String> response = httpClient.send(request, bodyHandler);
			return response.body();
//			int responseCode = response.statusCode();
//			System.exit(responseCode);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Cannot read " + uri + " as a string", e);
		}
	}

	protected URI normalizeUri(URI uri) {
		if (uri.getHost() != null)
			return uri;
		try {
			String path = uri.getPath();
			if (path.startsWith("/")) {// absolute
				return new URI(this.uri.getScheme(), this.uri.getUserInfo(), this.uri.getHost(), this.uri.getPort(),
						path, uri.getQuery(), uri.getFragment());
			} else {
				String thisUriStr = this.uri.toString();
				if (!thisUriStr.endsWith("/"))
					thisUriStr = thisUriStr + "/";
				return URI.create(thisUriStr + path);
			}
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Cannot interpret " + uri, e);
		}
	}

	public URI getUri() {
		return uri;
	}

	String getGssToken() {
		return gssToken;
	}

	public HttpClient getHttpClient() {
		if (httpClient == null) {
			login();
			HttpClient client = HttpClient.newBuilder() //
					.sslContext(ipaSslContext()) //
					.version(HttpClient.Version.HTTP_1_1) //
					.build();
			httpClient = client;
		}
		return httpClient;
	}

	public CompletableFuture<WebSocket> newWebSocket(WebSocket.Listener listener) {
		return newWebSocket(uri, listener);
	}

	public CompletableFuture<WebSocket> newWebSocket(URI uri, WebSocket.Listener listener) {
		CompletableFuture<WebSocket> ws = getHttpClient().newWebSocketBuilder()
				.header(HttpHeader.AUTHORIZATION.getHeaderName(), HttpHeader.NEGOTIATE + " " + getGssToken())
				.buildAsync(uri, listener);
		return ws;
	}

	@SuppressWarnings("unchecked")
	protected SSLContext ipaSslContext() {
		try {
			final Collection<X509Certificate> certificates;
			Path caCertificatePath = Paths.get("/etc/ipa/ca.crt");
			if (Files.exists(caCertificatePath)) {
				CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
				try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(caCertificatePath))) {
					certificates = (Collection<X509Certificate>) certificateFactory.generateCertificates(in);
				}
			} else {
				certificates = null;
			}
			TrustManager[] noopTrustManager = new TrustManager[] { new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] xcs, String string) {
				}

				public void checkServerTrusted(X509Certificate[] xcs, String string) {
				}

				public X509Certificate[] getAcceptedIssuers() {
					if (certificates == null)
						return null;
					return certificates.toArray(new X509Certificate[certificates.size()]);
				}
			} };

			SSLContext sc = SSLContext.getInstance("ssl");
			sc.init(null, noopTrustManager, null);
			return sc;
		} catch (KeyManagementException | NoSuchAlgorithmException | CertificateException | IOException e) {
			throw new IllegalStateException("Cannot create SSL context ", e);
		}
	}

}
