package org.argeo.cms.internal.jcr;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;

import org.argeo.api.cms.CmsDeployment;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsConstants;
import org.argeo.jackrabbit.client.ClientDavexRepositoryFactory;
import org.argeo.jcr.JcrException;
import org.argeo.util.naming.LdapAttrs;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;

/** JCR specific init utilities. */
@Deprecated
public class JcrInitUtils {
	private final static CmsLog log = CmsLog.getLog(JcrInitUtils.class);
	private final static BundleContext bundleContext = FrameworkUtil.getBundle(JcrInitUtils.class).getBundleContext();

//	public static void addToDeployment(CmsDeployment nodeDeployment) {
//		// node repository
////		Dictionary<String, Object> provided = null;
//		Dictionary<String, Object> provided = nodeDeployment.getProps(CmsConstants.NODE_REPOS_FACTORY_PID,
//				CmsConstants.NODE);
//		Dictionary<String, Object> nodeConfig = JcrInitUtils.getNodeRepositoryConfig(provided);
//		// node repository is mandatory
//		nodeDeployment.addFactoryDeployConfig(CmsConstants.NODE_REPOS_FACTORY_PID, nodeConfig);
//
//		// additional repositories
////		dataModels: for (DataModels.DataModel dataModel : dataModels.getNonAbstractDataModels()) {
////			if (NodeConstants.NODE_REPOSITORY.equals(dataModel.getName()))
////				continue dataModels;
////			Dictionary<String, Object> config = JcrInitUtils.getRepositoryConfig(dataModel.getName(),
////					getProps(NodeConstants.NODE_REPOS_FACTORY_PID, dataModel.getName()));
////			if (config.size() != 0)
////				putFactoryDeployConfig(NodeConstants.NODE_REPOS_FACTORY_PID, config);
////		}
//
//	}

	/** Override the provided config with the framework properties */
	public static Dictionary<String, Object> getNodeRepositoryConfig(Dictionary<String, Object> provided) {
		Dictionary<String, Object> props = provided != null ? provided : new Hashtable<String, Object>();
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

//	public static Dictionary<String, Object> getRepositoryConfig(String dataModelName,
//			Dictionary<String, Object> provided) {
//		if (dataModelName.equals(CmsConstants.NODE_REPOSITORY) || dataModelName.equals(CmsConstants.EGO_REPOSITORY))
//			throw new IllegalArgumentException("Data model '" + dataModelName + "' is reserved.");
//		Dictionary<String, Object> props = provided != null ? provided : new Hashtable<String, Object>();
//		for (RepoConf repoConf : RepoConf.values()) {
//			Object value = getFrameworkProp(
//					CmsConstants.NODE_REPOS_PROP_PREFIX + dataModelName + '.' + repoConf.name());
//			if (value != null) {
//				props.put(repoConf.name(), value);
//				if (log.isDebugEnabled())
//					log.debug("Set " + dataModelName + " repo configuration " + repoConf.name() + " to " + value);
//			}
//		}
//		if (props.size() != 0)
//			props.put(CmsConstants.CN, dataModelName);
//		return props;
//	}

	private static void registerRemoteInit(String uri) {
		try {
			Repository repository = createRemoteRepository(new URI(uri));
			Hashtable<String, Object> properties = new Hashtable<>();
			properties.put(CmsConstants.CN, CmsConstants.NODE_INIT);
			properties.put(LdapAttrs.labeledURI.name(), uri);
			properties.put(Constants.SERVICE_RANKING, -1000);
			bundleContext.registerService(Repository.class, repository, properties);
		} catch (RepositoryException e) {
			throw new JcrException(e);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private static Repository createRemoteRepository(URI uri) throws RepositoryException {
		RepositoryFactory repositoryFactory = new ClientDavexRepositoryFactory();
		Map<String, String> params = new HashMap<String, String>();
		params.put(ClientDavexRepositoryFactory.JACKRABBIT_DAVEX_URI, uri.toString());
		// TODO make it configurable
		params.put(ClientDavexRepositoryFactory.JACKRABBIT_REMOTE_DEFAULT_WORKSPACE, CmsConstants.SYS_WORKSPACE);
		return repositoryFactory.getRepository(params);
	}

	private static String getFrameworkProp(String key, String def) {
		String value;
		if (bundleContext != null)
			value = bundleContext.getProperty(key);
		else
			value = System.getProperty(key);
		if (value == null)
			return def;
		return value;
	}

	private static String getFrameworkProp(String key) {
		return getFrameworkProp(key, null);
	}

}
