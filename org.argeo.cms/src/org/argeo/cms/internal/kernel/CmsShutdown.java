package org.argeo.cms.internal.kernel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.launch.Framework;

/** Shutdowns the OSGi framework */
class CmsShutdown extends Thread {
	public final int EXIT_OK = 0;
	public final int EXIT_ERROR = 1;
	public final int EXIT_TIMEOUT = 2;
	public final int EXIT_UNKNOWN = 3;

	private final Log log = LogFactory.getLog(CmsShutdown.class);
	// private final BundleContext bc =
	// FrameworkUtil.getBundle(CmsShutdown.class).getBundleContext();
	private final Framework framework;

	/** Shutdown timeout in ms */
	private long timeout = 10 * 60 * 1000;

	public CmsShutdown() {
		super("CMS Shutdown Hook");
		framework = (Framework) FrameworkUtil.getBundle(CmsShutdown.class).getBundleContext().getBundle(0);
	}

	@Override
	public void run() {
		if (framework.getState() != Bundle.ACTIVE) {
			return;
		}
		
		if (log.isDebugEnabled())
			log.debug("Shutting down OSGi framework...");
		try {
			// shutdown framework
			framework.stop();
			// wait for shutdown
			FrameworkEvent shutdownEvent = framework.waitForStop(timeout);
			int stoppedType = shutdownEvent.getType();
			Runtime runtime = Runtime.getRuntime();
			if (stoppedType == FrameworkEvent.STOPPED) {
				// close VM
				//System.exit(EXIT_OK);
			} else if (stoppedType == FrameworkEvent.ERROR) {
				log.error("The OSGi framework stopped with an error");
				runtime.halt(EXIT_ERROR);
			} else if (stoppedType == FrameworkEvent.WAIT_TIMEDOUT) {
				log.error("The OSGi framework hasn't stopped after " + timeout + "ms."
						+ " Forcibly terminating the JVM...");
				runtime.halt(EXIT_TIMEOUT);
			} else {
				log.error("Unknown state of OSGi framework after " + timeout + "ms."
						+ " Forcibly terminating the JVM... (" + shutdownEvent + ")");
				runtime.halt(EXIT_UNKNOWN);
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Unexpected exception " + e + " in shutdown hook. " + " Forcibly terminating the JVM...");
			Runtime.getRuntime().halt(EXIT_UNKNOWN);
		}
	}

}
