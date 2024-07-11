package org.argeo.init.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.connect.ConnectContent;

/**
 * A {@link ConnectContent} based on a {@link Bundle} from another OSGi runtime.
 */
class ForeignBundleConnectContent implements ConnectContent {
	private final Bundle foreignBundle;
	private final ClassLoader classLoader;

	public ForeignBundleConnectContent(BundleContext localBundleContext, Bundle foreignBundle) {
		this.foreignBundle = foreignBundle;
		this.classLoader = new ForeignBundleClassLoader(localBundleContext, foreignBundle);
	}

	@Override
	public Optional<Map<String, String>> getHeaders() {
		Dictionary<String, String> dict = foreignBundle.getHeaders();
		List<String> keys = Collections.list(dict.keys());
		Map<String, String> dictCopy = keys.stream().collect(Collectors.toMap(Function.identity(), dict::get));
		return Optional.of(dictCopy);
	}

	@Override
	public Iterable<String> getEntries() throws IOException {
		List<String> lst = Collections.list(foreignBundle.findEntries("", "*", true)).stream().map((u) -> u.getPath())
				.toList();
		return lst;
	}

	@Override
	public Optional<ConnectEntry> getEntry(String path) {
		URL u = foreignBundle.getEntry(path);
		if (u == null) {
			u = foreignBundle.getEntry("bin/" + path);
			// System.err.println(u2);
		}
		if (u == null) {
			if ("plugin.xml".equals(path))
				return Optional.empty();
			if (path.startsWith("META-INF/versions/"))
				return Optional.empty();
			System.err.println(foreignBundle.getSymbolicName() + " " + path + " not found");
			return Optional.empty();
		}
		URL url = u;
		ConnectEntry urlConnectEntry = new ConnectEntry() {

			@Override
			public String getName() {
				return path;
			}

			@Override
			public long getLastModified() {
				return foreignBundle.getLastModified();
			}

			@Override
			public InputStream getInputStream() throws IOException {
				return url.openStream();
			}

			@Override
			public long getContentLength() {
				return -1;
			}
		};
		return Optional.of(urlConnectEntry);
	}

	@Override
	public Optional<ClassLoader> getClassLoader() {
		ClassLoader cl;
		// cl = bundle.adapt(BundleWiring.class).getClassLoader();

		// cl = subFrameworkClassLoader;
		cl = classLoader;
		return Optional.of(cl);
//			return Optional.empty();
	}

	@Override
	public void open() throws IOException {
	}

	@Override
	public void close() throws IOException {

	}

}
