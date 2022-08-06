package org.argeo.cms.client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import org.argeo.cms.auth.ConsoleCallbackHandler;
import org.argeo.cms.auth.RemoteAuthUtils;
import org.argeo.util.http.HttpHeader;

public class SpnegoHttpClient {
	public final static String CLIENT_LOGIN_CONTEXT = "CLIENT";

	public static void main(String[] args) throws MalformedURLException {
//		String principal = System.getProperty("javax.security.auth.login.name");
		if (args.length == 0) {
			System.err.println("usage: java -Djavax.security.auth.login.name=<principal@REALM> "
					+ SpnegoHttpClient.class.getName() + " <url>");
			System.exit(1);
			return;
		}
		String url = args[0];
		URL u = new URL(url);
		String server = u.getHost();

		URL jaasUrl = SpnegoHttpClient.class.getResource("jaas-client-ipa.cfg");
		System.setProperty("java.security.auth.login.config", jaasUrl.toExternalForm());
		try {
			LoginContext lc = new LoginContext(CLIENT_LOGIN_CONTEXT, new ConsoleCallbackHandler());
			lc.login();

			HttpClient httpClient = openHttpClient(lc.getSubject());
			String token = RemoteAuthUtils.createGssToken(lc.getSubject(), "HTTP", server);

			HttpRequest request = HttpRequest.newBuilder().uri(u.toURI()) //
					.header(HttpHeader.AUTHORIZATION.getHeaderName(), HttpHeader.NEGOTIATE + " " + token) //
					.build();
			BodyHandler<String> bodyHandler = BodyHandlers.ofString();
			HttpResponse<String> response = httpClient.send(request, bodyHandler);
			System.out.println(response.body());
			int responseCode = response.statusCode();
			System.exit(responseCode);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static HttpClient openHttpClient(Subject subject) {
		HttpClient client = HttpClient.newBuilder() //
				.sslContext(ipaSslContext()) //
				.version(HttpClient.Version.HTTP_1_1) //
				.build();

		return client;
	}

	@SuppressWarnings("unchecked")
	static SSLContext ipaSslContext() {
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
