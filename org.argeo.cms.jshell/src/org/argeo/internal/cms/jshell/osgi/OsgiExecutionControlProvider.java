package org.argeo.internal.cms.jshell.osgi;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;

import org.argeo.api.cms.CmsLog;
import org.argeo.cms.jshell.CmsExecutionControl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;

public class OsgiExecutionControlProvider implements ExecutionControlProvider {
	private final static CmsLog log = CmsLog.getLog(OsgiExecutionControlProvider.class);

	public final static String PROVIDER_NAME = "osgi";
	public final static String BUNDLE_PARAMETER = "bundle";

	@Override
	public String name() {
		return PROVIDER_NAME;
	}

	@Override
	public Map<String, String> defaultParameters() {
		Map<String, String> defaultParameters = new HashMap<>();
		defaultParameters.put(BUNDLE_PARAMETER, null);
		return defaultParameters;
	}

	@Override
	public ExecutionControl generate(ExecutionEnv env, Map<String, String> parameters) throws Throwable {
		Long bundleId = Long.parseLong(parameters.get(BUNDLE_PARAMETER));
		Bundle fromBundle = getBundleFromId(bundleId);

		BundleWiring fromBundleWiring = fromBundle.adapt(BundleWiring.class);
		ClassLoader fromBundleClassLoader = fromBundleWiring.getClassLoader();

		ExecutionControl executionControl = new CmsExecutionControl(env,
				new WrappingLoaderDelegate(env, fromBundleClassLoader));
		log.trace(() -> "JShell from " + fromBundle.getSymbolicName() + "_" + fromBundle.getVersion() + " ["
				+ fromBundle.getBundleId() + "]");
		return executionControl;
	}

	public static Bundle getBundleFromSn(String symbolicName) {
		BundleContext bc = FrameworkUtil.getBundle(OsgiExecutionControlProvider.class).getBundleContext();
		Objects.requireNonNull(symbolicName);
		NavigableMap<Version, Bundle> bundles = new TreeMap<Version, Bundle>();
		for (Bundle b : bc.getBundles()) {
			if (symbolicName.equals(b.getSymbolicName()))
				bundles.put(b.getVersion(), b);
		}
		if (bundles.isEmpty())
			return null;
		Bundle fromBundle = bundles.lastEntry().getValue();
		return fromBundle;
	}

	public static Bundle getBundleFromId(Long bundleId) {
		BundleContext bc = FrameworkUtil.getBundle(OsgiExecutionControlProvider.class).getBundleContext();
		Bundle fromBundle = bc.getBundle(bundleId);
		return fromBundle;
	}

	public static Path getBundleStartupScript(Long bundleId) {
		BundleContext bc = FrameworkUtil.getBundle(OsgiExecutionControlProvider.class).getBundleContext();
		Bundle fromBundle = bc.getBundle(bundleId);

		int bundleState = fromBundle.getState();
		if (Bundle.INSTALLED == bundleState)
			throw new IllegalStateException("Bundle " + fromBundle.getSymbolicName() + " is not resolved");
		if (Bundle.RESOLVED == bundleState) {
			try {
				fromBundle.start();
			} catch (BundleException e) {
				throw new IllegalStateException("Cannot start bundle " + fromBundle.getSymbolicName(), e);
			}
			while (Bundle.ACTIVE != fromBundle.getState())
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// we assume the session has been closed
					throw new RuntimeException("Bundle " + fromBundle.getSymbolicName() + " is not active", e);
				}
		}

		Path bundleStartupScript = fromBundle.getDataFile("BUNDLE.jsh").toPath();

		BundleWiring fromBundleWiring = fromBundle.adapt(BundleWiring.class);
		ClassLoader fromBundleClassLoader = fromBundleWiring.getClassLoader();

		Set<String> packagesToImport = new TreeSet<>();

