package org.argeo.init.osgi;

import static java.lang.System.Logger.Level.INFO;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.argeo.api.init.InitConstants;
import org.argeo.api.init.RuntimeManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.connect.ConnectContent;
import org.osgi.framework.connect.ConnectFrameworkFactory;
import org.osgi.framework.connect.ConnectModule;
import org.osgi.framework.connect.ModuleConnector;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.BundleWiring;

public class SubFrameworkActivator implements BundleActivator {
	private final static Logger logger = System.getLogger(SubFrameworkActivator.class.getName());

//	private final static String EQUINOX_FRAMEWORK_CLASS = "org.eclipse.osgi.launch.Equinox";
	private final static String EQUINOX_FRAMEWORK_FACTORY_CLASS = "org.eclipse.osgi.launch.EquinoxFactory";

//	private ClassLoader bundleClassLoader;
//	private ClassLoader subFrameworkClassLoader;
	private BundleContext foreignBundleContext;

	private ConnectFrameworkFactory frameworkFactory;

	private Map<UUID, Framework> subFrameworks = Collections.synchronizedMap(new HashMap<>());

	private UUID foreignFrameworkUuid;

	@Override
	public void start(BundleContext context) throws Exception {
		this.foreignBundleContext = context;
		foreignFrameworkUuid = UUID.fromString(foreignBundleContext.getProperty(Constants.FRAMEWORK_UUID));

		try {
//			Bundle bundle = context.getBundle();
//			ClassLoader bundleClassLoader = bundle.adapt(BundleWiring.class).getClassLoader();
//			subFrameworkClassLoader = new URLClassLoader(new URL[0], bundleClassLoader);

			@SuppressWarnings("unchecked")
			Class<? extends ConnectFrameworkFactory> frameworkFactoryClass = (Class<? extends ConnectFrameworkFactory>) Framework.class
					.getClassLoader().loadClass(EQUINOX_FRAMEWORK_FACTORY_CLASS);
			frameworkFactory = frameworkFactoryClass.getConstructor().newInstance();

			boolean test = false;
			if (test)
				new Thread() {

					@Override
					public void run() {
						for (int i = 0; i < 5; i++) {
							Map<String, String> config = new HashMap<>();
							Path basePase = Paths.get(System.getProperty("user.home"), ".config/argeo/test/",
									"test" + i);
							config.put(InitConstants.PROP_OSGI_CONFIGURATION_AREA,
									basePase.resolve(RuntimeManager.STATE).toString());
							config.put(InitConstants.PROP_OSGI_INSTANCE_AREA,
									basePase.resolve(RuntimeManager.DATA).toString());
							config.put("argeo.host", "host" + i);
							config.put("osgi.console", "host" + i + ":2023");
							createFramework(config);
						}
					}

				}.start();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	Framework createFramework(Map<String, String> config) {
		try {
			URL bundleConfigUrl = foreignBundleContext.getBundle().getEntry("config.ini");
			try (InputStream in = bundleConfigUrl.openStream()) {
				RuntimeManager.loadConfig(in, config);
			}

			// Equinox
//			config.put("osgi.frameworkParentClassloader", "current");
//			config.put("osgi.parentClassLoader", "app");
//			config.put("osgi.contextClassLoaderParent", "app");

			ModuleConnector moduleConnector = new ParentBundleModuleConnector(foreignBundleContext);

//			URL frameworkUrl = URI.create(bundleContext.getProperty("osgi.framework")).toURL();
//			URLClassLoader frameworkClassLoader = new URLClassLoader(new URL[] { frameworkUrl, });
//			Class<? extends Framework> frameworkClass = (Class<? extends Framework>) frameworkClassLoader
//					.loadClass(EQUINOX_FRAMEWORK_CLASS);
//			Framework framework = frameworkClass.getConstructor(Map.class, ModuleConnector.class).newInstance(config,
//					moduleConnector);

			config.put(InitConstants.PROP_ARGEO_OSGI_PARENT_UUID, foreignFrameworkUuid.toString());
			Framework framework = frameworkFactory.newFramework(config, moduleConnector);

			framework.init((e) -> {
				UUID frameworkUuid = UUID
						.fromString(framework.getBundleContext().getProperty(Constants.FRAMEWORK_UUID));
				if (e.getType() == FrameworkEvent.STOPPED) {
					subFrameworks.remove(frameworkUuid);
					logger.log(INFO, "Removed subframework " + frameworkUuid + " in parent " + foreignFrameworkUuid);
				}
			});

			for (Bundle b : foreignBundleContext.getBundles()) {
				if (b.getBundleId() == 0)
					continue;
				String location = b.getLocation();
				if (location.contains("/org.argeo.tp/") //
						|| location.contains("/org.argeo.tp.sys/") //
						|| location.contains("/org.argeo.tp.httpd/") //
						|| location.contains("/org.argeo.tp.sshd/") //
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

			UUID frameworkUuid = UUID.fromString(framework.getBundleContext().getProperty(Constants.FRAMEWORK_UUID));
			subFrameworks.put(frameworkUuid, framework);
			logger.log(INFO, "Created subframework " + frameworkUuid + " in parent " + foreignFrameworkUuid);
			return framework;
		} catch (Exception e) {
			throw new IllegalStateException("Cannot start framework", e);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		for (Iterator<Framework> it = subFrameworks.values().iterator(); it.hasNext();) {
			Framework framework = it.next();
			framework.stop();
			it.remove();

		}
//		for (Framework framework : subFrameworks.values()) {
//			framework.stop();
//		}
		subFrameworks.clear();
		foreignBundleContext = null;
		frameworkFactory = null;
	}

	static class ParentBundleModuleConnector implements ModuleConnector {
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
	}

	static class ForeignBundleClassLoader extends ClassLoader {// implements BundleReference {
		private BundleContext localBundleContext;
		private Bundle foreignBundle;

		public ForeignBundleClassLoader(BundleContext localBundleContext, Bundle foreignBundle) {
			super("Foreign bundle " + foreignBundle.toString(), Optional
					.ofNullable(foreignBundle.adapt(BundleWiring.class)).map((bw) -> bw.getClassLoader()).orElse(null));
			this.localBundleContext = localBundleContext;
			this.foreignBundle = foreignBundle;
		}

//		@Override
		protected Bundle getBundle() {
			return localBundleContext.getBundle(foreignBundle.getLocation());
		}

//		@Override
//		public URL getResource(String resName) {
//			URL res = super.getResource(resName);
//			return res;
//		}
//
//		@Override
//		protected URL findResource(String resName) {
//			Bundle localBundle = getBundle();
//			if (localBundle != null) {
//				URL res = localBundle.getEntry(resName);
//				if (res != null)
//					return res;
//			}
//			return null;
//		}

	}

	static class ForeignBundleConnectContent implements ConnectContent {
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
			List<String> lst = Collections.list(foreignBundle.findEntries("", "*", true)).stream()
					.map((u) -> u.getPath()).toList();
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
}
