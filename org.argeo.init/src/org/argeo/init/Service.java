package org.argeo.init;

import java.lang.System.Logger;
import java.util.HashMap;
import java.util.Map;

import org.argeo.init.osgi.OsgiRuntimeContext;

/** Configure and launch an Argeo service. */
public class Service implements Runnable, AutoCloseable {
	private final static Logger log = System.getLogger(Service.class.getName());

	private static RuntimeContext runtimeContext = null;

	protected Service(String[] args) {
	}

	@Override
	public void run() {
	}

	@Override
	public void close() throws Exception {
	}

	public static void main(String[] args) {
		long pid = ProcessHandle.current().pid();
		log.log(Logger.Level.DEBUG, "Argeo Init starting with PID " + pid);

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

		Map<String, String> config = new HashMap<>();
		config.put("osgi.framework.useSystemProperties", "true");
//		for (Object key : System.getProperties().keySet()) {
//			config.put(key.toString(), System.getProperty(key.toString()));
//			log.log(Logger.Level.DEBUG, key + "=" + System.getProperty(key.toString()));
//		}
		try {
			try {
				OsgiRuntimeContext osgiRuntimeContext = new OsgiRuntimeContext((Map<String, String>) config);
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
		log.log(Logger.Level.DEBUG, "Argeo Init stopped with PID " + pid);
	}

	
	public static RuntimeContext getRuntimeContext() {
		return runtimeContext;
	}
}
