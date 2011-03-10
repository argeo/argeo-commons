package org.argeo.jackrabbit;

import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;

import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.DefaultRepositoryRegister;

public class JackrabbitRepositoryFactory extends DefaultRepositoryRegister
		implements RepositoryFactory, ArgeoJcrConstants {

	@SuppressWarnings("rawtypes")
	public Repository getRepository(Map parameters) throws RepositoryException {
		String alias;
		if (parameters.containsKey(JCR_REPOSITORY_ALIAS)) {
			alias = parameters.get(JCR_REPOSITORY_ALIAS).toString();
		}
		return null;
	}

}
