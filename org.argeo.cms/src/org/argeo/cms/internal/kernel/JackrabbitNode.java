package org.argeo.cms.internal.kernel;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import javax.jcr.Repository;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.RepositoryConfigurationParser;
import org.argeo.ArgeoException;
import org.argeo.jackrabbit.JackrabbitWrapper;
import org.argeo.jcr.ArgeoJcrConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.xml.sax.InputSource;

/** Data storage */
class JackrabbitNode extends JackrabbitWrapper {
	private static Log log = LogFactory.getLog(JackrabbitNode.class);

	private ServiceRegistration<Repository> repositoryReg;

	public JackrabbitNode(BundleContext bundleContext) {
		setBundleContext(bundleContext);
		createNode();
		setCndFiles(Arrays.asList(KernelConstants.DEFAULT_CNDS));
		prepareDataModel();
	}

	public void publish() {
		Hashtable<String, String> regProps = new Hashtable<String, String>();
		regProps.put(ArgeoJcrConstants.JCR_REPOSITORY_ALIAS,
				ArgeoJcrConstants.ALIAS_NODE);
		repositoryReg = getBundleContext().registerService(Repository.class,
				this, regProps);
	}

	public void destroy() {
		repositoryReg.unregister();
		((RepositoryImpl) getRepository()).shutdown();
	}

	Dictionary<String, ?> getDefaults() {
		return KernelUtils.asDictionary(getClass().getClassLoader(),
				"/org/argeo/cms/internal/kernel/jackrabbit-node.properties");
	}

	InputSource getConfigurationXml(JackrabbitNodeTypes type) {
		ClassLoader cl = getClass().getClassLoader();
		InputStream in = cl
				.getResourceAsStream("/org/argeo/cms/internal/kernel/repository-h2.xml");
		return new InputSource(in);
	}

	Properties getDefaultConfigurationProperties() {
		Properties configurationProperties = new Properties();
		configurationProperties.setProperty(KernelConstants.REPO_DBUSER, "sa");
		configurationProperties
				.setProperty(KernelConstants.REPO_DBPASSWORD, "");
		configurationProperties.setProperty(KernelConstants.REPO_MAX_POOL_SIZE,
				"10");
		configurationProperties.setProperty(
				KernelConstants.REPO_DEFAULT_WORKSPACE, "main");
		return configurationProperties;
	}

	private void createNode() {
		Thread.currentThread().setContextClassLoader(
				getClass().getClassLoader());

		File osgiInstanceDir = KernelUtils
				.getOsgiInstanceDir(getBundleContext());
		File homeDir = new File(osgiInstanceDir, "node");

		// H2
		String dburl = "jdbc:h2:" + homeDir.getPath() + "/h2/repository";
		Properties configurationProperties = getDefaultConfigurationProperties();
		configurationProperties.setProperty(KernelConstants.REPO_DBURL, dburl);
		configurationProperties.put(
				RepositoryConfigurationParser.REPOSITORY_HOME_VARIABLE,
				homeDir.getAbsolutePath());
		// InputSource configurationXml = getConfigurationXml(null);

		// jackrabbitContainer.setHomeDirectory(homeDir);
		// jackrabbitContainer.setConfigurationProperties(configurationProperties);
		// jackrabbitContainer.setConfigurationXml(configurationXml);

		// jackrabbitContainer.init();

		RepositoryImpl repository = createJackrabbitRepository(
				configurationProperties, getConfigurationXml(null));

		setRepository(repository);
	}

	private RepositoryImpl createJackrabbitRepository(Properties vars,
			InputSource config) {
		File homeDirectory = null;
		long begin = System.currentTimeMillis();
		InputStream configurationIn = null;
		RepositoryImpl repository;
		try {
			RepositoryConfig repositoryConfig = RepositoryConfig.create(config,
					vars);

			//
			// Actual repository creation
			//
			repository = RepositoryImpl.create(repositoryConfig);

			double duration = ((double) (System.currentTimeMillis() - begin)) / 1000;
			if (log.isTraceEnabled())
				log.trace("Created Jackrabbit repository in " + duration
						+ " s, home: " + homeDirectory);

			return repository;
		} catch (Exception e) {
			throw new ArgeoException("Cannot create Jackrabbit repository "
					+ homeDirectory, e);
		} finally {
			IOUtils.closeQuietly(configurationIn);
		}
	}

}
