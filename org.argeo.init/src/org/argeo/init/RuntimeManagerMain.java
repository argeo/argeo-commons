package org.argeo.init;

import static org.argeo.api.init.InitConstants.SYMBOLIC_NAME_INIT;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.argeo.api.init.InitConstants;
import org.argeo.api.init.RuntimeContext;
import org.argeo.api.init.RuntimeManager;
import org.argeo.init.logging.ThinLoggerFinder;
import org.argeo.init.osgi.OsgiRuntimeContext;
import org.argeo.internal.init.InternalState;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;

/**
 * Dynamically configures and launches multiple runtimes, coordinated by a main
 * one.
 */
public class RuntimeManagerMain implements RuntimeManager {
	private final static Logger logger = System.getLogger(RuntimeManagerMain.class.getName());

	private final static String ENV_STATE_DIRECTORY = "STATE_DIRECTORY";
//	private final static String ENV_CONFIGURATION_DIRECTORY = "CONFIGURATION_DIRECTORY";
//	private final static String ENV_CACHE_DIRECTORY = "CACHE_DIRECTORY";

	private final static long RUNTIME_SHUTDOWN_TIMEOUT = 60 * 1000;

	private Path baseConfigArea;
	private Path baseWritableArea;
	private Map<String, String> configuration = new HashMap<>();

	private Map<String, OsgiRuntimeContext> runtimeContexts = new TreeMap<>();

	RuntimeManagerMain(Path configArea, Path stateArea) {
		RuntimeManager.loadConfig(configArea, configuration);
		configuration.put(InitConstants.PROP_OSGI_CONFIGURATION_AREA, stateArea.resolve(STATE).toUri().toString());
		// use config area if instance area is not set
		if (!configuration.containsKey(InitConstants.PROP_OSGI_INSTANCE_AREA))
			configuration.put(InitConstants.PROP_OSGI_INSTANCE_AREA, stateArea.resolve(DATA).toUri().toString());
		this.baseConfigArea = configArea.getParent();
		this.baseWritableArea = stateArea.getParent();

		logger.log(Level.TRACE, () -> "Runtime manager configuration: " + configuration);

//		System.out.println("java.library.path=" + System.getProperty("java.library.path"));
	}

	public void run() {
//		try {
//			for (Path p : Files.newDirectoryStream(Paths.get("/usr/local/lib/a2"), "*.so")) {
//				try {
//					System.load(p.toString());
//				} catch (UnsatisfiedLinkError e) {
//					e.printStackTrace();
//				}
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

		OsgiRuntimeContext managerRuntimeContext = new OsgiRuntimeContext(configuration);
		try {
			managerRuntimeContext.run();
			InternalState.setMainRuntimeContext(managerRuntimeContext);

			// shutdown on exit
			Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(), "Runtime shutdown"));

			BundleContext bc = managerRuntimeContext.getFramework().getBundleContext();
			// uninstall init as a bundle since it will be available via OSGi system
			for (Bundle b : bc.getBundles()) {
				if (b.getSymbolicName().equals(SYMBOLIC_NAME_INIT)) {
					b.uninstall();
				}
			}
			bc.registerService(RuntimeManager.class, this, new Hashtable<>(configuration));
			logger.log(Level.DEBUG, "Registered runtime manager");

			managerRuntimeContext.waitForStop(0);
		} catch (InterruptedException | BundleException e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	protected void shutdown() {
		// shutdowm runtimes
		Map<String, RuntimeContext> shutdowning = new HashMap<>(runtimeContexts);
		for (String id : new HashSet<>(runtimeContexts.keySet())) {
			logger.log(Logger.Level.DEBUG, "Shutting down runtime " + id + " ...");
			closeRuntime(id, true);
		}
		for (String id : shutdowning.keySet())
			try {
				RuntimeContext runtimeContext = shutdowning.get(id);
				runtimeContext.waitForStop(RUNTIME_SHUTDOWN_TIMEOUT);
			} catch (InterruptedException e) {
				// silent
			} catch (Exception e) {
				logger.log(Logger.Level.DEBUG, "Cannot wait for " + id + " to shutdown", e);
			}
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

	OsgiRuntimeContext loadRuntime(String relPath, Consumer<Map<String, String>> configCallback) {
		closeRuntime(relPath, false);
		Path writableArea = baseWritableArea.resolve(relPath);
		Path configArea = baseConfigArea.resolve(relPath);
		Map<String, String> config = new HashMap<>();
		RuntimeManager.loadConfig(configArea, config);
		config.put(InitConstants.PROP_OSGI_CONFIGURATION_AREA, writableArea.resolve(STATE).toUri().toString());

		if (configCallback != null)
			configCallback.accept(config);

		// use config area if instance area is not set
		if (!config.containsKey(InitConstants.PROP_OSGI_INSTANCE_AREA))
			config.put(InitConstants.PROP_OSGI_INSTANCE_AREA, writableArea.resolve(DATA).toUri().toString());

		OsgiRuntimeContext runtimeContext = new OsgiRuntimeContext(config);
		runtimeContexts.put(relPath, runtimeContext);
		return runtimeContext;
	}

	public void startRuntime(String relPath, Consumer<Map<String, String>> configCallback) {
		OsgiRuntimeContext runtimeContext = loadRuntime(relPath, configCallback);
		runtimeContext.run();
		Framework framework = runtimeContext.getFramework();

//		for (Bundle b : framework.getBundleContext().getBundles()) {
//			try {
////				if (b.getSymbolicName().startsWith("org.eclipse.swt.gtk")) {
////					b.uninstall();
////				}
////				else if (b.getSymbolicName().startsWith("org.eclipse.jface")) {
////					b.uninstall();
////				}
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}

		if (framework != null) {// in case the framework has closed very quickly after run
			framework.getBundleContext().addFrameworkListener((e) -> {
				if (e.getType() >= FrameworkEvent.STOPPED) {
					logger.log(Level.DEBUG, "Externally stopped runtime " + relPath + ". Unregistering...", e);
					runtimeContexts.remove(relPath);
				}
			});
		} else {
			closeRuntime(relPath, false);
		}
	}

	public void closeRuntime(String relPath, boolean async) {
		if (!runtimeContexts.containsKey(relPath))
			return;
		RuntimeContext runtimeContext = runtimeContexts.get(relPath);
		try {
			runtimeContext.close();
			if (!async) {
				runtimeContext.waitForStop(RUNTIME_SHUTDOWN_TIMEOUT);
				System.gc();
			}
		} catch (Exception e) {
			logger.log(Level.ERROR, "Cannot close runtime context " + relPath, e);
		} finally {
			runtimeContexts.remove(relPath);
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
