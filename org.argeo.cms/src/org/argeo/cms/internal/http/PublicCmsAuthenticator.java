package org.argeo.cms.internal.http;

import org.argeo.cms.auth.RemoteAuthRequest;
import org.argeo.cms.auth.RemoteAuthResponse;

public class PublicCmsAuthenticator extends CmsAuthenticator {

	@Override
	protected boolean authIsRequired(RemoteAuthRequest remoteAuthRequest, RemoteAuthResponse remoteAuthResponse) {
		return false;
	}

}
