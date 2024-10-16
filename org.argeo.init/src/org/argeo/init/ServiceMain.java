package org.argeo.init;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;

import org.argeo.api.init.InitConstants;
import org.argeo.init.logging.ThinLoggerFinder;
import org.argeo.init.osgi.OsgiRuntimeContext;
import org.argeo.internal.init.InternalState;

/** Configures and launches a single runtime, typically as a systemd service. */
public class ServiceMain {
	private final static Logger logger = System.getLogger(ServiceMain.class.getName());

	final static String FILE_SYSTEM_PROPERTIES = "system.properties";

	@Deprecated
	public final static String PROP_ARGEO_INIT_MAIN = "argeo.init.main";

//	private static RuntimeContext runtimeContext = null;

	private static List<Runnable> postStart = Collections.synchronizedList(new ArrayList<>());

	protected ServiceMain(String[] args) {
	}

	public static void main(String[] args) {
		final long pid = ProcessHandle.current().pid();
		logger.log(Logger.Level.DEBUG, () -> "Argeo Init starting with PID " + pid);

		// shutdown on exit
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				if (InternalState.getMainRuntimeContext() != null) {
					InternalState.getMainRuntimeContext().close();
					InternalState.getMainRuntimeContext().waitForStop(0);
				}
			} catch (Exception e) {
				e.printStackTrace();
				Runtime.getRuntime().halt(1);
			}
		}, "Runtime shutdown"));

		// TODO use args as well
		String dataArea = System.getProperty(InitConstants.PROP_OSGI_INSTANCE_AREA);
		String stateArea = System.getProperty(InitConstants.PROP_OSGI_CONFIGURATION_AREA);
		String configArea = System.getProperty(InitConstants.PROP_OSGI_SHARED_CONFIGURATION_AREA);

		if (configArea != null) {
			Path configAreaPath = Paths.get(configArea);
			Path additionalSystemPropertiesPath = configAreaPath.resolve(FILE_SYSTEM_PROPERTIES);
			if (Files.exists(additionalSystemPropertiesPath)) {
				Properties properties = new Properties();
				try (InputStream in = Files.newInputStream(additionalSystemPropertiesPath)) {
					properties.load(in);
				} catch (IOException e) {
					logger.log(Logger.Level.ERROR,
							"Cannot load additional system properties " + additionalSystemPropertiesPath, e);
				}

				for (Object key : properties.keySet()) {
					String currentValue = System.getProperty(key.toString());
					String value = properties.getProperty(key.toString());
					if (currentValue != null) {
						if (!Objects.equals(value, currentValue))
							logger.log(Logger.Level.WARNING, "System property " + key + " already set with value "
									+ currentValue + " instead of " + value + ". Ignoring new value.");
					} else {
						System.setProperty(key.toString(), value);
						logger.log(Logger.Level.TRACE, () -> "Added " + key + "=" + value
								+ " to system properties, from " + additionalSystemPropertiesPath.getFileName());
					}
				}
				ThinLoggerFinder.reloadConfiguration();
			}
		}

		Map<String, String> config = new HashMap<>();
		config.put(PROP_ARGEO_INIT_MAIN, "true");

		// add OSGi system properties to the configuration
		sysprops: for (Object key : new TreeMap<>(System.getProperties()).keySet()) {
			String keyStr = key.toString();
			switch (keyStr) {
			case InitConstants.PROP_OSGI_CONFIGURATION_AREA:
			case InitConstants.PROP_OSGI_SHARED_CONFIGURATION_AREA:
			case InitConstants.PROP_OSGI_INSTANCE_AREA:
				// we should already have dealt with those
				continue sysprops;
			default:
			}

			if (keyStr.startsWith("osgi.") || keyStr.startsWith("org.osgi.") || keyStr.startsWith("eclipse.")
					|| keyStr.startsWith("org.eclipse.equinox.") || keyStr.startsWith("felix.")) {
				String value = System.getProperty(keyStr);
				config.put(keyStr, value);
				logger.log(Logger.Level.TRACE,
						() -> "Added " + key + "=" + value + " to configuration, from system properties");
			}
		}

		try {
			try {
				if (stateArea != null)
					config.put(InitConstants.PROP_OSGI_CONFIGURATION_AREA, stateArea);
				if (configArea != null)
					config.put(InitConstants.PROP_OSGI_SHARED_CONFIGURATION_AREA, configArea);
				if (dataArea != null)
					config.put(InitConstants.PROP_OSGI_INSTANCE_AREA, dataArea);
				// config.put(OsgiBoot.PROP_OSGI_USE_SYSTEM_PROPERTIES, "true");

				OsgiRuntimeContext osgiRuntimeContext = new OsgiRuntimeContext(
						OsgiRuntimeContext.loadFrameworkFactory(), config);
				osgiRuntimeContext.run();
				InternalState.setMainRuntimeContext(osgiRuntimeContext);
				for (Runnable run : postStart) {
					try {
						run.run();
					} catch (Exception e) {
						logger.log(Level.ERROR, "Cannot run post start callback " + run, e);
					}
				}
				InternalState.getMainRuntimeContext().waitForStop(0);
			} catch (NoClassDefFoundError noClassDefFoundE) {
				StaticRuntimeContext staticRuntimeContext = new StaticRuntimeContext((Map<String, String>) config);
				staticRuntimeContext.run();
				InternalState.setMainRuntimeContext(staticRuntimeContext);
				for (Runnable run : postStart) {
					try {
						run.run();
					} catch (Exception e) {
						logger.log(Level.ERROR, "Cannot run post start callback " + run, e);
					}
				}
				InternalState.getMainRuntimeContext().waitForStop(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		logger.log(Logger.Level.DEBUG, "Argeo Init stopped with PID " + pid);
	}

	/** Add a post-start call back to be run after the runtime has been started. */
	public static void addPostStart(Runnable runnable) {
		postStart.add(runnable);
	}
}
