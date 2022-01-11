package org.argeo.cms.jcr.internal;

import java.net.URI;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryFactory;

import org.apache.jackrabbit.core.RepositoryContext;
import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsLog;
import org.argeo.cms.internal.jcr.RepoConf;
import org.argeo.cms.internal.jcr.RepositoryBuilder;
import org.argeo.cms.jcr.internal.osgi.CmsJcrActivator;
import org.argeo.util.LangUtils;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

/** A {@link ManagedServiceFactory} creating or referencing JCR repositories. */
public class RepositoryContextsFactory implements ManagedServiceFactory {
	private final static CmsLog log = CmsLog.getLog(RepositoryContextsFactory.class);
//	private final BundleContext bc = FrameworkUtil.getBundle(RepositoryServiceFactory.class).getBundleContext();

	private Map<String, RepositoryContext> repositories = new HashMap<String, RepositoryContext>();
	private Map<String, Object> pidToCn = new HashMap<String, Object>();
	
	public void init() {
		
	}

	public void destroy() {
		for (String pid : repositories.keySet()) {
			try {
				repositories.get(pid).getRepository().shutdown();
				if (log.isDebugEnabled())
					log.debug("Shut down repository " + pid
							+ (pidToCn.containsKey(pid) ? " (" + pidToCn.get(pid) + ")" : ""));
			} catch (Exception e) {
				log.error("Error when shutting down Jackrabbit repository " + pid, e);
			}
		}
	}


	@Override
	public String getName() {
		return "Jackrabbit repository service factory";
	}

	@Override
	public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
		if (repositories.containsKey(pid))
			throw new IllegalArgumentException("Already a repository registered for " + pid);

		if (properties == null)
			return;

		if (repositories.containsKey(pid)) {
			log.warn("Ignore update of Jackrabbit repository " + pid);
			return;
		}

		try {
			Object labeledUri = properties.get(RepoConf.labeledUri.name());
			if (labeledUri == null) {
				RepositoryBuilder repositoryBuilder = new RepositoryBuilder();
				RepositoryContext repositoryContext = repositoryBuilder.createRepositoryContext(properties);
				repositories.put(pid, repositoryContext);
				Dictionary<String, Object> props = LangUtils.dict(Constants.SERVICE_PID, pid);
				// props.put(ArgeoJcrConstants.JCR_REPOSITORY_URI,
				// properties.get(RepoConf.labeledUri.name()));
				Object cn = properties.get(CmsConstants.CN);
				if (cn != null) {
					props.put(CmsConstants.CN, cn);
					// props.put(NodeConstants.JCR_REPOSITORY_ALIAS, cn);
					pidToCn.put(pid, cn);
				}
				CmsJcrActivator.registerService(RepositoryContext.class, repositoryContext, props);
			} else {
				try {
					Object cn = properties.get(CmsConstants.CN);
					Object defaultWorkspace = properties.get(RepoConf.defaultWorkspace.name());
					if (defaultWorkspace == null)
						defaultWorkspace = RepoConf.defaultWorkspace.getDefault();
					URI uri = new URI(labeledUri.toString());
//					RepositoryFactory repositoryFactory = bc
//							.getService(bc.getServiceReference(RepositoryFactory.class));
					RepositoryFactory repositoryFactory = CmsJcrActivator.getService(RepositoryFactory.class);
					Map<String, String> parameters = new HashMap<String, String>();
					parameters.put(RepoConf.labeledUri.name(), uri.toString());
					parameters.put(RepoConf.defaultWorkspace.name(), defaultWorkspace.toString());
					Repository repository = repositoryFactory.getRepository(parameters);
					// Repository repository = NodeUtils.getRepositoryByUri(repositoryFactory,
					// uri.toString());
					Dictionary<String, Object> props = LangUtils.dict(Constants.SERVICE_PID, pid);
					props.put(RepoConf.labeledUri.name(),
							new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null)
									.toString());
					if (cn != null) {
						props.put(CmsConstants.CN, cn);
						// props.put(NodeConstants.JCR_REPOSITORY_ALIAS, cn);
						pidToCn.put(pid, cn);
					}
					CmsJcrActivator.registerService(Repository.class, repository, props);

					// home
					if (cn.equals(CmsConstants.NODE_REPOSITORY)) {
						Dictionary<String, Object> homeProps = LangUtils.dict(CmsConstants.CN,
								CmsConstants.EGO_REPOSITORY);
						EgoRepository homeRepository = new EgoRepository(repository, true);
						CmsJcrActivator.registerService(Repository.class, homeRepository, homeProps);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException("Cannot create Jackrabbit repository " + pid, e);
		}

	}

	@Override
	public void deleted(String pid) {
		RepositoryContext repositoryContext = repositories.remove(pid);
		repositoryContext.getRepository().shutdown();
		if (log.isDebugEnabled())
			log.debug("Deleted repository " + pid);
	}


}
