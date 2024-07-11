package org.argeo.init.osgi;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.argeo.api.init.InitConstants;
import org.argeo.api.init.RuntimeContext;
import org.argeo.api.init.RuntimeManager;
import org.argeo.internal.init.InternalState;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.connect.ConnectFrameworkFactory;
import org.osgi.framework.launch.Framework;

/**
 * Dynamically configures and launches multiple runtimes.
 */
class OsgiRuntimeManager implements RuntimeManager {
	private final static Logger logger = System.getLogger(OsgiRuntimeManager.class.getName());

	private final static long RUNTIME_SHUTDOWN_TIMEOUT = 60 * 1000;

//	private Path ownConfigArea;

	private Path baseConfigArea;
	private Path baseWritableArea;
//	private Map<String, String> configuration = new HashMap<>();

	private ConnectFrameworkFactory frameworkFactory;

	private final BundleContext bundleContext;

	private Map<String, OsgiRuntimeContext> runtimeContexts = new TreeMap<>();

	private boolean useForeignRuntime = Boolean
			.parseBoolean(System.getProperty(InitConstants.PROP_ARGEO_OSGI_EXPORT_ENABLED, "true"));

	OsgiRuntimeManager(BundleContext bundleContext) {
		Objects.requireNonNull(bundleContext);
		this.bundleContext = bundleContext;
		frameworkFactory = OsgiRuntimeContext.getFrameworkFactory(bundleContext);
		this.baseConfigArea = Paths
				.get(URI.create(bundleContext.getProperty(InitConstants.PROP_OSGI_SHARED_CONFIGURATION_AREA)));
//		this.baseConfigArea = ownConfigArea.getParent();
		this.baseWritableArea = Paths
				.get(URI.create(bundleContext.getProperty(InitConstants.PROP_OSGI_CONFIGURATION_AREA))).getParent();

//		logger.log(Level.TRACE, () -> "Runtime manager configuration: " + configuration);

//		System.out.println("java.library.path=" + System.getProperty("java.library.path"));
	}

	protected void shutdown() {
		// shutdown runtimes
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

		BundleContext foreignBundleContext = null;
		if (useForeignRuntime) {
			foreignBundleContext = bundleContext;

			Path parentRelPath = Paths.get(relPath).getParent();
			if (parentRelPath != null && Files.exists(baseConfigArea.resolve(parentRelPath))) {
				if (!runtimeContexts.containsKey(parentRelPath.toString())) {

					String exportCategories = bundleContext
							.getProperty(InitConstants.PROP_ARGEO_OSGI_EXPORT_CATEGORIES);
					List<String> foreignCategories = exportCategories == null ? new ArrayList<>()
							: Arrays.asList(exportCategories.trim().split(","));
					Path writableArea = baseWritableArea.resolve(parentRelPath);
					Path configArea = baseConfigArea.resolve(parentRelPath);
					Map<String, String> config = new HashMap<>();
					RuntimeManager.loadDefaults(config);
					config.put(InitConstants.PROP_OSGI_USE_SYSTEM_PROPERTIES, "false");
					config.put(InitConstants.PROP_OSGI_CONFIGURATION_AREA,
							writableArea.resolve(STATE).toUri().toString());
					config.put(InitConstants.PROP_OSGI_SHARED_CONFIGURATION_AREA, configArea.toUri().toString());
					config.put(InitConstants.PROP_OSGI_SHARED_CONFIGURATION_AREA_RO, "true");
					OsgiRuntimeContext runtimeContext = new OsgiRuntimeContext(frameworkFactory, config,
							foreignBundleContext, foreignCategories);
					runtimeContexts.put(parentRelPath.toString(), runtimeContext);
					runtimeContext.run();
					// FIXME properly stage installation
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// silent
					}
				}
				OsgiRuntimeContext parentRuntimeContext = runtimeContexts.get(parentRelPath.toString());
				foreignBundleContext = parentRuntimeContext.getFramework().getBundleContext();
			}
		}
		Path writableArea = baseWritableArea.resolve(relPath);
		Path configArea = baseConfigArea.resolve(relPath).getParent().resolve(SHARED);
		Map<String, String> config = new HashMap<>();
//		RuntimeManager.loadConfig(configArea, config);
		RuntimeManager.loadDefaults(config);
		config.put(InitConstants.PROP_OSGI_USE_SYSTEM_PROPERTIES, "false");
		config.put(InitConstants.PROP_OSGI_CONFIGURATION_AREA, writableArea.resolve(STATE).toUri().toString());
		config.put(InitConstants.PROP_OSGI_SHARED_CONFIGURATION_AREA, configArea.toUri().toString());
		config.put(InitConstants.PROP_OSGI_SHARED_CONFIGURATION_AREA_RO, "true");

		if (configCallback != null)
			configCallback.accept(config);

		// use config area if instance area is not set
		if (!config.containsKey(InitConstants.PROP_OSGI_INSTANCE_AREA))
			config.put(InitConstants.PROP_OSGI_INSTANCE_AREA, writableArea.resolve(DATA).toUri().toString());

		// create framework
		OsgiRuntimeContext runtimeContext;
		if (useForeignRuntime) {
			String exportCategories = bundleContext.getProperty(InitConstants.PROP_ARGEO_OSGI_EXPORT_CATEGORIES);
			List<String> foreignCategories = exportCategories == null ? new ArrayList<>()
					: Arrays.asList(exportCategories.trim().split(","));
			runtimeContext = new OsgiRuntimeContext(frameworkFactory, config, foreignBundleContext, foreignCategories);
		} else {
			runtimeContext = new OsgiRuntimeContext(frameworkFactory, config);
		}
		runtimeContexts.put(relPath, runtimeContext);
		return runtimeContext;
	}

	public void startRuntime(String relPath, Consumer<Map<String, String>> configCallback) {
		OsgiRuntimeContext runtimeContext = loadRuntime(relPath, configCallback);
		runtimeContext.run();
		Framework framework = runtimeContext.getFramework();
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
}
