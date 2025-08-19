package org.argeo.cms.internal.http;

import org.argeo.cms.auth.RemoteAuthRequest;
import org.argeo.cms.auth.RemoteAuthResponse;

/**
 * A {@link CmsAuthenticator} allowing anonymous access. That is, already
 * authenticated users will be authenticated, otherwise the use will be
 * authenticated as anonymous.
 */
public class PublicCmsAuthenticator extends CmsAuthenticator {

	@Override
	protected boolean authIsRequired(RemoteAuthRequest remoteAuthRequest, RemoteAuthResponse remoteAuthResponse) {
		return false;
	}

}
