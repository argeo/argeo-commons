package org.argeo.cms.internal.kernel;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import org.argeo.cms.CmsException;
import org.osgi.framework.BundleContext;

class KernelUtils {
	final static String OSGI_INSTANCE_AREA = "osgi.instance.area";

	static Dictionary<String, ?> asDictionary(Properties props) {
		Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
		for (Object key : props.keySet()) {
			hashtable.put(key.toString(), props.get(key));
		}
		return hashtable;
	}

	static Dictionary<String, ?> asDictionary(ClassLoader cl,
			String resource) {
		Properties props = new Properties();
		try {
			props.load(cl.getResourceAsStream(resource));
		} catch (IOException e) {
			throw new CmsException("Cannot load " + resource
					+ " from classpath", e);
		}
		return asDictionary(props);
	}

	static File getOsgiInstanceDir(BundleContext bundleContext) {
		return new File(bundleContext.getProperty(OSGI_INSTANCE_AREA)
				.substring("file:".length())).getAbsoluteFile();
	}

	private KernelUtils() {

	}
}
