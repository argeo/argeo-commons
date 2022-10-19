package org.argeo.init;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.argeo.init.logging.ThinLoggerFinder;
import org.argeo.init.osgi.OsgiBoot;
import org.argeo.init.osgi.OsgiRuntimeContext;

/** Configure and launch an Argeo service. */
public class Service {
	private final static Logger logger = System.getLogger(Service.class.getName());

	public final static String PROP_ARGEO_INIT_MAIN = "argeo.init.main";

	private static RuntimeContext runtimeContext = null;

	protected Service(String[] args) {
	}

	public static void main(String[] args) {
		final long pid = ProcessHandle.current().pid();
		logger.log(Logger.Level.DEBUG, () -> "Argeo Init starting with PID " + pid);

		// shutdown on exit
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				if (Service.runtimeContext != null) {
//					System.out.println("Argeo Init stopping with PID " + pid);
					Service.runtimeContext.close();
					Service.runtimeContext.waitForStop(0);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}, "Runtime shutdown"));

		// TODO use args as well
		String dataArea = System.getProperty(OsgiBoot.PROP_OSGI_INSTANCE_AREA);
		String stateArea = System.getProperty(OsgiBoot.PROP_OSGI_CONFIGURATION_AREA);
		String configArea = System.getProperty(OsgiBoot.PROP_OSGI_SHARED_CONFIGURATION_AREA);

		if (configArea != null) {
			Path configAreaPath = Paths.get(configArea);
			Path additionalSystemPropertiesPath = configAreaPath.resolve("system.properties");
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
					}
				}
				ThinLoggerFinder.reloadConfiguration();
			}
		}

		Map<String, String> config = new HashMap<>();
		config.put(PROP_ARGEO_INIT_MAIN, "true");

		try {
			try {
				if (stateArea != null)
					config.put(OsgiBoot.PROP_OSGI_CONFIGURATION_AREA, stateArea);
				if (configArea != null)
					config.put(OsgiBoot.PROP_OSGI_SHARED_CONFIGURATION_AREA, configArea);
				if (dataArea != null)
					config.put(OsgiBoot.PROP_OSGI_INSTANCE_AREA, dataArea);
				// config.put(OsgiBoot.PROP_OSGI_USE_SYSTEM_PROPERTIES, "true");

				OsgiRuntimeContext osgiRuntimeContext = new OsgiRuntimeContext(config);
				osgiRuntimeContext.run();
				Service.runtimeContext = osgiRuntimeContext;
				Service.runtimeContext.waitForStop(0);
			} catch (NoClassDefFoundError e) {
				StaticRuntimeContext staticRuntimeContext = new StaticRuntimeContext((Map<String, String>) config);
				staticRuntimeContext.run();
				Service.runtimeContext = staticRuntimeContext;
				Service.runtimeContext.waitForStop(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		logger.log(Logger.Level.DEBUG, "Argeo Init stopped with PID " + pid);
	}

	public static RuntimeContext getRuntimeContext() {
		return runtimeContext;
	}
}
