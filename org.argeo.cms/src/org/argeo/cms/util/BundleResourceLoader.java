package org.argeo.cms.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.argeo.cms.CmsException;
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
		// String location =
		// bundle.getLocation().substring("initial@reference:".length());
		// if (location.startsWith("file:")) {
		// Path path = null;
		// try {
		// path = Paths.get(new URI(location));
		// } catch (URISyntaxException e) {
		// e.printStackTrace();
		// }
		// if (path != null) {
		// Path resourcePath = path.resolve(resourceName);
		// if (Files.exists(resourcePath))
		// return Files.newInputStream(resourcePath);
		// }
		// }
		URL res = bundle.getResource(resourceName);
		if (res == null)
			throw new CmsException("Resource " + resourceName
					+ " not found in bundle " + bundle.getSymbolicName());
		return res.openStream();
	}

}
