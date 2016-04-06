package org.argeo.jackrabbit;

import java.io.File;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryContext;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.cache.CacheManager;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.RepositoryConfigurationParser;
import org.apache.jackrabbit.stats.RepositoryStatisticsImpl;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoJcrException;
import org.argeo.jcr.JcrRepositoryWrapper;
import org.argeo.jcr.RepoConf;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.xml.sax.InputSource;

public class ManagedJackrabbitRepository extends JcrRepositoryWrapper implements ManagedService, JackrabbitRepository {
	private final static Log log = LogFactory.getLog(ManagedJackrabbitRepository.class);

	// Node
	final static String REPO_TYPE = "repoType";
	// final static String REPO_CONFIGURATION = "argeo.node.repo.configuration";
	// final static String REPO_DEFAULT_WORKSPACE = "defaultWorkspace";
	// final static String REPO_DBURL = "dburl";
	// final static String REPO_DBUSER = "dbuser";
	// final static String REPO_DBPASSWORD = "dbpassword";
	// final static String REPO_MAX_POOL_SIZE = "maxPoolSize";
	// final static String REPO_MAX_CACHE_MB = "maxCacheMB";
	// final static String REPO_BUNDLE_CACHE_MB = "bundleCacheMB";
	// final static String REPO_EXTRACTOR_POOL_SIZE = "extractorPoolSize";
	// final static String REPO_SEARCH_CACHE_SIZE = "searchCacheSize";
	// final static String REPO_MAX_VOLATILE_INDEX_SIZE =
	// "maxVolatileIndexSize";

	private Dictionary<String, ?> properties;

	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		this.properties = properties;
		if (properties == null)
			return;

