package org.argeo.init.osgi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.argeo.api.init.InitConstants;
import org.argeo.api.init.RuntimeManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.connect.ConnectContent;
import org.osgi.framework.connect.ConnectFrameworkFactory;
import org.osgi.framework.connect.ConnectModule;
import org.osgi.framework.connect.ModuleConnector;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.BundleWiring;

public class SubFrameworkActivator implements BundleActivator {
//	private final static String EQUINOX_FRAMEWORK_CLASS = "org.eclipse.osgi.launch.Equinox";
	private final static String EQUINOX_FRAMEWORK_FACTORY_CLASS = "org.eclipse.osgi.launch.EquinoxFactory";

//	private ClassLoader bundleClassLoader;
//	private ClassLoader subFrameworkClassLoader;
	private BundleContext bundleContext;

	private ConnectFrameworkFactory frameworkFactory;

	@Override
	public void start(BundleContext context) throws Exception {
		this.bundleContext = context;

		try {
			Bundle bundle = context.getBundle();
			ClassLoader bundleClassLoader = bundle.adapt(BundleWiring.class).getClassLoader();
//			subFrameworkClassLoader = new URLClassLoader(new URL[0], bundleClassLoader);

			@SuppressWarnings("unchecked")
			Class<? extends ConnectFrameworkFactory> frameworkFactoryClass = (Class<? extends ConnectFrameworkFactory>) bundleClassLoader
					.loadClass(EQUINOX_FRAMEWORK_FACTORY_CLASS);
			frameworkFactory = frameworkFactoryClass.getConstructor().newInstance();

			new Thread() {

				@Override
				public void run() {
					for (int i = 0; i < 5; i++) {
						Map<String, String> config = new HashMap<>();
						Path basePase = Paths.get(System.getProperty("user.home"), ".config/argeo/test/", "test" + i);
						config.put(InitConstants.PROP_OSGI_CONFIGURATION_AREA,
								basePase.resolve(RuntimeManager.STATE).toString());
						config.put(InitConstants.PROP_OSGI_INSTANCE_AREA,
								basePase.resolve(RuntimeManager.DATA).toString());
						config.put("argeo.host", "host" + i);
						config.put("osgi.console", "host" + i + ":2023");
						startFramework(config);
					}
				}

			}.start();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	Framework startFramework(Map<String, String> config) {
		try {
			URL bundleConfigUrl = bundleContext.getBundle().getEntry("config.ini");
			try (InputStream in = bundleConfigUrl.openStream()) {
				RuntimeManager.loadConfig(in, config);
			}

			// Equinox
//			config.put("osgi.frameworkParentClassloader", "current");
//			config.put("osgi.parentClassLoader", "app");
//			config.put("osgi.contextClassLoaderParent", "app");

			ModuleConnector moduleConnector = new ParentBundleModuleConnector(bundleContext);

//			URL frameworkUrl = URI.create(bundleContext.getProperty("osgi.framework")).toURL();
//			URLClassLoader frameworkClassLoader = new URLClassLoader(new URL[] { frameworkUrl, });
//			Class<? extends Framework> frameworkClass = (Class<? extends Framework>) frameworkClassLoader
//					.loadClass(EQUINOX_FRAMEWORK_CLASS);
//			Framework framework = frameworkClass.getConstructor(Map.class, ModuleConnector.class).newInstance(config,
//					moduleConnector);

			Framework framework = frameworkFactory.newFramework(config, moduleConnector);

			framework.init();

			for (Bundle b : bundleContext.getBundles()) {
				if (b.getBundleId() == 0)
					continue;
				String location = b.getLocation();
				if (location.contains("/org.argeo.tp/") //
						|| location.contains("/org.argeo.tp.sys/") //
						|| location.contains("/org.argeo.tp.httpd/") //
						|| location.contains("/org.argeo.tp.sshd/") //
						|| location.contains("/org.argeo.cms/org.argeo.init") //
				) {
					framework.getBundleContext().installBundle(b.getLocation());
				}
			}

			OsgiBoot osgiBoot = new OsgiBoot(framework.getBundleContext());
			osgiBoot.install();
//			OsgiBoot.uninstallBundles(osgiBoot.getBundleContext(), "org.argeo.api.cms");
//			OsgiBoot.uninstallBundles(osgiBoot.getBundleContext(), "org.osgi.service.useradmin");
//			osgiBoot.getBundleContext()
//					.installBundle("initial@reference:file:../../../../../argeo-commons/org.argeo.api.cms/");
//			osgiBoot.getBundleContext().installBundle(
//					"reference:file:/usr/local/share/a2/osgi/equinox/org.argeo.tp.osgi/org.osgi.service.useradmin.1.1.jar");
			osgiBoot.refresh();
			framework.start();
			osgiBoot.startBundles();

//			for (Bundle b : framework.getBundleContext().getBundles()) {
//				BundleContext bc = b.getBundleContext();
//				if (bc == null)
//					System.err.println(b.getSymbolicName() + " BC null");
//			}
			return framework;
		} catch (Exception e) {
			throw new IllegalStateException("Cannot start framework", e);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		bundleContext = null;
		frameworkFactory = null;
	}

	class ParentBundleModuleConnector implements ModuleConnector {
		private final BundleContext foreignBundleContext;
		private BundleContext localBundleContext;

		public ParentBundleModuleConnector(BundleContext foreignBundleContext) {
			this.foreignBundleContext = foreignBundleContext;
		}

		@Override
		public Optional<BundleActivator> newBundleActivator() {
			return Optional.of(new BundleActivator() {
				@Override
				public void start(BundleContext context) throws Exception {
					ParentBundleModuleConnector.this.localBundleContext = context;
				}

				@Override
				public void stop(BundleContext context) throws Exception {
					ParentBundleModuleConnector.this.localBundleContext = null;
				}

			});
		}

		@Override
		public void initialize(File storage, Map<String, String> configuration) {
		}

		@Override
		public Optional<ConnectModule> connect(String location) throws BundleException {
			Bundle bundle = foreignBundleContext.getBundle(location);
			if (bundle != null && bundle.getBundleId() != 0) {
				System.out.println("Foreign Bundle: " + bundle.getSymbolicName() + " " + location);
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
	}

	class ForeignBundleClassLoader extends ClassLoader implements BundleReference {
		private BundleContext localBundleContext;
		private Bundle foreignBundle;

		public ForeignBundleClassLoader(BundleContext localBundleContext, Bundle foreignBundle) {
			super("Foreign bundle " + foreignBundle.toString(), Optional
					.ofNullable(foreignBundle.adapt(BundleWiring.class)).map((bw) -> bw.getClassLoader()).orElse(null));
			this.localBundleContext = localBundleContext;
			this.foreignBundle = foreignBundle;
		}

		@Override
		public Bundle getBundle() {
			return localBundleContext.getBundle(foreignBundle.getLocation());
		}
	}

	class ForeignBundleConnectContent implements ConnectContent {
		private final Bundle bundle;
		private final ClassLoader classLoader;

		public ForeignBundleConnectContent(BundleContext localBundleContext, Bundle bundle) {
			this.bundle = bundle;
			this.classLoader = new ForeignBundleClassLoader(localBundleContext, bundle);
		}

		@Override
		public Optional<Map<String, String>> getHeaders() {
			Dictionary<String, String> dict = bundle.getHeaders();
			List<String> keys = Collections.list(dict.keys());
			Map<String, String> dictCopy = keys.stream().collect(Collectors.toMap(Function.identity(), dict::get));
			return Optional.of(dictCopy);
		}

		@Override
		public Iterable<String> getEntries() throws IOException {
			List<String> lst = Collections.list(bundle.findEntries("", "*", true)).stream().map((u) -> u.getPath())
					.toList();
			return lst;
		}

		@Override
		public Optional<ConnectEntry> getEntry(String path) {
			URL u = bundle.getEntry(path);
			if (u == null) {
				u = bundle.getEntry("bin/" + path);
				// System.err.println(u2);
			}
			if (u == null) {
				if ("plugin.xml".equals(path))
					return Optional.empty();
				System.err.println(bundle.getSymbolicName() + " " + path + " not found");
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
					// FIXME
					return System.currentTimeMillis();
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
}
