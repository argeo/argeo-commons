package webdav;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;

public class WebDavTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			HostConfiguration hostConfig = new HostConfiguration();
			hostConfig.setHost("localhost", 8082);
			// hostConfig.
			HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
			HttpConnectionManagerParams params = new HttpConnectionManagerParams();
			int maxHostConnections = 20;
			params.setMaxConnectionsPerHost(hostConfig, maxHostConnections);
			connectionManager.setParams(params);
			HttpClient client = new HttpClient(connectionManager);
			Credentials creds = new UsernamePasswordCredentials("scanner1",
					"scanner1");
			client.getState().setCredentials(AuthScope.ANY, creds);
			client.setHostConfiguration(hostConfig);
			// return client;

			PutMethod pm = new PutMethod(
					"http://localhost:8082/webdav/scanner1_queque/uploader/image_"
							+ ".txt");
			String text = "this is the document content";
			pm.setRequestBody(text);
			client.executeMethod(pm);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
