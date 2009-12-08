package org.argeo.server.json;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.codehaus.jackson.map.ObjectMapper;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.util.BundleDelegatingClassLoader;

public class JsonObjectFactoryImpl implements JsonObjectFactory,
		BundleContextAware, InitializingBean {
	private final static Log log = LogFactory
			.getLog(JsonObjectFactoryImpl.class);

	private BundleContext bundleContext;
	private ClassLoader classLoader = getClass().getClassLoader();

	private ObjectMapper objectMapper = new ObjectMapper();
	private Map<String, Class<?>> supportedTypes = new HashMap<String, Class<?>>();

	public Boolean supports(String type) {
		if (supportedTypes.containsKey(type))
			return true;

		return loadClass(type) != null ? true : false;
	}

	@SuppressWarnings("unchecked")
	public <T> T readValue(String type, String str) {
		final Class<?> clss;
		if (supportedTypes.containsKey(type))
			clss = supportedTypes.get(type);
		else {
			clss = loadClass(type);
			if (clss == null)
				throw new ArgeoException("Cannot find type " + type);
		}

		try {
			return (T) objectMapper.readValue(str, clss);
		} catch (Exception e) {
			throw new ArgeoException("Cannot deserialize " + str
					+ " (type=" + type + ")", e);
		}
	}

	public void setSupportedTypes(Map<String, Class<?>> supportedTypes) {
		this.supportedTypes = supportedTypes;
	}

	protected Class<?> loadClass(String type) {
		try {
			return classLoader.loadClass(type);
		} catch (ClassNotFoundException e) {
			if (log.isDebugEnabled())
				log.debug("BundleDelegatingClassLoader.loadClass failed: " + e);
		}

		return null;
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	public void afterPropertiesSet() throws Exception {
		classLoader = BundleDelegatingClassLoader
				.createBundleClassLoaderFor(bundleContext.getBundle());
	}
}
