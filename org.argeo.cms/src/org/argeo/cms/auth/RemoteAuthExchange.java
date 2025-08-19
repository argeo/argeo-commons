package org.argeo.cms.auth;

/**
 * Marker interface combining the semantics of {@link RemoteAuthRequest} and
 * {@link RemoteAuthResponse} in a single object.
 */
public interface RemoteAuthExchange extends RemoteAuthRequest, RemoteAuthResponse {
}
