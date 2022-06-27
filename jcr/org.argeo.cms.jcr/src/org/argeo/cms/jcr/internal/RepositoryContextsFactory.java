package org.argeo.cms.jcr.internal;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;

import org.apache.jackrabbit.core.RepositoryContext;
import org.argeo.api.cms.CmsConstants;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsState;
import org.argeo.cms.internal.jcr.RepoConf;
import org.argeo.cms.internal.jcr.RepositoryBuilder;
import org.argeo.cms.jcr.internal.osgi.CmsJcrActivator;
import org.argeo.util.LangUtils;
import org.osgi.service.cm.ManagedServiceFactory;

/** A {@link ManagedServiceFactory} creating or referencing JCR repositories. */
public class RepositoryContextsFactory {
	private final static CmsLog log = CmsLog.getLog(RepositoryContextsFactory.class);
//	private final BundleContext bc = FrameworkUtil.getBundle(RepositoryServiceFactory.class).getBundleContext();

//	private Map<String, RepositoryContext> repositories = new HashMap<String, RepositoryContext>();
//	private Map<String, Object> pidToCn = new HashMap<String, Object>();

	private RepositoryContext repositoryContext;

	private CmsState cmsState;

	public void init() {
		Dictionary<String, Object> config = getNodeRepositoryConfig();
		deployRepository(config);
	}

	public void destroy() {
		if (this.repositoryContext != null) {
			this.repositoryContext.getRepository().shutdown();
		}
//		for (String pid : repositories.keySet()) {
//			try {
//				RepositoryContext repositoryContext = repositories.get(pid);
//				// Must start in another thread otherwise shutdown is interrupted
//				// TODO use an executor?
//				new Thread(() -> {
//					repositoryContext.getRepository().shutdown();
//					if (log.isDebugEnabled())
//						log.debug("Shut down repository " + pid
//								+ (pidToCn.containsKey(pid) ? " (" + pidToCn.get(pid) + ")" : ""));
//				}, "Shutdown JCR repository " + pid).start();
//			} catch (Exception e) {
//				log.error("Error when shutting down Jackrabbit repository " + pid, e);
//			}
//		}
	}

//	@Override
//	public String getName() {
//		return "Jackrabbit repository service factory";
//	}

	/** Override the provided config with the framework properties */
	private Dictionary<String, Object> getNodeRepositoryConfig() {
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		for (RepoConf repoConf : RepoConf.values()) {
			Object value = getFrameworkProp(CmsConstants.NODE_REPO_PROP_PREFIX + repoConf.name());
			if (value != null) {
				props.put(repoConf.name(), value);
				if (log.isDebugEnabled())
					log.debug("Set node repo configuration " + repoConf.name() + " to " + value);
			}
		}
		props.put(CmsConstants.CN, CmsConstants.NODE_REPOSITORY);
		return props;
	}

//	@Override
//	public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
	protected void deployRepository(Dictionary<String, Object> properties) {
//		if (repositories.containsKey(pid))
//			throw new IllegalArgumentException("Already a repository registered for " + pid);

		if (properties == null)
			return;

		Object cn = properties.get(CmsConstants.CN);
//		if (cn != null)
//			for (String otherPid : pidToCn.keySet()) {
//				Object o = pidToCn.get(otherPid);
//				if (cn.equals(o)) {
//					RepositoryContext repositoryContext = repositories.remove(otherPid);
//					repositories.put(pid, repositoryContext);
//					if (log.isDebugEnabled())
//						log.debug("Ignoring update of Jackrabbit repository " + cn);
//					// FIXME perform a proper update (also of the OSGi service)
//					return;
//				}
//			}

		try {
			Object labeledUri = properties.get(RepoConf.labeledUri.name());
			if (labeledUri == null) {
				RepositoryBuilder repositoryBuilder = new RepositoryBuilder();
				RepositoryContext repositoryContext = repositoryBuilder.createRepositoryContext(properties);
//				repositories.put(pid, repositoryContext);
//				Dictionary<String, Object> props = LangUtils.dict(Constants.SERVICE_PID, pid);
				Dictionary<String, Object> props = new Hashtable<>();
				// props.put(ArgeoJcrConstants.JCR_REPOSITORY_URI,
				// properties.get(RepoConf.labeledUri.name()));
				if (cn != null) {
					props.put(CmsConstants.CN, cn);
					// props.put(NodeConstants.JCR_REPOSITORY_ALIAS, cn);
//					pidToCn.put(pid, cn);
				}
				CmsJcrActivator.registerService(RepositoryContext.class, repositoryContext, props);
				this.repositoryContext = repositoryContext;
			} else {
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
//				Dictionary<String, Object> props = LangUtils.dict(Constants.SERVICE_PID, pid);
				Dictionary<String, Object> props = new Hashtable<>();
				props.put(RepoConf.labeledUri.name(),
						new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null)
								.toString());
				if (cn != null) {
					props.put(CmsConstants.CN, cn);
//					pidToCn.put(pid, cn);
				}
				CmsJcrActivator.registerService(Repository.class, repository, props);

				// home
				if (cn.equals(CmsConstants.NODE_REPOSITORY)) {
					Dictionary<String, Object> homeProps = LangUtils.dict(CmsConstants.CN, CmsConstants.EGO_REPOSITORY);
					EgoRepository homeRepository = new EgoRepository(repository, true);
					CmsJcrActivator.registerService(Repository.class, homeRepository, homeProps);
				}
			}
		} catch (RepositoryException | URISyntaxException | IOException e) {
			throw new IllegalStateException("Cannot create Jackrabbit repository " + properties, e);
		}

	}

//	@Override
//	public void deleted(String pid) {
//		RepositoryContext repositoryContext = repositories.remove(pid);
//		repositoryContext.getRepository().shutdown();
//		if (log.isDebugEnabled())
//			log.debug("Deleted repository " + pid);
//	}

	private String getFrameworkProp(String key) {
		return cmsState.getDeployProperty(key);
	}

	public void setCmsState(CmsState cmsState) {
		this.cmsState = cmsState;
	}

}
