package org.argeo.jcr;

import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;

public class DefaultRepositoryFactory extends DefaultRepositoryRegister
		implements RepositoryFactory, ArgeoJcrConstants {
//	private final static Log log = LogFactory
//			.getLog(DefaultRepositoryFactory.class);

	@SuppressWarnings("rawtypes")
	public Repository getRepository(Map parameters) throws RepositoryException {
		if (parameters.containsKey(JCR_REPOSITORY_ALIAS)) {
			String alias = parameters.get(JCR_REPOSITORY_ALIAS).toString();
			if (getRepositories().containsKey(alias))
				return getRepositories().get(alias);
		}
		return null;
	}

}
