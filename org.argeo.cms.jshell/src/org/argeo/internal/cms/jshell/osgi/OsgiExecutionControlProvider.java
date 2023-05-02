package org.argeo.internal.cms.jshell.osgi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
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
		BundleContext bc = FrameworkUtil.getBundle(OsgiExecutionControlProvider.class).getBundleContext();

		String symbolicName = parameters.get(BUNDLE_PARAMETER);
		Objects.requireNonNull(symbolicName);
		NavigableMap<Version, Bundle> bundles = new TreeMap<Version, Bundle>();
		for (Bundle b : bc.getBundles()) {
			if (symbolicName.equals(b.getSymbolicName()))
				bundles.put(b.getVersion(), b);
		}
		Bundle fromBundle = bundles.lastEntry().getValue();

		BundleWiring fromBundleWiring = fromBundle.adapt(BundleWiring.class);
		ClassLoader fromBundleClassLoader = fromBundleWiring.getClassLoader();

		Set<String> packagesToImport = new TreeSet<>();
		Set<Bundle> bundlesToAddToCompileClasspath = new TreeSet<>();

		// from bundle
		bundlesToAddToCompileClasspath.add(fromBundle);
		// from bundle packages
		for (Package pkg : fromBundleClassLoader.getDefinedPackages()) {
			packagesToImport.add(pkg.getName());
		}

//		System.out.println(Arrays.asList(fromBundleClassLoader.getDefinedPackages()));
		List<BundleWire> bundleWires = fromBundleWiring.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);
		for (BundleWire bw : bundleWires) {
//			System.out.println(bw.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
			bundlesToAddToCompileClasspath.add(bw.getProviderWiring().getBundle());
			packagesToImport.add(bw.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE).toString());
		}
		log.debug("JShell from " + fromBundle.getSymbolicName() + "_" + fromBundle.getVersion() + " ["
				+ fromBundle.getBundleId() + "]");
		log.debug("  required packages " + packagesToImport);
		log.debug("  required bundles " + bundlesToAddToCompileClasspath);

		ExecutionControl executionControl = new DirectExecutionControl(
				new WrappingLoaderDelegate(fromBundleClassLoader));
		return executionControl;
	}

}
