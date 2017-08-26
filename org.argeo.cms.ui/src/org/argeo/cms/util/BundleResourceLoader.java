package org.argeo.cms.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.argeo.cms.CmsException;
import org.eclipse.rap.rwt.service.ResourceLoader;
import org.osgi.framework.Bundle;

/** {@link ResourceLoader} implementation wrapping an {@link Bundle}. */
class BundleResourceLoader implements ResourceLoader {
	private final Bundle bundle;

	public BundleResourceLoader(Bundle bundle) {
		this.bundle = bundle;
	}

	@Override
	public InputStream getResourceAsStream(String resourceName) throws IOException {
		URL res = bundle.getResource(resourceName);
		if (res == null)
			throw new CmsException("Resource " + resourceName + " not found in bundle " + bundle.getSymbolicName());
		return res.openStream();
	}

}
