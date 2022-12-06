package org.argeo.cms.dav;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.argeo.cms.http.HttpHeader;
import org.argeo.cms.http.HttpMethod;
import org.argeo.cms.http.HttpStatus;

public class DavClient {

	private HttpClient httpClient;

	public DavClient() {
		httpClient = HttpClient.newBuilder() //
//				.sslContext(insecureContext()) //
				.version(HttpClient.Version.HTTP_1_1) //
				.authenticator(new Authenticator() {

					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication("root", "demo".toCharArray());
					}

				}) //
				.build();
	}

	public void setProperty(String url, QName key, String value) {
		try {
			String body = """
					<?xml version="1.0" encoding="utf-8" ?>
					<D:propertyupdate xmlns:D="DAV:"
					""" //
					+ "xmlns:" + key.getPrefix() + "=\"" + key.getNamespaceURI() + "\">" + //
					"""
								<D:set>
									<D:prop>
							""" //
					+ "<" + key.getPrefix() + ":" + key.getLocalPart() + ">" + value + "</" + key.getPrefix() + ":"
					+ key.getLocalPart() + ">" + //
					"""
									</D:prop>
								</D:set>
							</D:propertyupdate>
							""";
			System.out.println(body);
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)) //
					.header(HttpHeader.DEPTH.getHeaderName(), DavDepth.DEPTH_1.getValue()) //
					.method(HttpMethod.PROPPATCH.name(), BodyPublishers.ofString(body)) //
					.build();
			BodyHandler<String> bodyHandler = BodyHandlers.ofString();
			HttpResponse<String> response = httpClient.send(request, bodyHandler);
			System.out.println(response.body());
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Iterator<DavResponse> listChildren(URI uri) {
		try {
			String body = """
					<?xml version="1.0" encoding="utf-8" ?>
					<D:propfind xmlns:D="DAV:">
					  <D:propname/>
					</D:propfind>""";
			HttpRequest request = HttpRequest.newBuilder().uri(uri) //
					.header(HttpHeader.DEPTH.getHeaderName(), DavDepth.DEPTH_1.getValue()) //
					.method(HttpMethod.PROPFIND.name(), BodyPublishers.ofString(body)) //
					.build();

			HttpResponse<String> responseStr = httpClient.send(request, BodyHandlers.ofString());
			System.out.println(responseStr.body());

			HttpResponse<InputStream> response = httpClient.send(request, BodyHandlers.ofInputStream());
			MultiStatusReader msReader = new MultiStatusReader(response.body(), uri.getPath());
			return msReader;
		} catch (IOException | InterruptedException e) {
			throw new IllegalStateException("Cannot list children of " + uri, e);
		}

	}

	public boolean exists(URI uri) {
		try {
			HttpRequest request = HttpRequest.newBuilder().uri(uri) //
					.header(HttpHeader.DEPTH.getHeaderName(), DavDepth.DEPTH_0.getValue()) //
					.method(HttpMethod.HEAD.name(), BodyPublishers.noBody()) //
					.build();
			BodyHandler<String> bodyHandler = BodyHandlers.ofString();
			HttpResponse<String> response = httpClient.send(request, bodyHandler);
			System.out.println(response.body());
			int responseStatusCode = response.statusCode();
			if (responseStatusCode == HttpStatus.NOT_FOUND.getCode())
				return false;
			if (responseStatusCode >= 200 && responseStatusCode < 300)
				return true;
			throw new IllegalStateException(
					"Cannot check whether " + uri + " exists: Unknown response status code " + responseStatusCode);
		} catch (IOException | InterruptedException e) {
			throw new IllegalStateException("Cannot check whether " + uri + " exists", e);
		}

	}

	public DavResponse get(URI uri) {
		try {
			String body = """
					<?xml version="1.0" encoding="utf-8" ?>
					<D:propfind xmlns:D="DAV:">
					  <D:allprop/>
					</D:propfind>""";
			HttpRequest request = HttpRequest.newBuilder().uri(uri) //
					.header(HttpHeader.DEPTH.getHeaderName(), DavDepth.DEPTH_0.getValue()) //
					.method(HttpMethod.PROPFIND.name(), BodyPublishers.ofString(body)) //
					.build();

//			HttpResponse<String> responseStr = httpClient.send(request, BodyHandlers.ofString());
//			System.out.println(responseStr.body());

			HttpResponse<InputStream> response = httpClient.send(request, BodyHandlers.ofInputStream());
			MultiStatusReader msReader = new MultiStatusReader(response.body());
			if (!msReader.hasNext())
				throw new IllegalArgumentException(uri + " does not exist");
			return msReader.next();
		} catch (IOException | InterruptedException e) {
			throw new IllegalStateException("Cannot list children of " + uri, e);
		}

	}

	public static void main(String[] args) {
		DavClient davClient = new DavClient();
//		Iterator<DavResponse> responses = davClient
//				.listChildren(URI.create("http://localhost/unstable/a2/org.argeo.tp.sdk/"));
		Iterator<DavResponse> responses = davClient
				.listChildren(URI.create("http://root:demo@localhost:7070/api/acr/srv/example"));
		while (responses.hasNext()) {
			DavResponse response = responses.next();
			System.out.println(response.getHref() + (response.isCollection() ? " (collection)" : ""));
			//System.out.println("  " + response.getPropertyNames(HttpStatus.OK));

		}
//		davClient.setProperty("http://localhost/unstable/a2/org.argeo.tp.sdk/org.opentest4j.1.2.jar",
//				CrName.uuid.qName(), UUID.randomUUID().toString());

	}

}
