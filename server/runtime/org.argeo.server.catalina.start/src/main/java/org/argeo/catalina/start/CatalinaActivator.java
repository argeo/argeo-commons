package org.argeo.catalina.start;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.web.tomcat.internal.Activator;

/** Starts Catalina (hacked from Spring OSGi 1.0) */
public class CatalinaActivator extends Activator {
	private final static Log log = LogFactory.getLog(CatalinaActivator.class);

	private final static String ARGEO_OSGI_DATA_DIR = "argeo.osgi.data.dir";
	/** System properties used to override Tomcat XML config URL */
	public final static String ARGEO_SERVER_TOMCAT_CONFIG = "argeo.server.tomcat.config";

	public void start(BundleContext context) throws Exception {
		if (!System.getProperties().containsKey(ARGEO_OSGI_DATA_DIR)) {
			String osgiInstanceArea = System.getProperty("osgi.instance.area");
			String osgiInstanceAreaDefault = System
					.getProperty("osgi.instance.area.default");
			String tempDir = System.getProperty("java.io.tmpdir");

			File dataDir = null;
			if (osgiInstanceArea != null) {
				// within OSGi with -data specified
				osgiInstanceArea = removeFilePrefix(osgiInstanceArea);
				dataDir = new File(osgiInstanceArea);
			} else if (osgiInstanceAreaDefault != null) {
				// within OSGi without -data specified
				osgiInstanceAreaDefault = removeFilePrefix(osgiInstanceAreaDefault);
				dataDir = new File(osgiInstanceAreaDefault);
			} else {// outside OSGi
				dataDir = new File(tempDir + File.separator + "osgiData");
			}

			System.setProperty(ARGEO_OSGI_DATA_DIR, dataDir.getAbsolutePath());
		}

		// Load config properties and put them in system properties so that they
		// can be used in tomcat conf
		Properties confProps = new Properties();
		URL propsUrl = context.getBundle().getResource("tomcat.properties");
		if (propsUrl != null) {
			InputStream in = null;
			try {
				in = propsUrl.openStream();
				confProps.load(in);
			} catch (Exception e) {
				throw new RuntimeException("Cannot read catalina properties.",
						e);
			} finally {
				IOUtils.closeQuietly(in);
			}

			for (Object key : confProps.keySet()) {
				// System properties have priority
				if (!System.getProperties().containsKey(key)) {
					System.setProperty(key.toString(),
							confProps.getProperty(key.toString()));
				}
			}
		}

		// calling Catalina.setCatalinaHome(String) or
		// Catalina.setCatalinaBase(String) does the same
		if (System.getProperty("catalina.home") == null)
			System.setProperty("catalina.home",
					System.getProperty(ARGEO_OSGI_DATA_DIR) + "/tomcat");
		if (System.getProperty("catalina.base") == null)
			System.setProperty("catalina.base",
					System.getProperty(ARGEO_OSGI_DATA_DIR) + "/tomcat");

		// Make sure directories are created
		File catalinaDir = new File(System.getProperty("catalina.home"));
		if (!catalinaDir.exists()) {
			catalinaDir.mkdirs();
			if (log.isDebugEnabled())
				log.debug("Created Tomcat directory " + catalinaDir);
		}

		// Call Spring starter
		super.start(context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
	}

	protected String removeFilePrefix(String url) {
		if (url.startsWith("file:"))
			return url.substring("file:".length());
		else if (url.startsWith("reference:file:"))
			return url.substring("reference:file:".length());
		else
			return url;
	}

}
