package org.argeo.osgi.boot;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/** Launch an OSGi framework and deploy a CMS Node into it. */
public class NodeRunner {
	private Long timeout = 30 * 1000l;
	private final Path baseDir;
	private final Path confDir;
	private final Path dataDir;

	private String baseUrl = "http://forge.argeo.org/data/java/argeo-2.1/";
	private String distributionUrl = null;

	private Framework framework = null;

	public NodeRunner(String distributionUrl, Path baseDir) {
		this.distributionUrl = distributionUrl;
		Path mavenBase = Paths.get(System.getProperty("user.home") + "/.m2/repository");
		Path osgiBase = Paths.get("/user/share/osgi");
		if (Files.exists(mavenBase)) {
			Path mavenPath = mavenBase.resolve(distributionUrl);
			if (Files.exists(mavenPath))
				baseUrl = mavenBase.toUri().toString();
		} else if (Files.exists(osgiBase)) {
			Path osgiPath = osgiBase.resolve(distributionUrl);
			if (Files.exists(osgiPath))
				baseUrl = osgiBase.toUri().toString();
		}

		this.baseDir = baseDir;
		this.confDir = this.baseDir.resolve("state");
		this.dataDir = this.baseDir.resolve("data");

	}

	public void start() {
		long begin = System.currentTimeMillis();
		// log4j
		Path log4jFile = confDir.resolve("log4j.properties");
		if (!Files.exists(log4jFile))
			copyResource("/org/argeo/osgi/boot/log4j.properties", log4jFile);
		System.setProperty("log4j.configuration", "file://" + log4jFile.toAbsolutePath());

		// Start Equinox
		try {
			ServiceLoader<FrameworkFactory> ff = ServiceLoader.load(FrameworkFactory.class);
			FrameworkFactory frameworkFactory = ff.iterator().next();
			Map<String, String> configuration = new HashMap<String, String>();
			configuration.put("osgi.configuration.area", confDir.toAbsolutePath().toString());
			configuration.put("osgi.instance.area", dataDir.toAbsolutePath().toString());
			defaultConfiguration(configuration);

			framework = frameworkFactory.newFramework(configuration);
			framework.start();
			info("## Date : " + new Date());
			info("## Data : " + dataDir.toAbsolutePath());
		} catch (Exception e) {
			throw new IllegalStateException("Cannot start OSGi framework", e);
		}
		BundleContext bundleContext = framework.getBundleContext();
		try {

			// Spring configs currently require System properties
			// System.getProperties().putAll(configuration);

			// expected by JAAS as System.property FIXME
			System.setProperty("osgi.instance.area", bundleContext.getProperty("osgi.instance.area"));

			// OSGi bootstrap
			OsgiBoot osgiBoot = new OsgiBoot(bundleContext);

			osgiBoot.installUrls(osgiBoot.getDistributionUrls(distributionUrl, baseUrl));

			// Start runtime
			Properties startProperties = new Properties();
			// TODO make it possible to override it
			startProperties.put("argeo.osgi.start.2.node",
					"org.eclipse.equinox.http.servlet,org.eclipse.equinox.http.jetty,"
							+ "org.eclipse.equinox.metatype,org.eclipse.equinox.cm,org.eclipse.rap.rwt.osgi");
			startProperties.put("argeo.osgi.start.3.node", "org.argeo.cms");
			startProperties.put("argeo.osgi.start.4.node",
					"org.eclipse.gemini.blueprint.extender,org.eclipse.equinox.http.registry");
			osgiBoot.startBundles(startProperties);

			// Find node repository
			ServiceReference<?> sr = null;
			while (sr == null) {
				sr = bundleContext.getServiceReference("javax.jcr.Repository");
				if (System.currentTimeMillis() - begin > timeout)
					throw new RuntimeException("Could find node after " + timeout + "ms");
				Thread.sleep(100);
			}
			Object nodeDeployment = bundleContext.getService(sr);
			info("Node Deployment " + nodeDeployment);

			// Initialization completed
			long duration = System.currentTimeMillis() - begin;
			info("## CMS Launcher initialized in " + (duration / 1000) + "s " + (duration % 1000) + "ms");
		} catch (Exception e) {
			shutdown();
			throw new RuntimeException("Cannot start CMS", e);
		} finally {

		}
	}

	private void defaultConfiguration(Map<String, String> configuration) {
		// all permissions to OSGi security manager
		Path policyFile = confDir.resolve("node.policy");
		if (!Files.exists(policyFile))
			copyResource("/org/argeo/osgi/boot/node.policy", policyFile);
		configuration.put("java.security.policy", "file://" + policyFile.toAbsolutePath());

		configuration.put("org.eclipse.rap.workbenchAutostart", "false");
		configuration.put("org.eclipse.equinox.http.jetty.autostart", "false");
		configuration.put("org.osgi.framework.bootdelegation",
				"com.sun.jndi.ldap,com.sun.jndi.ldap.sasl,com.sun.security.jgss,com.sun.jndi.dns,"
						+ "com.sun.nio.file,com.sun.nio.sctp");

		// Do clean
		// configuration.put("osgi.clean", "true");
		// if (args.length == 0) {
		// configuration.put("osgi.console", "");
		// }
	}

	public void shutdown() {
		try {
			framework.stop();
			framework.waitForStop(15 * 1000);
		} catch (Exception silent) {
		}
	}

	public Path getConfDir() {
		return confDir;
	}

	public Path getDataDir() {
		return dataDir;
	}

	public Framework getFramework() {
		return framework;
	}

	public static void main(String[] args) {
		try {
			// Prepare directories
			Path executionDir = Paths.get(System.getProperty("user.dir"));

			String distributionUrl;
			if (args.length == 0) {
				distributionUrl = "org/argeo/commons/org.argeo.dep.cms.sdk/2.1.65/org.argeo.dep.cms.sdk-2.1.65.jar";
			} else {
				distributionUrl = args[0];
			}

			NodeRunner nodeRunner = new NodeRunner(distributionUrl, executionDir);
			nodeRunner.start();
			if (args.length != 0)
				System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	protected static void info(Object msg) {
		System.out.println(msg);
	}

	protected static void err(Object msg) {
		System.err.println(msg);
	}

	protected static void debug(Object msg) {
		System.out.println(msg);
	}

	protected static void copyResource(String resource, Path targetFile) {
		InputStream input = null;
		OutputStream output = null;
		try {
			input = NodeRunner.class.getResourceAsStream(resource);
			Files.createDirectories(targetFile.getParent());
			output = Files.newOutputStream(targetFile);
			byte[] buf = new byte[8192];
			while (true) {
				int length = input.read(buf);
				if (length < 0)
					break;
				output.write(buf, 0, length);
			}
		} catch (Exception e) {
			throw new RuntimeException("Cannot write " + resource + " file to " + targetFile, e);
		} finally {
			try {
				input.close();
			} catch (Exception ignore) {
			}
			try {
				output.close();
			} catch (Exception ignore) {
			}
		}

	}

}
