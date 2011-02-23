package org.argeo.jcr;

import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryFactory;

/** Allows to register repositories by name. */
public interface RepositoryRegister extends RepositoryFactory {
	public final static String JCR_REPOSITORY_NAME = "argeo.jcr.repository.name";

	/**
	 * The registered {@link Repository} as a read-only map. Note that this
	 * method should be called for each access in order to be sure to be up to
	 * date in case repositories have registered/unregistered
	 */
	public Map<String, Repository> getRepositories();
}
