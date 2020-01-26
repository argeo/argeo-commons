package org.argeo.osgi.boot.internal.springutil;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bootstrap {

	public static void main(String[] args) {
		try {
			String configurationArea = "file:" + System.getProperty("user.dir") + "/state";
			String instanceArea = "file:" + System.getProperty("user.dir") + "/data";
			String log4jUrl = "file:" + System.getProperty("user.dir") + "/log4j.properties";

			System.setProperty("org.osgi.service.http.port", "7070");
			System.setProperty("log4j.configuration", log4jUrl);

			System.setProperty("osgi.console", "2323");
			Map<String, String> props = new HashMap<String, String>();
			props.put("osgi.clean", "true");
//			props.put("osgi.console", "2323");
			props.put("osgi.configuration.area", configurationArea);
			props.put("osgi.instance.area", instanceArea);

			System.setProperty("argeo.osgi.start.2.node",
					"org.eclipse.equinox.console,org.eclipse.equinox.http.servlet,org.eclipse.equinox.ds,"
							+ "org.eclipse.equinox.metatype,org.eclipse.equinox.cm,org.eclipse.rap.rwt.osgi");
			System.setProperty("argeo.osgi.start.3.node", "org.argeo.cms");

			// URL osgiJar =
			// Bootstrap.class.getClassLoader().getResource("/usr/share/osgi/boot/org.eclipse.org.jar");
			URL osgiJar = new URL(
					"file:///home/mbaudier/dev/git/apache2/argeo-commons/demo/exec/cms-e4-rap/backup/share/osgi/boot/org.eclipse.org.jar");
			URL osgiBootJar = new URL(
					"file:///home/mbaudier/dev/git/apache2/argeo-commons/demo/exec/cms-e4-rap/backup/share/osgi/boot/org.argeo.osgi.boot.jar");
			URL[] jarUrls = { osgiJar };
			try (URLClassLoader urlCl = new URLClassLoader(jarUrls)) {

				// Class<?> factoryClass =
				// urlCl.loadClass("/org/eclipse/osgi/launch/EquinoxFactory");
				Class<?> factoryClass = urlCl.loadClass("org.eclipse.osgi.launch.EquinoxFactory");
				Class<?> frameworkClass = urlCl.loadClass("org.osgi.framework.launch.Framework");
				Class<?> bundleContextClass = urlCl.loadClass("org.osgi.framework.BundleContext");
				Class<?> bundleClass = urlCl.loadClass("org.osgi.framework.Bundle");

				Object factory = factoryClass.getConstructor().newInstance();
				Method newFrameworkMethod = factoryClass.getMethod("newFramework", Map.class);
				Object framework = newFrameworkMethod.invoke(factory, props);
				Method startFramework = frameworkClass.getMethod("start", new Class[] {});
				startFramework.invoke(framework);
				Method getBundleContext = frameworkClass.getMethod("getBundleContext", new Class[] {});
				Object bundleContext = getBundleContext.invoke(framework);
				Class<?>[] installArgs = { String.class, InputStream.class };
				Method install = bundleContextClass.getMethod("installBundle", installArgs);
				Method startBundle = bundleClass.getMethod("start");
				Method getSymbolicName = bundleClass.getMethod("getSymbolicName");

				Path basePath = Paths.get(
						"/home/mbaudier/dev/git/apache2/argeo-commons/demo/exec/cms-e4-rap/backup/share/osgi/boot/");
				List<Object> bundles = new ArrayList<>();
				for (Path p : Files.newDirectoryStream(basePath)) {
					try (InputStream in = Files.newInputStream(p)) {
						Object bundle = install.invoke(bundleContext, "file:" + p, in);
						bundles.add(bundle);
						System.out.println("Installed " + bundle);
					} catch (Exception e) {
						if (!p.getFileName().toString().startsWith("org.eclipse.osgi")) {
							System.err.println(p);
							e.printStackTrace();
						}
					}
				}

//				for (Object bundle : bundles) {
//					try {
//						String symbolicName = getSymbolicName.invoke(bundle).toString();
//						startBundle.invoke(bundle);
//					} catch (Exception e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}

				Object osgiBootBundle = install.invoke(bundleContext, osgiBootJar.toString(), osgiBootJar.openStream());
				startBundle.invoke(osgiBootBundle);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
