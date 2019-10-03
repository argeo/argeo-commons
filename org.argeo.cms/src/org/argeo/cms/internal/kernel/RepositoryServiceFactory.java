package org.argeo.cms.internal.kernel;

import java.net.URI;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.RepositoryContext;
import org.argeo.cms.CmsException;
import org.argeo.cms.internal.jcr.RepoConf;
import org.argeo.cms.internal.jcr.RepositoryBuilder;
import org.argeo.node.NodeConstants;
import org.argeo.util.LangUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

class RepositoryServiceFactory implements ManagedServiceFactory {
	private final static Log log = LogFactory.getLog(RepositoryServiceFactory.class);
	private final BundleContext bc = FrameworkUtil.getBundle(RepositoryServiceFactory.class).getBundleContext();

	private Map<String, RepositoryContext> repositories = new HashMap<String, RepositoryContext>();
	private Map<String, Object> pidToCn = new HashMap<String, Object>();

	@Override
	public String getName() {
		return "Jackrabbit repository service factory";
	}

	@Override
	public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
		if (repositories.containsKey(pid))
			throw new CmsException("Already a repository registered for " + pid);

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
				Dictionary<String, Object> props = LangUtils.dico(Constants.SERVICE_PID, pid);
				// props.put(ArgeoJcrConstants.JCR_REPOSITORY_URI,
				// properties.get(RepoConf.labeledUri.name()));
				Object cn = properties.get(NodeConstants.CN);
				if (cn != null) {
					props.put(NodeConstants.CN, cn);
					// props.put(NodeConstants.JCR_REPOSITORY_ALIAS, cn);
					pidToCn.put(pid, cn);
				}
				bc.registerService(RepositoryContext.class, repositoryContext, props);
			} else {
				try {
					Object cn = properties.get(NodeConstants.CN);
					Object defaultWorkspace = properties.get(RepoConf.defaultWorkspace.name());
					if (defaultWorkspace == null)
						defaultWorkspace = RepoConf.defaultWorkspace.getDefault();
					URI uri = new URI(labeledUri.toString());
					RepositoryFactory repositoryFactory = bc
							.getService(bc.getServiceReference(RepositoryFactory.class));
					Map<String, String> parameters = new HashMap<String, String>();
					parameters.put(RepoConf.labeledUri.name(), uri.toString());
					parameters.put(RepoConf.defaultWorkspace.name(), defaultWorkspace.toString());
					Repository repository = repositoryFactory.getRepository(parameters);
					// Repository repository = NodeUtils.getRepositoryByUri(repositoryFactory,
					// uri.toString());
					Dictionary<String, Object> props = LangUtils.dico(Constants.SERVICE_PID, pid);
					props.put(RepoConf.labeledUri.name(),
							new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null)
									.toString());
					if (cn != null) {
						props.put(NodeConstants.CN, cn);
						// props.put(NodeConstants.JCR_REPOSITORY_ALIAS, cn);
						pidToCn.put(pid, cn);
					}
					bc.registerService(Repository.class, repository, props);

					// home
					// TODO make a sperate home configurable
					if (cn.equals(NodeConstants.NODE)) {
						Dictionary<String, Object> homeProps = LangUtils.dico(NodeConstants.CN, NodeConstants.HOME);
						HomeRepository homeRepository = new HomeRepository(repository, true);
						bc.registerService(Repository.class, homeRepository, homeProps);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			throw new CmsException("Cannot create Jackrabbit repository " + pid, e);
		}

	}

	@Override
	public void deleted(String pid) {
		RepositoryContext repositoryContext = repositories.remove(pid);
		repositoryContext.getRepository().shutdown();
		if (log.isDebugEnabled())
			log.debug("Deleted repository " + pid);
	}

	public void shutdown() {
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
}
