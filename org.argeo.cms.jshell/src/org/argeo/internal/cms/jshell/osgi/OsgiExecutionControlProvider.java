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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import jdk.jshell.execution.DirectExecutionControl;
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
		// TODO find a better way to get a default bundle context
		// NOTE: the related default bundle has to be started

//		String symbolicName = parameters.get(BUNDLE_PARAMETER);
//		Bundle fromBundle = getBundleFromSn(symbolicName);

		Long bundleId = Long.parseLong(parameters.get(BUNDLE_PARAMETER));
		Bundle fromBundle = getBundleFromId(bundleId);

		BundleWiring fromBundleWiring = fromBundle.adapt(BundleWiring.class);
		ClassLoader fromBundleClassLoader = fromBundleWiring.getClassLoader();

		// use the bundle classloade as context classloader
		Thread.currentThread().setContextClassLoader(fromBundleClassLoader);

		ExecutionControl executionControl = new DirectExecutionControl(
				new WrappingLoaderDelegate(env, fromBundleClassLoader));
		log.debug("JShell from " + fromBundle.getSymbolicName() + "_" + fromBundle.getVersion() + " ["
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
		Path bundleStartupScript = fromBundle.getDataFile("BUNDLE.jsh").toPath();

		BundleWiring fromBundleWiring = fromBundle.adapt(BundleWiring.class);
		ClassLoader fromBundleClassLoader = fromBundleWiring.getClassLoader();

		Set<String> packagesToImport = new TreeSet<>();

		// from bundle packages
		for (Package pkg : fromBundleClassLoader.getDefinedPackages()) {
			packagesToImport.add(pkg.getName());
		}

		List<BundleWire> bundleWires = fromBundleWiring.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);
		for (BundleWire bw : bundleWires) {
			packagesToImport.add(bw.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE).toString());
		}

		try (Writer writer = Files.newBufferedWriter(bundleStartupScript, StandardCharsets.UTF_8)) {
			for (String p : packagesToImport) {
				writer.write("import " + p + ".*;\n");
			}

			String std = """
					import jdk.jshell.spi.ExecutionEnv;

					InputStream STDIN = new Supplier<InputStream>() {

							@Override
							public InputStream get() {
								return ((ExecutionEnv) getClass().getClassLoader()).userIn();
							}

						}.get();
					PrintStream STDOUT = new Supplier<PrintStream>() {

							@Override
							public PrintStream get() {
								return ((ExecutionEnv) getClass().getClassLoader()).userOut();
							}

						}.get();
					PrintStream STDERR = new Supplier<PrintStream>() {

							@Override
							public PrintStream get() {
								return ((ExecutionEnv) getClass().getClassLoader()).userErr();
							}

						}.get();
										""";
			writer.write(std);
		} catch (IOException e) {
			throw new RuntimeException("Cannot writer bundle startup script to " + bundleStartupScript, e);
		}

		return bundleStartupScript;
	}

	public static String getBundleClasspath(Long bundleId) throws IOException {
		String framework = System.getProperty("osgi.framework");
		Path frameworkLocation = Paths.get(URI.create(framework)).toAbsolutePath();
		BundleContext bc = FrameworkUtil.getBundle(OsgiExecutionControlProvider.class).getBundleContext();
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
			classpath.add(p.toString());
		}

		return classpath.toString();
	}

	static Path bundleToPath(Path frameworkLocation, Bundle bundle) throws IOException {
		String location = bundle.getLocation();
		if (location.startsWith("initial@reference:file:")) {
			location = location.substring("initial@reference:file:".length());
			Path p = frameworkLocation.getParent().resolve(location).toRealPath();
			// TODO load dev.properties from OSGi configuration directory
			if (Files.isDirectory(p))
				p = p.resolve("bin");
			return p;
		}
		Path p = Paths.get(location);
		return p;
	}
}
