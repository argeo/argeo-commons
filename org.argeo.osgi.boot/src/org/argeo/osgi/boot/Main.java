package org.argeo.osgi.boot;

import java.lang.management.ManagementFactory;

public class Main {

	public static void main(String[] args) {
		String mainClass = System.getProperty(OsgiBoot.PROP_ARGEO_OSGI_BOOT_APPCLASS);
		if (mainClass == null) {
			throw new IllegalArgumentException(
					"System property " + OsgiBoot.PROP_ARGEO_OSGI_BOOT_APPCLASS + " must be specified");
		}

		OsgiBuilder osgi = new OsgiBuilder();
		String distributionUrl = System.getProperty(OsgiBoot.PROP_ARGEO_OSGI_DISTRIBUTION_URL);
		if (distributionUrl != null)
			osgi.install(distributionUrl);
		// osgi.conf("argeo.node.useradmin.uris", "os:///");
		// osgi.conf("osgi.clean", "true");
		// osgi.conf("osgi.console", "true");
		osgi.launch();
		osgi.main(mainClass, args);

		long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
		String jvmUptimeStr = (jvmUptime / 1000) + "." + (jvmUptime % 1000) + "s";
		System.out.println("Command " + mainClass + " executed in " + jvmUptimeStr);

		osgi.shutdown();

	}

}
