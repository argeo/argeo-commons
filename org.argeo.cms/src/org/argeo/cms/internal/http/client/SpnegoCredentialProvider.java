package org.argeo.cms.internal.http.client;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.CredentialsNotAvailableException;
import org.apache.commons.httpclient.auth.CredentialsProvider;

/** SPNEGO credential provider */
public class SpnegoCredentialProvider implements CredentialsProvider {

	@Override
	public Credentials getCredentials(AuthScheme scheme, String host, int port, boolean proxy)
			throws CredentialsNotAvailableException {
		return new Credentials() {
		};
	}

}
