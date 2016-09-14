/*
 * Copyright (C) 2007-2012 Argeo GmbH
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
package org.argeo.eclipse.ui.workbench.osgi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.argeo.cms.ui.workbench.SecurityUiPlugin;
import org.argeo.eclipse.ui.TreeParent;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

/** <b>Experimental</b> The OSGi runtime from a module perspective. */
@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
public class MultiplePackagesView extends ViewPart {
	private TreeViewer viewer;
	private PackageAdmin packageAdmin;
	private Comparator<ExportedPackage> epc = new Comparator<ExportedPackage>() {
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
		viewer.setLabelProvider(new LabelProvider());
		viewer.setInput(SecurityUiPlugin.getDefault().getBundle()
				.getBundleContext());
	}

	@Override
	public void setFocus() {
		viewer.getTree().setFocus();
	}

	private class ModulesContentProvider implements ITreeContentProvider {
		private static final long serialVersionUID = 3819934804640641721L;

		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof BundleContext) {
				BundleContext bundleContext = (BundleContext) parentElement;
				Bundle[] bundles = bundleContext.getBundles();

				// scan packages
				ServiceReference paSr = bundleContext
						.getServiceReference(PackageAdmin.class.getName());
				// TODO: make a cleaner referencing
				packageAdmin = (PackageAdmin) bundleContext.getService(paSr);

				Map<Bundle, Set<ExportedPackage>> imported = new HashMap<Bundle, Set<ExportedPackage>>();
				Map<String, Set<ExportedPackage>> packages = new TreeMap<String, Set<ExportedPackage>>();
				for (Bundle bundle : bundles) {
					processBundle(bundle, imported, packages);
				}

				List<MultiplePackagesNode> multiplePackages = new ArrayList<MultiplePackagesNode>();
				for (String packageName : packages.keySet()) {
					Set<ExportedPackage> pkgs = packages.get(packageName);
					if (pkgs.size() > 1) {
						MultiplePackagesNode mpn = new MultiplePackagesNode(
								packageName, pkgs);
						multiplePackages.add(mpn);
					}
				}

				return multiplePackages.toArray();
			} else if (parentElement instanceof TreeParent) {
				return ((TreeParent) parentElement).getChildren();
			} else {
				return null;
			}
		}

		protected void processBundle(Bundle bundle,
				Map<Bundle, Set<ExportedPackage>> imported,
				Map<String, Set<ExportedPackage>> packages) {
			ExportedPackage[] pkgs = packageAdmin.getExportedPackages(bundle);
			if (pkgs == null)
				return;
			for (ExportedPackage pkg : pkgs) {
				if (!packages.containsKey(pkg.getName()))
					packages.put(pkg.getName(), new TreeSet<ExportedPackage>(
							epc));
				Set<ExportedPackage> expPackages = packages.get(pkg.getName());
				expPackages.add(pkg);

				// imported
				for (Bundle b : pkg.getImportingBundles()) {
					if (bundle.getBundleId() != b.getBundleId()) {
						if (!imported.containsKey(b)) {
							imported.put(b, new TreeSet<ExportedPackage>(epc));
						}
						Set<ExportedPackage> impPackages = imported.get(b);
						impPackages.add(pkg);
					}
				}
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
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	private class MultiplePackagesNode extends TreeParent {
		public MultiplePackagesNode(String packageName,
				Set<ExportedPackage> exportedPackages) {
			super(packageName);
			for (ExportedPackage pkg : exportedPackages) {
				addChild(new ExportedPackageNode(pkg));
			}
		}
	}

	private class ExportedPackageNode extends TreeParent {
		public ExportedPackageNode(ExportedPackage exportedPackage) {
			super(exportedPackage.getName() + " - "
					+ exportedPackage.getVersion() + " ("
					+ exportedPackage.getExportingBundle() + ")");
			for (Bundle bundle : exportedPackage.getImportingBundles()) {
				addChild(new BundleNode(bundle, true));
			}
		}
	}
}
