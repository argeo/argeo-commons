/*
 * Copyright (C) 2007-2012 Mathieu Baudier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.osgi.ui.explorer.views;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.osgi.ui.explorer.OsgiExplorerPlugin;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

/** <b>Experimental</b> The OSGi runtime from a module perspective. */
public class ModulesView extends ViewPart {
	private final static Log log = LogFactory.getLog(ModulesView.class);

	private TreeViewer viewer;

	private PackageAdmin packageAdmin;

	private Comparator<ExportedPackage> exportedPackageComparator = new Comparator<ExportedPackage>() {

		public int compare(ExportedPackage o1, ExportedPackage o2) {
			if (!o1.getName().equals(o2.getName()))
				return o1.getName().compareTo(o2.getName());
			else
				return o1.getVersion().compareTo(o2.getVersion());
		}
	};

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new ModulesContentProvider());
		viewer.setLabelProvider(new ModulesLabelProvider());
		viewer.setInput(OsgiExplorerPlugin.getDefault().getBundle()
				.getBundleContext());
	}

	@Override
	public void setFocus() {
		viewer.getTree().setFocus();
	}

	private class ModulesContentProvider implements ITreeContentProvider {

		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof BundleContext) {
				BundleContext bundleContext = (BundleContext) parentElement;
				Bundle[] bundles = bundleContext.getBundles();

				TreeParent bundlesNode = new TreeParent("Bundles");
				for (Bundle bundle : bundles) {
					if (bundle.getState() == Bundle.ACTIVE)
						bundlesNode.addChild(new BundleNode(bundle));
				}

				// scan packages
				ServiceReference paSr = bundleContext
						.getServiceReference(PackageAdmin.class.getName());
				// TODO: make a cleaner referencing
				packageAdmin = (PackageAdmin) bundleContext.getService(paSr);

				Bundle bundle1 = null;
				Bundle bundle2 = null;

				Map<Bundle, Set<ExportedPackage>> importedPackages = new HashMap<Bundle, Set<ExportedPackage>>();
				Map<String, Set<ExportedPackage>> packages = new TreeMap<String, Set<ExportedPackage>>();
				for (Bundle bundle : bundles) {
					if (bundle.getSymbolicName()
							.equals("org.argeo.security.ui"))
						bundle1 = bundle;
					if (bundle.getSymbolicName().equals(
							"org.argeo.security.equinox"))
						bundle2 = bundle;

					ExportedPackage[] pkgs = packageAdmin
							.getExportedPackages(bundle);
					if (pkgs != null)
						for (ExportedPackage pkg : pkgs) {
							if (!packages.containsKey(pkg.getName()))
								packages.put(pkg.getName(),
										new TreeSet<ExportedPackage>(
												exportedPackageComparator));
							Set<ExportedPackage> expPackages = (Set<ExportedPackage>) packages
									.get(pkg.getName());
							expPackages.add(pkg);

							// imported
							for (Bundle b : pkg.getImportingBundles()) {
								if (bundle.getBundleId() != b.getBundleId()) {
									if (!importedPackages.containsKey(b))
										importedPackages
												.put(b,
														new TreeSet<ExportedPackage>(
																exportedPackageComparator));
									Set<ExportedPackage> impPackages = (Set<ExportedPackage>) importedPackages
											.get(b);
									impPackages.add(pkg);
								}
							}
						}
				}

				TreeParent mPackageNode = new TreeParent("Multiple Packages");
				// TreeParent aPackageNode = new TreeParent("All Packages");
				for (String packageName : packages.keySet()) {
					Set<ExportedPackage> pkgs = packages.get(packageName);
					if (pkgs.size() > 1) {
						MultiplePackagesNode mpn = new MultiplePackagesNode(
								packageName, pkgs);
						mPackageNode.addChild(mpn);
						// aPackageNode.addChild(mpn);
					} else {
						// aPackageNode.addChild(new ExportedPackageNode(pkgs
						// .iterator().next()));
					}
				}

				return new Object[] { bundlesNode, mPackageNode };// ,
																	// aPackageNode
																	// };
			} else if (parentElement instanceof TreeParent) {
				return ((TreeParent) parentElement).getChildren();
			} else {
				return null;
			}
		}

		public Object getParent(Object element) {
			// TODO Auto-generated method stub
			return null;
		}

		public boolean hasChildren(Object element) {
			if (element instanceof TreeParent) {
				return ((TreeParent) element).hasChildren();
			}
			return false;
		}

		public void dispose() {
			// TODO Auto-generated method stub

		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// TODO Auto-generated method stub

		}

	}

	protected Map<String, ExportedPackage> dependencySpace(Bundle bundle,
			Map<Bundle, Set<ExportedPackage>> importedPackages,
			Map<String, Set<String>> traces) {
		log.debug("Dependency space for " + bundle.getSymbolicName());
		Map<String, ExportedPackage> space = new TreeMap<String, ExportedPackage>();
		fillDependencySpace(space, bundle, importedPackages,
				bundle.getSymbolicName(), traces);
		return space;
	}

	/** Recursive */
	protected void fillDependencySpace(Map<String, ExportedPackage> space,
			Bundle bundle, Map<Bundle, Set<ExportedPackage>> importedPackages,
			String currTrace, Map<String, Set<String>> traces) {
		if (importedPackages.containsKey(bundle)) {
			Set<ExportedPackage> imports = importedPackages.get(bundle);
			// log.debug("## Fill dependency space for " + bundle + " : ");
			for (ExportedPackage pkg : imports) {
				if (!traces.containsKey(pkg.getName()))
					traces.put(pkg.getName(), new TreeSet<String>());
				traces.get(pkg.getName()).add(currTrace);
				if (!space.containsKey(pkg.getName())) {
					space.put(pkg.getName(), pkg);
					Bundle exportingBundle = pkg.getExportingBundle();
					// if (bundle.getBundleId() !=
					// exportingBundle.getBundleId())
					fillDependencySpace(space, exportingBundle,
							importedPackages, currTrace + " > "
									+ exportingBundle.getSymbolicName(), traces);
				}
			}
		}
	}

	private class ModulesLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		public Image getColumnImage(Object element, int columnIndex) {
			// TODO Auto-generated method stub
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			return getText(element);
		}

	}

	class BundleNode extends TreeParent {
		private final Bundle bundle;

		public BundleNode(Bundle bundle) {
			super(bundle.getSymbolicName());
			this.bundle = bundle;

			// Registered services
			ServiceReference[] registeredServices = bundle
					.getRegisteredServices();
			if (registeredServices != null) {
				TreeParent registeredServicesNode = new TreeParent(
						"Registered Services");
				addChild(registeredServicesNode);
				for (ServiceReference sr : registeredServices) {
					if (sr != null)
						registeredServicesNode
								.addChild(new ServiceReferenceNode(sr));
				}
			}

			// Used services
			ServiceReference[] usedServices = bundle.getRegisteredServices();
			if (usedServices != null) {
				TreeParent usedServicesNode = new TreeParent("Used Services");
				addChild(usedServicesNode);
				for (ServiceReference sr : usedServices) {
					if (sr != null)
						usedServicesNode.addChild(new ServiceReferenceNode(sr));
				}
			}
		}

		public Bundle getBundle() {
			return bundle;
		}

	}

	class ServiceReferenceNode extends TreeParent {
		private final ServiceReference serviceReference;

		public ServiceReferenceNode(ServiceReference serviceReference) {
			super(serviceReference.toString());
			this.serviceReference = serviceReference;

			Bundle[] usedBundles = serviceReference.getUsingBundles();
			if (usedBundles != null) {
				TreeParent usingBundles = new TreeParent("Using Bundles");
				addChild(usingBundles);
				for (Bundle b : usedBundles) {
					if (b != null)
						usingBundles.addChild(new TreeParent(b
								.getSymbolicName()));
				}
			}

			TreeParent properties = new TreeParent("Properties");
			addChild(properties);
			for (String key : serviceReference.getPropertyKeys()) {
				properties.addChild(new TreeParent(key + "="
						+ serviceReference.getProperty(key)));
			}

		}

		public ServiceReference getServiceReference() {
			return serviceReference;
		}

	}

	class MultiplePackagesNode extends TreeParent {
		private String packageName;
		private Set<ExportedPackage> exportedPackages;

		public MultiplePackagesNode(String packageName,
				Set<ExportedPackage> exportedPackages) {
			super(packageName);
			this.packageName = packageName;
			this.exportedPackages = exportedPackages;
			for (ExportedPackage pkg : exportedPackages) {
				addChild(new ExportedPackageNode(pkg));
			}
		}

	}

	class ConflictingPackageNode extends TreeParent {
		private ExportedPackage exportedPackage;

		public ConflictingPackageNode(ExportedPackage exportedPackage) {
			super(exportedPackage.getName() + " - "
					+ exportedPackage.getVersion() + " ("
					+ exportedPackage.getExportingBundle() + ")");
			this.exportedPackage = exportedPackage;

			TreeParent bundlesNode = new TreeParent("Dependent Bundles");
			this.addChild(bundlesNode);
			Map<String, Bundle> bundles = new TreeMap<String, Bundle>();
			for (Bundle b : exportedPackage.getImportingBundles()) {
				bundles.put(b.getSymbolicName(), b);
			}
			for (String key : bundles.keySet()) {
				addDependentBundles(bundlesNode, bundles.get(key));
			}
		}
	}

	protected void addDependentBundles(TreeParent parent, Bundle bundle) {
		TreeParent bundleNode = new TreeParent(bundle.toString());
		parent.addChild(bundleNode);
		Map<String, Bundle> bundles = new TreeMap<String, Bundle>();
		ExportedPackage[] pkgs = packageAdmin.getExportedPackages(bundle);
		if (pkgs != null)
			for (ExportedPackage pkg : pkgs) {
				for (Bundle b : pkg.getImportingBundles()) {
					if (!bundles.containsKey(b.getSymbolicName())
							&& b.getBundleId() != bundle.getBundleId()) {
						bundles.put(b.getSymbolicName(), b);
					}
				}
			}

		for (String key : bundles.keySet()) {
			addDependentBundles(bundleNode, bundles.get(key));
		}
	}

	class ExportedPackageNode extends TreeParent {
		private ExportedPackage exportedPackage;

		public ExportedPackageNode(ExportedPackage exportedPackage) {
			super(exportedPackage.getName() + " - "
					+ exportedPackage.getVersion() + " ("
					+ exportedPackage.getExportingBundle() + ")");
			this.exportedPackage = exportedPackage;
			for (Bundle bundle : exportedPackage.getImportingBundles()) {
				addChild(new BundleNode(bundle));
			}
		}
	}
}