		JackrabbitNodeType type = JackrabbitNodeType.valueOf(prop(RepoConf.type).toString());
		try {
			repositoryContext = createNode(type);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ArgeoException("Cannot create Jackrabbit repository of type " + type, e);
		}
	}

	private RepositoryContext repositoryContext;

	public ManagedJackrabbitRepository() {
		// setBundleContext(Activator.getBundleContext());
		// JackrabbitNodeType type = JackrabbitNodeType.valueOf(prop(REPO_TYPE,
		// JackrabbitNodeType.h2.name()));
		// try {
		// repositoryContext = createNode(type);
		// cndFiles = Arrays.asList(DEFAULT_CNDS);
		// prepareDataModel();
		// } catch (Exception e) {
		// throw new ArgeoException("Cannot create Jackrabbit repository of type
		// " + type, e);
		// }
	}

	public void destroy() {
		((RepositoryImpl) getRepository()).shutdown();
	}

	RepositoryStatisticsImpl getRepositoryStatistics() {
		return repositoryContext.getRepositoryStatistics();
	}

	private RepositoryConfig getConfiguration(JackrabbitNodeType type, Hashtable<String, Object> props)
			throws RepositoryException {
		ClassLoader cl = getClass().getClassLoader();
		InputStream in = null;
		try {
			final String base = "/org/argeo/jackrabbit";
			switch (type) {
			case h2:
				in = cl.getResourceAsStream(base + "/repository-h2.xml");
				break;
			case postgresql:
				in = cl.getResourceAsStream(base + "/repository-postgresql.xml");
				break;
			case memory:
				in = cl.getResourceAsStream(base + "/repository-memory.xml");
				break;
			case localfs:
				in = cl.getResourceAsStream(base + "/repository-localfs.xml");
				break;
			default:
				throw new ArgeoJcrException("Unsupported node type " + type);
			}

			if (in == null)
				throw new ArgeoJcrException("Repository configuration not found");
			InputSource config = new InputSource(in);
			Properties jackrabbitVars = new Properties();
			// convert values to Strings, otherwise they are skipped
			for (String key : props.keySet())
				jackrabbitVars.setProperty(key, props.get(key).toString());
			RepositoryConfig repositoryConfig = RepositoryConfig.create(config, jackrabbitVars);
			return repositoryConfig;
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	private Hashtable<String, Object> getConfigurationProperties(JackrabbitNodeType type) {
		Hashtable<String, Object> props = new Hashtable<String, Object>();

		// home
		File osgiInstanceDir = getOsgiInstanceDir();
		File homeDir = new File(osgiInstanceDir, "repos/node");
		homeDir.mkdirs();
		// home cannot be overridden
		props.put(RepositoryConfigurationParser.REPOSITORY_HOME_VARIABLE, homeDir.getAbsolutePath());

		// common
		setProp(props, RepoConf.defaultWorkspace);
		setProp(props, RepoConf.maxPoolSize);
		// Jackrabbit defaults
		setProp(props, RepoConf.bundleCacheMB);
		// See http://wiki.apache.org/jackrabbit/Search
		setProp(props, RepoConf.extractorPoolSize);
		setProp(props, RepoConf.searchCacheSize);
		setProp(props, RepoConf.maxVolatileIndexSize);

		// specific
		String dburl;
		switch (type) {
		case h2:
			dburl = "jdbc:h2:" + homeDir.getPath() + "/h2/repository";
			setProp(props, RepoConf.dburl, dburl);
			setProp(props, RepoConf.dbuser, "sa");
			setProp(props, RepoConf.dbpassword, "");
			break;
		case postgresql:
			dburl = "jdbc:postgresql://localhost/demo";
			setProp(props, RepoConf.dburl, dburl);
			setProp(props, RepoConf.dbuser, "argeo");
			setProp(props, RepoConf.dbpassword, "argeo");
			break;
		case memory:
			break;
		case localfs:
			break;
		default:
			throw new ArgeoJcrException("Unsupported node type " + type);
		}
		return props;
	}

	private void setProp(Dictionary<String, Object> props, RepoConf key, String def) {
		Object value = prop(key);
		if (value == null)
			value = def;
		props.put(key.name(), value);
	}

	private void setProp(Dictionary<String, Object> props, RepoConf key) {
		setProp(props, key, null);
	}

	private Object prop(RepoConf key) {
		if (properties == null)
			throw new ArgeoJcrException("Properties are not set");
		Object value = properties.get(key.name());
		if (value == null)
			return key.getDefault();
		else
			return value;
	}

	private RepositoryContext createNode(JackrabbitNodeType type) throws RepositoryException {
		Hashtable<String, Object> props = getConfigurationProperties(type);
		RepositoryConfig repositoryConfig = getConfiguration(type, props);
		RepositoryContext repositoryContext = createJackrabbitRepository(repositoryConfig);
		RepositoryImpl repository = repositoryContext.getRepository();

		// cache
		Object maxCacheMbStr = prop(RepoConf.maxCacheMB);
		if (maxCacheMbStr != null) {
			Integer maxCacheMB = Integer.parseInt(maxCacheMbStr.toString());
			CacheManager cacheManager = repository.getCacheManager();
			cacheManager.setMaxMemory(maxCacheMB * 1024l * 1024l);
			cacheManager.setMaxMemoryPerCache((maxCacheMB / 4) * 1024l * 1024l);
		}

		// wrap the repository
		setRepository(repository);
		return repositoryContext;
	}

	private RepositoryContext createJackrabbitRepository(RepositoryConfig repositoryConfig) throws RepositoryException {
		long begin = System.currentTimeMillis();
		//
		// Actual repository creation
		//
		RepositoryContext repositoryContext = RepositoryContext.create(repositoryConfig);

		double duration = ((double) (System.currentTimeMillis() - begin)) / 1000;
		if (log.isTraceEnabled())
			log.trace("Created Jackrabbit repository in " + duration + " s, home: " + repositoryConfig.getHomeDir());

		return repositoryContext;
	}

	/*
	 * DATA MODEL
	 */

	public synchronized void waitForInit() {
		while (repositoryContext == null)
			try {
				wait(100);
			} catch (InterruptedException e) {
				return;
			}
	}

	private final static String OSGI_INSTANCE_AREA = "osgi.instance.area";

	private File getOsgiInstanceDir() {
		String instanceArea = System.getProperty(OSGI_INSTANCE_AREA);
		return new File(instanceArea.substring("file:".length())).getAbsoluteFile();
	}

	@Override
	public Session login(Credentials credentials, String workspaceName, Map<String, Object> attributes)
			throws LoginException, NoSuchWorkspaceException, RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void shutdown() {
		destroy();

	}

}
