package org.argeo.cms.internal.kernel;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.RepositoryContext;
import org.argeo.cms.CmsException;
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
			RepositoryBuilder repositoryBuilder = new RepositoryBuilder();
			RepositoryContext repositoryContext = repositoryBuilder.createRepositoryContext(properties);
			repositories.put(pid, repositoryContext);
			Dictionary<String, Object> props = LangUtils.init(Constants.SERVICE_PID, pid);
			// props.put(ArgeoJcrConstants.JCR_REPOSITORY_URI,
			// properties.get(RepoConf.labeledUri.name()));
			Object cn = properties.get(NodeConstants.CN);
			if (cn != null) {
				props.put(NodeConstants.CN, cn);
				props.put(NodeConstants.JCR_REPOSITORY_ALIAS, cn);
				pidToCn.put(pid, cn);
			}
			bc.registerService(RepositoryContext.class, repositoryContext, props);
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
