package org.argeo.init.osgi;

import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.Vector;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * An OSGi configurator. See
 * <a href="http://wiki.eclipse.org/Configurator">http:
 * //wiki.eclipse.org/Configurator</a>
 */
public class Activator implements BundleActivator {
	private Long checkpoint = null;

	public void start(final BundleContext bundleContext) throws Exception {
		// admin thread
		Thread adminThread = new AdminThread(bundleContext);
		adminThread.start();

		// bootstrap
		OsgiBoot osgiBoot = new OsgiBoot(bundleContext);
		if (checkpoint == null) {
			osgiBoot.bootstrap();
			checkpoint = System.currentTimeMillis();
		} else {
			osgiBoot.update();
			checkpoint = System.currentTimeMillis();
		}
	}

	public void stop(BundleContext context) throws Exception {
	}

	class JournaldResourceBundle extends ResourceBundle {

		@Override
		protected Object handleGetObject(String key) {
			switch (key) {
			case "ERROR":
				return "<5>";
			}
			return null;
		}

		@Override
		public Enumeration<String> getKeys() {
			Vector<String> keys = new Vector<>();
			keys.add("ERROR");
			return keys.elements();
		}

	}
}
