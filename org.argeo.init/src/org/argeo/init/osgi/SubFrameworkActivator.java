package org.argeo.init.osgi;

import static java.lang.System.Logger.Level.INFO;

import java.io.InputStream;
import java.lang.System.Logger;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.argeo.api.init.InitConstants;
import org.argeo.api.init.RuntimeManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.connect.ConnectFrameworkFactory;
import org.osgi.framework.connect.ModuleConnector;
import org.osgi.framework.launch.Framework;

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

			ModuleConnector moduleConnector = new ForeignModuleConnector(foreignBundleContext, null);

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

}
