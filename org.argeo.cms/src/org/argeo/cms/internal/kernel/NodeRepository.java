package org.argeo.cms.internal.kernel;

import org.argeo.jackrabbit.JackrabbitWrapper;
import org.argeo.jcr.ArgeoJcrConstants;

/** Jacrabbit based data layer */
class NodeRepository extends JackrabbitWrapper implements KernelConstants,
		ArgeoJcrConstants {
//	private static Log log = LogFactory.getLog(NodeRepository.class);
//
//	private RepositoryContext repositoryContext;
//
//	public NodeRepository() {
//		setBundleContext(Activator.getBundleContext());
//		JackrabbitNodeType type = JackrabbitNodeType.valueOf(prop(REPO_TYPE,
//				h2.name()));
//		try {
//			repositoryContext = createNode(type);
//			setCndFiles(Arrays.asList(DEFAULT_CNDS));
//			prepareDataModel();
//		} catch (Exception e) {
//			throw new ArgeoException(
//					"Cannot create Jackrabbit repository of type " + type, e);
//		}
//	}
//
//	public void destroy() {
//		((RepositoryImpl) getRepository()).shutdown();
//	}
//
//	RepositoryStatisticsImpl getRepositoryStatistics() {
//		return repositoryContext.getRepositoryStatistics();
//	}
//
//	private RepositoryConfig getConfiguration(JackrabbitNodeType type,
//			Hashtable<String, Object> vars) throws RepositoryException {
//		ClassLoader cl = getClass().getClassLoader();
//		InputStream in = null;
//		try {
//			final String base = "/org/argeo/cms/internal/kernel";
//			switch (type) {
//			case h2:
//				in = cl.getResourceAsStream(base + "/repository-h2.xml");
//				break;
//			case postgresql:
//				in = cl.getResourceAsStream(base + "/repository-postgresql.xml");
//				break;
//			case memory:
//				in = cl.getResourceAsStream(base + "/repository-memory.xml");
//				break;
//			case localfs:
//				in = cl.getResourceAsStream(base + "/repository-localfs.xml");
//				break;
//			default:
//				throw new CmsException("Unsupported node type " + type);
//			}
//
//			if (in == null)
//				throw new CmsException("Repository configuration not found");
//			InputSource config = new InputSource(in);
//			Properties jackrabbitProps = new Properties();
//			jackrabbitProps.putAll(vars);
//			RepositoryConfig repositoryConfig = RepositoryConfig.create(config,
//					jackrabbitProps);
//			return repositoryConfig;
//		} finally {
//			IOUtils.closeQuietly(in);
//		}
//	}
//
//	private Hashtable<String, Object> getConfigurationProperties(
//			JackrabbitNodeType type) {
//		// use Hashtable to ease integration with Properties
//		Hashtable<String, Object> defaults = new Hashtable<String, Object>();
//
//		// home
//		File osgiInstanceDir = KernelUtils.getOsgiInstanceDir();
//		File homeDir = new File(osgiInstanceDir, DIR_NODE);
//		// home cannot be overridden
//		defaults.put(RepositoryConfigurationParser.REPOSITORY_HOME_VARIABLE,
//				homeDir.getAbsolutePath());
//
//		// common
//		setProp(defaults, REPO_DEFAULT_WORKSPACE, "main");
//		setProp(defaults, REPO_MAX_POOL_SIZE, "10");
//		// Jackrabbit defaults
//		setProp(defaults, REPO_BUNDLE_CACHE_MB, "8");
//		// See http://wiki.apache.org/jackrabbit/Search
//		setProp(defaults, REPO_EXTRACTOR_POOL_SIZE, "0");
//		setProp(defaults, REPO_SEARCH_CACHE_SIZE, "1000");
//		setProp(defaults, REPO_MAX_VOLATILE_INDEX_SIZE, "1048576");
//
//		// specific
//		String dburl;
//		switch (type) {
//		case h2:
//			dburl = "jdbc:h2:" + homeDir.getPath() + "/h2/repository";
//			setProp(defaults, REPO_DBURL, dburl);
//			setProp(defaults, REPO_DBUSER, "sa");
//			setProp(defaults, REPO_DBPASSWORD, "");
//			break;
//		case postgresql:
//			dburl = "jdbc:postgresql://localhost/demo";
//			setProp(defaults, REPO_DBURL, dburl);
//			setProp(defaults, REPO_DBUSER, "argeo");
//			setProp(defaults, REPO_DBPASSWORD, "argeo");
//			break;
//		case memory:
//			break;
//		case localfs:
//			break;
//		default:
//			throw new CmsException("Unsupported node type " + type);
//		}
//		return defaults;
//	}
//
//	private void setProp(Dictionary<String, Object> props, String key,
//			String defaultValue) {
//		String value = prop(key, defaultValue);
//		props.put(key, value);
//	}
//
//	private String prop(String key, String defaultValue) {
//		// TODO use OSGi CM instead of Framework/System properties
//		return KernelUtils.getFrameworkProp(key, defaultValue);
//	}
//
//	private RepositoryContext createNode(JackrabbitNodeType type)
//			throws RepositoryException {
//		Hashtable<String, Object> vars = getConfigurationProperties(type);
//		RepositoryConfig repositoryConfig = getConfiguration(type, vars);
//		RepositoryContext repositoryContext = createJackrabbitRepository(repositoryConfig);
//		RepositoryImpl repository = repositoryContext.getRepository();
//
//		// cache
//		String maxCacheMbStr = prop(REPO_MAX_CACHE_MB, null);
//		if (maxCacheMbStr != null) {
//			Integer maxCacheMB = Integer.parseInt(maxCacheMbStr);
//			CacheManager cacheManager = repository.getCacheManager();
//			cacheManager.setMaxMemory(maxCacheMB * 1024l * 1024l);
//			cacheManager.setMaxMemoryPerCache((maxCacheMB / 4) * 1024l * 1024l);
//		}
//
//		// wrap the repository
//		setRepository(repository);
//		return repositoryContext;
//	}
//
//	private RepositoryContext createJackrabbitRepository(
//			RepositoryConfig repositoryConfig) throws RepositoryException {
//		long begin = System.currentTimeMillis();
//		//
//		// Actual repository creation
//		//
//		RepositoryContext repositoryContext = RepositoryContext
//				.create(repositoryConfig);
//
//		double duration = ((double) (System.currentTimeMillis() - begin)) / 1000;
//		if (log.isTraceEnabled())
//			log.trace("Created Jackrabbit repository in " + duration
//					+ " s, home: " + repositoryConfig.getHomeDir());
//
//		return repositoryContext;
//	}
}
