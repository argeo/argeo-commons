package org.argeo.init;

import java.lang.System.Logger;
import java.util.HashMap;
import java.util.Map;

import org.argeo.init.osgi.OsgiRuntimeContext;

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
		log.log(Logger.Level.DEBUG, "Starting with PID " + pid);

		// shutdown on exit
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				if (Service.runtimeContext != null)
					Service.runtimeContext.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}, "Runtime shutdown"));

		Map<String, String> config = new HashMap<>();
		try {
			try (OsgiRuntimeContext osgiRuntimeContext = new OsgiRuntimeContext(config)) {
				osgiRuntimeContext.run();
				Service.runtimeContext = osgiRuntimeContext;
				Service.runtimeContext.waitForStop(0);
			} catch (NoClassDefFoundError e) {
				try (StaticRuntimeContext staticRuntimeContext = new StaticRuntimeContext(config)) {
					staticRuntimeContext.run();
					Service.runtimeContext = staticRuntimeContext;
					Service.runtimeContext.waitForStop(0);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
