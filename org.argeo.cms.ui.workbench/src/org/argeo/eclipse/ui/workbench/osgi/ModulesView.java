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
import java.util.List;

import org.argeo.cms.ui.workbench.SecurityUiPlugin;
import org.argeo.eclipse.ui.TreeParent;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/** The OSGi runtime from a module perspective. */
public class ModulesView extends ViewPart {
	private TreeViewer viewer;

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new ModulesContentProvider());
		viewer.setLabelProvider(new ModulesLabelProvider());
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

				List<BundleNode> modules = new ArrayList<BundleNode>();
				for (Bundle bundle : bundles) {
					if (bundle.getState() == Bundle.ACTIVE)
						modules.add(new BundleNode(bundle, true));
				}
				return modules.toArray();
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
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	private class ModulesLabelProvider extends StateLabelProvider {
		private static final long serialVersionUID = 5290046145534824722L;

		@Override
		public String getText(Object element) {
			if (element instanceof BundleNode)
				return element.toString() + " ["
						+ ((BundleNode) element).getBundle().getBundleId()
						+ "]";
			return element.toString();
		}
	}
}
