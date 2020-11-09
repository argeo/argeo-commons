package org.argeo.jackrabbit.client;

import javax.jcr.RepositoryException;

import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi2davex.BatchReadConfig;
import org.apache.jackrabbit.spi2davex.RepositoryServiceImpl;

/**
 * Wrapper for {@link RepositoryServiceImpl} in order to access the underlying
 * {@link HttpClientContext}.
 */
public class ClientDavexRepositoryService extends RepositoryServiceImpl {

	public ClientDavexRepositoryService(String jcrServerURI, BatchReadConfig batchReadConfig)
			throws RepositoryException {
		super(jcrServerURI, batchReadConfig);
	}

	public ClientDavexRepositoryService(String jcrServerURI, String defaultWorkspaceName,
			BatchReadConfig batchReadConfig, int itemInfoCacheSize, int maximumHttpConnections)
			throws RepositoryException {
		super(jcrServerURI, defaultWorkspaceName, batchReadConfig, itemInfoCacheSize, maximumHttpConnections);
	}

	public ClientDavexRepositoryService(String jcrServerURI, String defaultWorkspaceName,
			BatchReadConfig batchReadConfig, int itemInfoCacheSize) throws RepositoryException {
		super(jcrServerURI, defaultWorkspaceName, batchReadConfig, itemInfoCacheSize);
	}

	@Override
	protected HttpContext getContext(SessionInfo sessionInfo) throws RepositoryException {
		HttpClientContext result = HttpClientContext.create();
		result.setAuthCache(new NonSerialBasicAuthCache());
		return result;
	}

}
