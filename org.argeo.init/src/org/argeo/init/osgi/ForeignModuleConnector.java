package org.argeo.init.osgi;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.connect.ConnectContent;
import org.osgi.framework.connect.ConnectModule;
import org.osgi.framework.connect.ModuleConnector;

/**
 * A {@link ModuleConnector} based on another OSGi runtime.
 */
class ForeignModuleConnector implements ModuleConnector {
	private final BundleContext foreignBundleContext;
	private final List<String> foreignCategories;

	private BundleContext localBundleContext;

	public ForeignModuleConnector(BundleContext foreignBundleContext, List<String> foreignCategories) {
		this.foreignBundleContext = foreignBundleContext;
		this.foreignCategories = foreignCategories;
	}

	@Override
	public Optional<BundleActivator> newBundleActivator() {
		return Optional.of(new BundleActivator() {
			@Override
			public void start(BundleContext context) throws Exception {
				ForeignModuleConnector.this.localBundleContext = context;
			}

			@Override
			public void stop(BundleContext context) throws Exception {
				ForeignModuleConnector.this.localBundleContext = null;
			}

		});
	}

	@Override
	public void initialize(File storage, Map<String, String> configuration) {
	}

	@Override
	public Optional<ConnectModule> connect(String location) throws BundleException {
		// hacks
		if (location.contains("org.eclipse.rap.rwt.osgi"))
			return Optional.empty();

		String category = categoryFromLocation(location);
		if (category == null || !foreignCategories.contains(category))
			return Optional.empty();
		Bundle bundle = foreignBundleContext.getBundle(location);
		if (bundle != null && bundle.getBundleId() != 0) {
			// System.out.println("Foreign Bundle: " + bundle.getSymbolicName() + " " +
			// location);
			ConnectModule module = new ConnectModule() {

				@Override
				public ConnectContent getContent() throws IOException {
					return new ForeignBundleConnectContent(localBundleContext, bundle);
				}
			};
			return Optional.of(module);
		}
		return Optional.empty();
	}

	protected String categoryFromLocation(String location) {
		// deal with Windows (sigh)
		String regexp = File.separatorChar == '\\' ? "\\\\" : "/";
		String[] arr = location.split(regexp);
		if (arr.length < 2)
			return null;
		// TODO make it more robust
		String category = arr[arr.length - 2];
		return category;
	}
}
