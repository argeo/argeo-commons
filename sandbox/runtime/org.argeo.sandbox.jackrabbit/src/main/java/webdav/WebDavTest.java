package webdav;

import java.io.FileInputStream;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.webdav.client.methods.CheckoutMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;

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

			String baseUrl = "http://localhost:7070/org.argeo.server.jackrabbit.webapp/default/";

			String fileName = "test.xml";

			// PUT
			PutMethod pm = new PutMethod(baseUrl + fileName);
			RequestEntity requestEntity = new InputStreamRequestEntity(
					new FileInputStream(fileName));
			pm.setRequestEntity(requestEntity);
			client.executeMethod(pm);

			// GET
			CheckoutMethod gm = new CheckoutMethod(baseUrl + fileName);
			client.executeMethod(gm);
			String responseGet = gm.getResponseBodyAsString();
			log.debug("CHECKOUT: " + responseGet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
