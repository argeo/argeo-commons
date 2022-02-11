package org.argeo.jackrabbit.client;

import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.commons.ItemInfoCacheImpl;
import org.apache.jackrabbit.spi2davex.BatchReadConfig;
import org.apache.jackrabbit.spi2davex.Spi2davexRepositoryServiceFactory;

/**
 * Wrapper for {@link Spi2davexRepositoryServiceFactory} in order to create a
 * {@link ClientDavexRepositoryService}.
 */
public class ClientDavexRepositoryServiceFactory extends Spi2davexRepositoryServiceFactory {
	@Override
	public RepositoryService createRepositoryService(Map<?, ?> parameters) throws RepositoryException {
		// retrieve the repository uri
		String uri;
		if (parameters == null) {
			uri = System.getProperty(PARAM_REPOSITORY_URI);
		} else {
			Object repoUri = parameters.get(PARAM_REPOSITORY_URI);
			uri = (repoUri == null) ? null : repoUri.toString();
		}
		if (uri == null) {
			uri = DEFAULT_REPOSITORY_URI;
		}

		// load other optional configuration parameters
		BatchReadConfig brc = null;
		int itemInfoCacheSize = ItemInfoCacheImpl.DEFAULT_CACHE_SIZE;
		int maximumHttpConnections = 0;

		// since JCR-4120 the default workspace name is no longer set to 'default'
		// note: if running with JCR Server < 1.5 a default workspace name must
		// therefore be configured
		String workspaceNameDefault = null;

		if (parameters != null) {
			// batchRead config
			Object param = parameters.get(PARAM_BATCHREAD_CONFIG);
			if (param != null && param instanceof BatchReadConfig) {
				brc = (BatchReadConfig) param;
			}

			// itemCache size config
			param = parameters.get(PARAM_ITEMINFO_CACHE_SIZE);
			if (param != null) {
				try {
					itemInfoCacheSize = Integer.parseInt(param.toString());
				} catch (NumberFormatException e) {
					// ignore, use default
				}
			}

			// max connections config
			param = parameters.get(PARAM_MAX_CONNECTIONS);
			if (param != null) {
				try {
					maximumHttpConnections = Integer.parseInt(param.toString());
				} catch (NumberFormatException e) {
					// using default
				}
			}

			param = parameters.get(PARAM_WORKSPACE_NAME_DEFAULT);
			if (param != null) {
				workspaceNameDefault = param.toString();
			}
		}

		// FIXME adapt to changes in Jackrabbit
//		if (maximumHttpConnections > 0) {
//			return new ClientDavexRepositoryService(uri, workspaceNameDefault, brc, itemInfoCacheSize,
//					maximumHttpConnections);
//		} else {
//			return new ClientDavexRepositoryService(uri, workspaceNameDefault, brc, itemInfoCacheSize);
//		}
		return null;
	}

}
