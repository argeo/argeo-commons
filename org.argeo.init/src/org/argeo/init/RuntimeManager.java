package org.argeo.init;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.argeo.init.logging.ThinLoggerFinder;
import org.argeo.init.osgi.OsgiBoot;
import org.argeo.init.osgi.OsgiRuntimeContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class RuntimeManager {
	private final static Logger logger = System.getLogger(RuntimeManager.class.getName());

	private final static String ENV_STATE_DIRECTORY = "STATE_DIRECTORY";
//	private final static String ENV_CONFIGURATION_DIRECTORY = "CONFIGURATION_DIRECTORY";
//	private final static String ENV_CACHE_DIRECTORY = "CACHE_DIRECTORY";

	private final static String JVM_ARGS = "jvm.args";

	private Path baseConfigArea;
	private Path baseStateArea;
	private Map<String, String> configuration = new HashMap<>();

	private Map<String, OsgiRuntimeContext> runtimeContexts = new TreeMap<>();

	RuntimeManager(Path configArea, Path stateArea) {
		loadConfig(configArea, configuration);
		configuration.put(OsgiBoot.PROP_OSGI_CONFIGURATION_AREA, stateArea.toUri().toString());
		this.baseConfigArea = configArea.getParent();
		this.baseStateArea = stateArea.getParent();

		logger.log(Level.TRACE, () -> "Runtime manager configuration: " + configuration);

	}

	public void run() {
		logger.log(Level.DEBUG, "Start OSGi");
		try (OsgiRuntimeContext runtimeContext = new OsgiRuntimeContext(configuration)) {
			runtimeContext.run();

			BundleContext bc = runtimeContext.getFramework().getBundleContext();
			// uninstall init as a bundle since it will be available via OSGi system
			for (Bundle b : bc.getBundles()) {
				if (b.getSymbolicName().equals("org.argeo.init")) {
					b.uninstall();
				}
			}
			bc.registerService(RuntimeManager.class, this, new Hashtable<>(configuration));
			logger.log(Level.DEBUG, "Registered runtime manager");

			runtimeContext.waitForStop(0);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	public static void loadConfig(Path dir, Map<String, String> config) {
		try {
			System.out.println("Load from "+dir);
			Path jvmArgsPath = dir.resolve(JVM_ARGS);
			if (!Files.exists(jvmArgsPath)) {
				// load from parent directory
				loadConfig(dir.getParent(), config);
			}

			if (Files.exists(dir))
				for (Path p : Files.newDirectoryStream(dir, "*.ini")) {
					Properties props = new Properties();
					try (InputStream in = Files.newInputStream(p)) {
						props.load(in);
					}
					for (Object key : props.keySet()) {
						config.put(key.toString(), props.getProperty(key.toString()));
					}
				}
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot load configuration from " + dir, e);
		}
	}

	OsgiRuntimeContext loadRuntime(String relPath, Consumer<Map<String, String>> configCallback) {
		stopRuntime(relPath);
		Path stateArea = baseStateArea.resolve(relPath);
		Path configArea = baseConfigArea.resolve(relPath);
		Map<String, String> config = new HashMap<>();
		loadConfig(configArea, config);
		config.put(OsgiBoot.PROP_OSGI_CONFIGURATION_AREA, stateArea.toUri().toString());

		if (configCallback != null)
			configCallback.accept(config);
		OsgiRuntimeContext runtimeContext = new OsgiRuntimeContext(config);
		runtimeContexts.put(relPath, runtimeContext);
		return runtimeContext;
	}

	public void startRuntime(String relPath, Consumer<Map<String, String>> configCallback) {
		OsgiRuntimeContext runtimeContext = loadRuntime(relPath, configCallback);
		runtimeContext.run();
	}

	public void stopRuntime(String relPath) {
		if (!runtimeContexts.containsKey(relPath))
			return;
		RuntimeContext runtimeContext = runtimeContexts.get(relPath);
		try {
			runtimeContext.close();
			runtimeContext.waitForStop(60 * 1000);
		} catch (Exception e) {
			logger.log(Level.ERROR, "Cannot close runtime context " + relPath, e);
		} finally {
			runtimeContexts.remove(relPath);
		}

	}

	public static void main(String[] args) {
		ThinLoggerFinder.reloadConfiguration();
		Map<String, String> env = System.getenv();
//		for (String envName : new TreeSet<>(env.keySet())) {
//			System.out.format("%s=%s%n", envName, env.get(envName));
//		}
		if (args.length < 1)
			throw new IllegalArgumentException("A relative configuration directory must be specified");
		Path configArea = Paths.get(System.getProperty("user.dir"), args[0]);

		// System.out.println("## Start with PID " + ProcessHandle.current().pid());
		// System.out.println("user.dir=" + System.getProperty("user.dir"));

		Path stateArea = Paths.get(env.get(ENV_STATE_DIRECTORY));

		RuntimeManager runtimeManager = new RuntimeManager(configArea, stateArea);
		runtimeManager.run();
	}

}
