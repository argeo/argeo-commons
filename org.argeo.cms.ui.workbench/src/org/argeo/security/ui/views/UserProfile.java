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
package org.argeo.security.ui.views;

import java.util.TreeSet;

import org.argeo.cms.auth.CurrentUser;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.security.ui.SecurityUiPlugin;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.part.ViewPart;

/** Information about the currently logged in user */
public class UserProfile extends ViewPart {
	public static String ID = SecurityUiPlugin.PLUGIN_ID + ".userProfile";

	private TableViewer viewer;

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(2, false));

		// Authentication authentication = CurrentUser.getAuthentication();
		// EclipseUiUtils.createGridLL(parent, "Name", authentication
		// .getPrincipal().toString());
		EclipseUiUtils.createGridLL(parent, "User ID",
				CurrentUser.getUsername());

		// roles table
		Table table = new Table(parent, SWT.V_SCROLL | SWT.BORDER);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		table.setLinesVisible(false);
		table.setHeaderVisible(false);
		viewer = new TableViewer(table);
		viewer.setContentProvider(new RolesContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		getViewSite().setSelectionProvider(viewer);
		viewer.setInput(getViewSite());
	}

	@Override
	public void setFocus() {
		viewer.getTable();
	}

	private class RolesContentProvider implements IStructuredContentProvider {
		private static final long serialVersionUID = -4576917440167866233L;

		public Object[] getElements(Object inputElement) {
			return new TreeSet<String>(CurrentUser.roles()).toArray();
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

}
