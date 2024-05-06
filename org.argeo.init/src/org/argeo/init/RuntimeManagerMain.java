package org.argeo.init;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.argeo.api.init.InitConstants;
import org.argeo.api.init.RuntimeManager;
import org.argeo.init.logging.ThinLoggerFinder;
import org.argeo.init.osgi.OsgiRuntimeContext;
import org.argeo.internal.init.InternalState;

/**
 * Dynamically configures and launches multiple runtimes, coordinated by a main
 * one.
 */
public class RuntimeManagerMain {
	private final static Logger logger = System.getLogger(RuntimeManagerMain.class.getName());

	private final static String ENV_STATE_DIRECTORY = "STATE_DIRECTORY";
//	private final static String ENV_CONFIGURATION_DIRECTORY = "CONFIGURATION_DIRECTORY";
//	private final static String ENV_CACHE_DIRECTORY = "CACHE_DIRECTORY";

	private final static long RUNTIME_SHUTDOWN_TIMEOUT = 60 * 1000;

	private Map<String, String> configuration = new HashMap<>();

	RuntimeManagerMain(Path configArea, Path stateArea) {
		RuntimeManager.loadConfig(configArea, configuration);

		// integration with OSGi runtime; this will be read by the init bundle
		configuration.put(ServiceMain.PROP_ARGEO_INIT_MAIN, "true");
		configuration.put(InitConstants.PROP_OSGI_SHARED_CONFIGURATION_AREA, configArea.toUri().toString());

		configuration.put(InitConstants.PROP_OSGI_CONFIGURATION_AREA,
				stateArea.resolve(RuntimeManager.STATE).toUri().toString());
		// use config area if instance area is not set
		if (!configuration.containsKey(InitConstants.PROP_OSGI_INSTANCE_AREA))
			configuration.put(InitConstants.PROP_OSGI_INSTANCE_AREA,
					stateArea.resolve(RuntimeManager.DATA).toUri().toString());

		logger.log(Level.TRACE, () -> "Runtime manager configuration: " + configuration);

//		System.out.println("java.library.path=" + System.getProperty("java.library.path"));
	}

	public void run() {
		OsgiRuntimeContext managerRuntimeContext = new OsgiRuntimeContext(OsgiRuntimeContext.loadFrameworkFactory(),
				configuration);
		try {
			managerRuntimeContext.run();
			InternalState.setMainRuntimeContext(managerRuntimeContext);

			// shutdown on exit
			Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(), "Runtime shutdown"));

//			BundleContext bc = managerRuntimeContext.getFramework().getBundleContext();
//			// uninstall init as a bundle since it will be available via OSGi system
//			OsgiBoot.uninstallBundles(bc, SYMBOLIC_NAME_INIT);
//			bc.registerService(RuntimeManager.class, this, new Hashtable<>(configuration));
			logger.log(Level.DEBUG, "Registered runtime manager");

			managerRuntimeContext.waitForStop(0);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	protected void shutdown() {
		// shutdown manager runtime
		try {
			InternalState.getMainRuntimeContext().close();
			InternalState.getMainRuntimeContext().waitForStop(RUNTIME_SHUTDOWN_TIMEOUT);
//			logger.log(Logger.Level.INFO, "Argeo Init stopped with PID " + ProcessHandle.current().pid());
			System.out.flush();
		} catch (Exception e) {
			e.printStackTrace();
			Runtime.getRuntime().halt(1);
		}
	}

	public static void main(String[] args) {
		ThinLoggerFinder.reloadConfiguration();
		logger.log(Logger.Level.DEBUG, () -> "Argeo Init starting with PID " + ProcessHandle.current().pid());
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

		RuntimeManagerMain runtimeManager = new RuntimeManagerMain(configArea, stateArea);
		runtimeManager.run();
	}

}
