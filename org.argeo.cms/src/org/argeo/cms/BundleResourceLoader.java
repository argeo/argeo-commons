package org.argeo.cms;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.rap.rwt.service.ResourceLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/** {@link ResourceLoader} implementation wrapping an {@link Bundle}. */
public class BundleResourceLoader implements ResourceLoader {
	private final BundleContext bundleContext;

	public BundleResourceLoader(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	@Override
	public InputStream getResourceAsStream(String resourceName)
			throws IOException {
		// TODO deal with other bundles
		Bundle bundle = bundleContext.getBundle();
		URL res = bundle.getResource(resourceName);
		if (res == null)
			throw new CmsException("Resource " + resourceName
					+ " not found in bundle " + bundle.getSymbolicName());
		return bundleContext.getBundle().getResource(resourceName).openStream();
	}

}
