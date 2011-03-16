package org.argeo.jackrabbit;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.jcr2dav.Jcr2davRepositoryFactory;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.DefaultRepositoryFactory;

public class JackrabbitRepositoryFactory extends DefaultRepositoryFactory
		implements RepositoryFactory, ArgeoJcrConstants {
	private final static Log log = LogFactory
			.getLog(JackrabbitRepositoryFactory.class);

	@SuppressWarnings("rawtypes")
	public Repository getRepository(Map parameters) throws RepositoryException {
		Repository repository = super.getRepository(parameters);
		if (repository != null)
			return repository;

		if (parameters.containsKey(JCR_REPOSITORY_URI)) {
			String uri = parameters.get(JCR_REPOSITORY_URI).toString();
			Map<String, String> params = new HashMap<String, String>();
			
			params.put(JcrUtils.REPOSITORY_URI, uri);
			repository = new Jcr2davRepositoryFactory().getRepository(params);
			if (repository == null)
				throw new ArgeoException("Remote Davex repository " + uri
						+ " not found");
			log.info("Initialized remote Jackrabbit repository " + repository
					+ " from uri " + uri);

		}

		return repository;
	}

}