		// from bundle packages
		for (Package pkg : fromBundleClassLoader.getDefinedPackages()) {
			packagesToImport.add(pkg.getName());
		}

//		List<BundleWire> exportedWires = fromBundleWiring.getProvidedWires(BundleRevision.PACKAGE_NAMESPACE);
//		for (BundleWire bw : exportedWires) {
//			packagesToImport.add(bw.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE).toString());
//		}

		List<BundleWire> importedWires = fromBundleWiring.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);
		for (BundleWire bw : importedWires) {
			packagesToImport.add(bw.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE).toString());
		}

		try (Writer writer = Files.newBufferedWriter(bundleStartupScript, StandardCharsets.UTF_8)) {
			for (String p : packagesToImport) {
				writer.write("import " + p + ".*;\n");
			}

			String std = """
					/open DEFAULT
					import jdk.jshell.spi.ExecutionEnv;
					import java.util.function.*;

					/** Redirected standard IO. */
					public class Std {
						final static InputStream in = new Supplier<InputStream>() {

							@Override
							public InputStream get() {
								return ((ExecutionEnv) getClass().getClassLoader()).userIn();
							}

						}.get();
						final static PrintStream out = new Supplier<PrintStream>() {

							@Override
							public PrintStream get() {
								return ((ExecutionEnv) getClass().getClassLoader()).userOut();
							}

						}.get();
						final static PrintStream err = new Supplier<PrintStream>() {

							@Override
							public PrintStream get() {
								return ((ExecutionEnv) getClass().getClassLoader()).userErr();
							}

						}.get();

					}
					""";
			writer.write(std);
		} catch (IOException e) {
			throw new RuntimeException("Cannot writer bundle startup script to " + bundleStartupScript, e);
		}

		return bundleStartupScript;
	}

	public static String getBundleClasspath(Long bundleId) throws IOException {
		BundleContext bc = FrameworkUtil.getBundle(OsgiExecutionControlProvider.class).getBundleContext();
		String framework = bc.getProperty("osgi.framework");
		Path frameworkLocation = Paths.get(URI.create(framework)).toAbsolutePath();
		Bundle fromBundle = bc.getBundle(bundleId);

		BundleWiring fromBundleWiring = fromBundle.adapt(BundleWiring.class);

		Set<Bundle> bundlesToAddToCompileClasspath = new TreeSet<>();

		// from bundle
		bundlesToAddToCompileClasspath.add(fromBundle);

		List<BundleWire> bundleWires = fromBundleWiring.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);
		for (BundleWire bw : bundleWires) {
			bundlesToAddToCompileClasspath.add(bw.getProviderWiring().getBundle());
		}

		StringJoiner classpath = new StringJoiner(File.pathSeparator);
		bundles: for (Bundle b : bundlesToAddToCompileClasspath) {
			if (b.getBundleId() == 0) {// system bundle
				classpath.add(frameworkLocation.toString());
				continue bundles;
			}
			Path p = bundleToPath(frameworkLocation, b);
			if (p != null)
				classpath.add(p.toString());
		}

		return classpath.toString();
	}

	static Path bundleToPath(Path frameworkLocation, Bundle bundle) throws IOException {
		String location = bundle.getLocation();
		if (location.startsWith("initial@reference:file:")) {// Eclipse IDE environment
			location = location.substring("initial@reference:file:".length());
			Path p = frameworkLocation.getParent().resolve(location).toAbsolutePath();
			if (Files.exists(p)) {
				p = p.toRealPath();
				// TODO load dev.properties from OSGi configuration directory
				if (Files.isDirectory(p))
					p = p.resolve("bin");
				return p;
			} else {
				log.warn("Ignore bundle " + p + " as it does not exist");
				return null;
			}
		} else if (location.startsWith("reference:file:")) {// a2+reference
			location = location.substring("reference:".length());
			Path p = Paths.get(URI.create(location));
			if (Files.exists(p)) {
				return p;
			} else {
				log.warn("Ignore bundle " + p + " as it does not exist");
				return null;
			}
		}
		Path p = Paths.get(location);
		return p;
	}
}
