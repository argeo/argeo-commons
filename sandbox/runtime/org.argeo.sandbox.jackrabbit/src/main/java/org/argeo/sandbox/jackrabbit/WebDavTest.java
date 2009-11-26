package org.argeo.sandbox.jackrabbit;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.webdav.client.methods.CheckoutMethod;
import org.apache.jackrabbit.webdav.client.methods.CopyMethod;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.PropPatchMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;

public class WebDavTest {
	private final static Log log = LogFactory.getLog(WebDavTest.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			HostConfiguration hostConfig = new HostConfiguration();
			hostConfig.setHost("localhost", 7070);
			// hostConfig.
			HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
			HttpConnectionManagerParams params = new HttpConnectionManagerParams();
			int maxHostConnections = 20;
			params.setMaxConnectionsPerHost(hostConfig, maxHostConnections);
			connectionManager.setParams(params);
			HttpClient client = new HttpClient(connectionManager);
			Credentials creds = new UsernamePasswordCredentials("demo", "demo");
			client.getState().setCredentials(AuthScope.ANY, creds);
			client.setHostConfiguration(hostConfig);
			// return client;

			String baseUrl = "http://localhost:7070/webdav/default/";
			// String fileName = "test.xml";
			String file00 = "dummy00.xls";
			String file01 = "dummy01.xls";
			String url00 = baseUrl + file00;
			String url01 = baseUrl + file01;
			String urlCopied = baseUrl + "test-copied.xls";

			// PUT
			log.debug("Create " + url00);
			PutMethod pm = new PutMethod(url00);
			RequestEntity requestEntity = new InputStreamRequestEntity(
					new FileInputStream(file00));
			pm.setRequestEntity(requestEntity);
			client.executeMethod(pm);
			log.debug("POST status: " + pm.getStatusCode() + " "
					+ pm.getStatusText());

			// PROP PATCH
			List<DavProperty> props = new ArrayList<DavProperty>();
			props.add(new DefaultDavProperty("auto-version",
					DeltaVConstants.XML_CHECKOUT_CHECKIN,
					DeltaVConstants.NAMESPACE));
			PropPatchMethod pp = new PropPatchMethod(url00, props);
			client.executeMethod(pp);
			log.debug("PROP PATCH status: " + pp.getStatusCode() + " "
					+ pp.getStatusText());

			// PUT (update)
			log.debug("Update " + url00);
			pm = new PutMethod(url00);
			requestEntity = new InputStreamRequestEntity(new FileInputStream(
					file01));
			pm.setRequestEntity(requestEntity);
			client.executeMethod(pm);
			log.debug("POST status: " + pm.getStatusCode() + " "
					+ pm.getStatusText());

			// COPY
			log.debug("Copy to " + urlCopied);
			DavMethod copy = new CopyMethod(url00, urlCopied, true);
			client.executeMethod(copy);

			log.debug("COPY status: " + copy.getStatusCode() + " "
					+ copy.getStatusText());

			// GET
			// CheckoutMethod gm = new CheckoutMethod(baseUrl + fileName);
			log.debug("Retrieve " + urlCopied);
			GetMethod gm = new GetMethod(urlCopied);
			client.executeMethod(gm);
			String responseGet = gm.getResponseBodyAsString();
			log.debug("GET status: " + gm.getStatusCode() + " "
					+ gm.getStatusText());
			// log.debug("GET: " + responseGet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
