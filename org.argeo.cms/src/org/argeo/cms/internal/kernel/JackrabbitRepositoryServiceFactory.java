package org.argeo.cms.internal.kernel;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryContext;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.cache.CacheManager;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.RepositoryConfigurationParser;
import org.argeo.ArgeoException;
import org.argeo.cms.CmsException;
import org.argeo.jackrabbit.JackrabbitNodeType;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.ArgeoJcrException;
import org.argeo.node.RepoConf;
import org.argeo.util.LangUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.xml.sax.InputSource;

public class JackrabbitRepositoryServiceFactory implements ManagedServiceFactory {
	private final static Log log = LogFactory.getLog(JackrabbitRepositoryServiceFactory.class);
	private final BundleContext bc = FrameworkUtil.getBundle(JackrabbitRepositoryServiceFactory.class)
			.getBundleContext();

	// Node
	final static String REPO_TYPE = "repoType";

	private Map<String, RepositoryContext> repositories = new HashMap<String, RepositoryContext>();

	@Override
	public String getName() {
		return "Jackrabbit repository service factory";
	}

	@Override
	public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
		if (repositories.containsKey(pid))
			throw new ArgeoException("Already a repository registered for " + pid);

		if (properties == null)
			return;

		if (repositories.containsKey(pid)) {
			log.warn("Ignore update of Jackrabbit repository " + pid);
			return;
		}

		try {
			RepositoryContext repositoryContext = createNode(properties);
			repositories.put(pid, repositoryContext);
			Dictionary<String, Object> props = LangUtils.init(Constants.SERVICE_PID, pid);
			props.put(ArgeoJcrConstants.JCR_REPOSITORY_URI, properties.get(RepoConf.uri.name()));
			bc.registerService(JackrabbitRepository.class, repositoryContext.getRepository(), props);
		} catch (Exception e) {
			throw new ArgeoException("Cannot create Jackrabbit repository " + pid, e);
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
			} catch (Exception e) {
				log.error("Error when shutting down Jackrabbit repository " + pid, e);
			}
		}
	}

	private RepositoryConfig getConfiguration(Dictionary<String, ?> properties) throws RepositoryException {
		JackrabbitNodeType type = JackrabbitNodeType.valueOf(prop(properties, RepoConf.type).toString());
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
			Properties jackrabbitVars = getConfigurationProperties(type, properties);
			RepositoryConfig repositoryConfig = RepositoryConfig.create(config, jackrabbitVars);
			return repositoryConfig;
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	private Properties getConfigurationProperties(JackrabbitNodeType type, Dictionary<String, ?> properties) {
		Properties props = new Properties();
		keys: for (Enumeration<String> keys = properties.keys(); keys.hasMoreElements();) {
			String key = keys.nextElement();
			if (key.equals(ConfigurationAdmin.SERVICE_FACTORYPID) || key.equals(Constants.SERVICE_PID))
				continue keys;
			String value = prop(properties, RepoConf.valueOf(key));
			if (value != null)
				props.put(key, value);
		}

		// home
		// File osgiInstanceDir = getOsgiInstanceDir();
		String homeUri = props.getProperty(RepoConf.uri.name());
		Path homePath;
		try {
			homePath = Paths.get(new URI(homeUri));
		} catch (URISyntaxException e) {
			throw new CmsException("Invalid repository home URI", e);
		}
		// File homeDir = new File(osgiInstanceDir, "repos/node");
		File homeDir = homePath.toFile();
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

	private void setProp(Properties props, RepoConf key, String def) {
		Object value = props.get(key.name());
		if (value == null)
			value = def;
		if (value == null)
			value = key.getDefault();
		if (value != null)
			props.put(key.name(), value.toString());
	}

	private void setProp(Properties props, RepoConf key) {
		setProp(props, key, null);
	}

	private String prop(Dictionary<String, ?> properties, RepoConf key) {
		Object value = properties.get(key.name());
		if (value == null)
			return key.getDefault() != null ? key.getDefault().toString() : null;
		else
			return value.toString();
	}

	private RepositoryContext createNode(Dictionary<String, ?> properties) throws RepositoryException {
		RepositoryConfig repositoryConfig = getConfiguration(properties);
		RepositoryContext repositoryContext = createJackrabbitRepository(repositoryConfig);
		RepositoryImpl repository = repositoryContext.getRepository();

		// cache
		Object maxCacheMbStr = prop(properties, RepoConf.maxCacheMB);
		if (maxCacheMbStr != null) {
			Integer maxCacheMB = Integer.parseInt(maxCacheMbStr.toString());
			CacheManager cacheManager = repository.getCacheManager();
			cacheManager.setMaxMemory(maxCacheMB * 1024l * 1024l);
			cacheManager.setMaxMemoryPerCache((maxCacheMB / 4) * 1024l * 1024l);
		}

		return repositoryContext;
	}

	private RepositoryContext createJackrabbitRepository(RepositoryConfig repositoryConfig) throws RepositoryException {
		ClassLoader currentContextCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(JackrabbitRepositoryServiceFactory.class.getClassLoader());
		try {
			long begin = System.currentTimeMillis();
			//
			// Actual repository creation
			//
			RepositoryContext repositoryContext = RepositoryContext.create(repositoryConfig);

			double duration = ((double) (System.currentTimeMillis() - begin)) / 1000;
			if (log.isTraceEnabled())
				log.trace(
						"Created Jackrabbit repository in " + duration + " s, home: " + repositoryConfig.getHomeDir());

			return repositoryContext;
		} finally {
			Thread.currentThread().setContextClassLoader(currentContextCl);
		}
	}

}
