package org.argeo.init.osgi;

import java.io.File;

import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;

/** Monitors the runtime and can shut it down. */
@Deprecated
public class AdminThread extends Thread {
	public final static String PROP_ARGEO_OSGI_SHUTDOWN_FILE = "argeo.osgi.shutdownFile";
	private File shutdownFile;
	private final BundleContext bundleContext;

	public AdminThread(BundleContext bundleContext) {
		super("OSGi Boot Admin");
		this.bundleContext = bundleContext;
		if (System.getProperty(PROP_ARGEO_OSGI_SHUTDOWN_FILE) != null) {
			shutdownFile = new File(
					System.getProperty(PROP_ARGEO_OSGI_SHUTDOWN_FILE));
			if (!shutdownFile.exists()) {
				shutdownFile = null;
				OsgiBootUtils.warn("Shutdown file " + shutdownFile
						+ " not found, feature deactivated");
			}
		}
	}

	public void run() {
		if (shutdownFile != null) {
			// wait for file to be removed
			while (shutdownFile.exists()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			Framework framework = (Framework) bundleContext.getBundle(0);
			try {
				// shutdown framework
				framework.stop();
				// wait 10 mins for shutdown
				framework.waitForStop(10 * 60 * 1000);
				// close VM
				System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}
