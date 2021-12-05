package org.argeo.cms.internal.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;
import java.util.UUID;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.RepositoryContext;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.cache.CacheManager;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.RepositoryConfigurationParser;
import org.argeo.api.NodeConstants;
import org.argeo.cms.jcr.internal.CmsPaths;
import org.xml.sax.InputSource;

/** Can interpret properties in order to create an actual JCR repository. */
public class RepositoryBuilder {
	private final static Log log = LogFactory.getLog(RepositoryBuilder.class);

	public RepositoryContext createRepositoryContext(Dictionary<String, ?> properties)
			throws RepositoryException, IOException {
		RepositoryConfig repositoryConfig = createRepositoryConfig(properties);
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

	RepositoryConfig createRepositoryConfig(Dictionary<String, ?> properties) throws RepositoryException, IOException {
		JackrabbitType type = JackrabbitType.valueOf(prop(properties, RepoConf.type).toString());
		ClassLoader cl = getClass().getClassLoader();
		final String base = "/org/argeo/cms/internal/jcr";
		try (InputStream in = cl.getResourceAsStream(base + "/repository-" + type.name() + ".xml")) {
			if (in == null)
				throw new IllegalArgumentException("Repository configuration not found");
			InputSource config = new InputSource(in);
			Properties jackrabbitVars = getConfigurationProperties(type, properties);
			// RepositoryConfig repositoryConfig = RepositoryConfig.create(config,
			// jackrabbitVars);

			// custom configuration parser
			CustomRepositoryConfigurationParser parser = new CustomRepositoryConfigurationParser(jackrabbitVars);
			parser.setClassLoader(cl);
			RepositoryConfig repositoryConfig = parser.parseRepositoryConfig(config);
			repositoryConfig.init();

			// set the proper classloaders
			repositoryConfig.getSecurityConfig().getSecurityManagerConfig().setClassLoader(cl);
			repositoryConfig.getSecurityConfig().getAccessManagerConfig().setClassLoader(cl);
//			for (WorkspaceConfig workspaceConfig : repositoryConfig.getWorkspaceConfigs()) {
//				workspaceConfig.getSecurityConfig().getAccessControlProviderConfig().setClassLoader(cl);
//			}
			return repositoryConfig;
		}
	}

	private Properties getConfigurationProperties(JackrabbitType type, Dictionary<String, ?> properties) {
		Properties props = new Properties();
		for (Enumeration<String> keys = properties.keys(); keys.hasMoreElements();) {
			String key = keys.nextElement();
			props.put(key, properties.get(key));
		}

		// cluster id
		// cf. https://wiki.apache.org/jackrabbit/Clustering
		// TODO deal with multiple repos
		String clusterId = System.getProperty("org.apache.jackrabbit.core.cluster.node_id");
		String clusterIdProp = props.getProperty(RepoConf.clusterId.name());
		if (clusterId != null) {
			if (clusterIdProp != null)
				throw new IllegalArgumentException("Cluster id defined as System properties and in deploy config");
			props.put(RepoConf.clusterId.name(), clusterId);
		} else {
			clusterId = clusterIdProp;
		}

		// home
		String homeUri = props.getProperty(RepoConf.labeledUri.name());
		Path homePath;
		if (homeUri == null) {
			String cn = props.getProperty(NodeConstants.CN);
			assert cn != null;
			if (clusterId != null) {
				homePath = CmsPaths.getRepoDirPath(cn + '/' + clusterId);
			} else {
				homePath = CmsPaths.getRepoDirPath(cn);
			}
		} else {
			try {
				URI uri = new URI(homeUri);
				String host = uri.getHost();
				if (host == null || host.trim().equals("")) {
					homePath = Paths.get(uri).toAbsolutePath();
				} else {
					// TODO remote at this stage?
					throw new IllegalArgumentException("Cannot manage repository path for host " + host);
				}
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("Invalid repository home URI", e);
			}
		}
		// TODO use Jackrabbit API (?)
		Path rootUuidPath = homePath.resolve("repository/meta/rootUUID");
		try {
			if (!Files.exists(rootUuidPath)) {
				Files.createDirectories(rootUuidPath.getParent());
				Files.write(rootUuidPath, UUID.randomUUID().toString().getBytes());
			}
			// File homeDir = homePath.toFile();
			// homeDir.mkdirs();
		} catch (IOException e) {
			throw new RuntimeException("Cannot set up repository  home " + homePath, e);
		}
		// home cannot be overridden
		props.put(RepositoryConfigurationParser.REPOSITORY_HOME_VARIABLE, homePath.toString());

		setProp(props, RepoConf.indexesBase, CmsPaths.getRepoIndexesBase().toString());
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
			dburl = "jdbc:h2:" + homePath.toAbsolutePath() + "/h2/repository";
			setProp(props, RepoConf.dburl, dburl);
			setProp(props, RepoConf.dbuser, "sa");
			setProp(props, RepoConf.dbpassword, "");
			break;
		case h2_postgresql:
			dburl = "jdbc:h2:" + homePath.toAbsolutePath() + "/h2/repository;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
			setProp(props, RepoConf.dburl, dburl);
			setProp(props, RepoConf.dbuser, "sa");
			setProp(props, RepoConf.dbpassword, "");
			break;
		case postgresql:
		case postgresql_ds:
		case postgresql_cluster:
		case postgresql_cluster_ds:
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
			throw new IllegalArgumentException("Unsupported node type " + type);
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

	private RepositoryContext createJackrabbitRepository(RepositoryConfig repositoryConfig) throws RepositoryException {
		ClassLoader currentContextCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(RepositoryBuilder.class.getClassLoader());
		try {
			long begin = System.currentTimeMillis();
			//
			// Actual repository creation
			//
			RepositoryContext repositoryContext = RepositoryContext.create(repositoryConfig);

			double duration = ((double) (System.currentTimeMillis() - begin)) / 1000;
			if (log.isDebugEnabled())
				log.debug(
						"Created Jackrabbit repository in " + duration + " s, home: " + repositoryConfig.getHomeDir());

			return repositoryContext;
		} finally {
			Thread.currentThread().setContextClassLoader(currentContextCl);
		}
	}

}
