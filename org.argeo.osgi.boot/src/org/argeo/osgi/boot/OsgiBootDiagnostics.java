package org.argeo.osgi.boot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

@SuppressWarnings("deprecation")
class OsgiBootDiagnostics {
	private final BundleContext bundleContext;

	public OsgiBootDiagnostics(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}
	/*
	 * DIAGNOSTICS
	 */
	/** Check unresolved bundles */
	protected void checkUnresolved() {
		// Refresh
		ServiceReference<PackageAdmin> packageAdminRef = bundleContext.getServiceReference(PackageAdmin.class);
		PackageAdmin packageAdmin = (PackageAdmin) bundleContext.getService(packageAdminRef);
		packageAdmin.resolveBundles(null);

		Bundle[] bundles = bundleContext.getBundles();
		List<Bundle> unresolvedBundles = new ArrayList<Bundle>();
		for (int i = 0; i < bundles.length; i++) {
			int bundleState = bundles[i].getState();
			if (!(bundleState == Bundle.ACTIVE || bundleState == Bundle.RESOLVED || bundleState == Bundle.STARTING))
				unresolvedBundles.add(bundles[i]);
		}

		if (unresolvedBundles.size() != 0) {
			OsgiBootUtils.warn("Unresolved bundles " + unresolvedBundles);
		}
	}

	/** List packages exported twice. */
	public Map<String, Set<String>> findPackagesExportedTwice() {
		ServiceReference<PackageAdmin> paSr = bundleContext.getServiceReference(PackageAdmin.class);
		PackageAdmin packageAdmin = (PackageAdmin) bundleContext.getService(paSr);

		// find packages exported twice
		Bundle[] bundles = bundleContext.getBundles();
		Map<String, Set<String>> exportedPackages = new TreeMap<String, Set<String>>();
		for (int i = 0; i < bundles.length; i++) {
			Bundle bundle = bundles[i];
			ExportedPackage[] pkgs = packageAdmin.getExportedPackages(bundle);
			if (pkgs != null)
				for (int j = 0; j < pkgs.length; j++) {
					String pkgName = pkgs[j].getName();
					if (!exportedPackages.containsKey(pkgName)) {
						exportedPackages.put(pkgName, new TreeSet<String>());
					}
					(exportedPackages.get(pkgName)).add(bundle.getSymbolicName() + "_" + bundle.getVersion());
				}
		}
		Map<String, Set<String>> duplicatePackages = new TreeMap<String, Set<String>>();
		Iterator<String> it = exportedPackages.keySet().iterator();
		while (it.hasNext()) {
			String pkgName = it.next().toString();
			Set<String> bdles = exportedPackages.get(pkgName);
			if (bdles.size() > 1)
				duplicatePackages.put(pkgName, bdles);
		}
		return duplicatePackages;
	}

}
