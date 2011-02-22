package org.argeo.jcr;

import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryFactory;

/** Allows to register repositories by name. */
public interface RepositoryRegister extends RepositoryFactory{
	public final static String JCR_REPOSITORY_NAME = "argeo.jcr.repository.name";

	public Map<String,Repository> getRepositories();
}
