package org.argeo.jackrabbit.client;

import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;

import org.apache.jackrabbit.jcr2spi.Jcr2spiRepositoryFactory;
import org.apache.jackrabbit.jcr2spi.RepositoryImpl;
import org.apache.jackrabbit.spi.RepositoryServiceFactory;

/** A customised {@link RepositoryFactory} access a remote DAVEX service. */
public class ClientDavexRepositoryFactory implements RepositoryFactory {
	public final static String JACKRABBIT_DAVEX_URI = ClientDavexRepositoryServiceFactory.PARAM_REPOSITORY_URI;
	public final static String JACKRABBIT_REMOTE_DEFAULT_WORKSPACE = ClientDavexRepositoryServiceFactory.PARAM_WORKSPACE_NAME_DEFAULT;

	@SuppressWarnings("rawtypes")
	@Override
	public Repository getRepository(Map parameters) throws RepositoryException {
		RepositoryServiceFactory repositoryServiceFactory = new ClientDavexRepositoryServiceFactory();
		return RepositoryImpl
				.create(new Jcr2spiRepositoryFactory.RepositoryConfigImpl(repositoryServiceFactory, parameters));
	}

}
