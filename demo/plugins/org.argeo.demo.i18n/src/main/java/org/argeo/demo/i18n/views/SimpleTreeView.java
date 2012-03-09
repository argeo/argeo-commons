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
package org.argeo.demo.i18n.views;

import org.argeo.demo.i18n.I18nDemoPlugin;
import org.argeo.demo.i18n.providers.SimpleContentProvider;
import org.argeo.demo.i18n.providers.SimpleLabelProvider;
import org.argeo.demo.i18n.utils.GenericDoubleClickListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.part.ViewPart;

/**
 * Basic View to display a tree with internationalized labels
 */

public class SimpleTreeView extends ViewPart {
	// private final static Log log = LogFactory.getLog(SimpleTreeView.class);
	public final static String ID = I18nDemoPlugin.ID + ".simpleTreeView";

	// This page widgets
	private TreeViewer treeViewer;
	private SimpleContentProvider treeContentProvider;

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());

		// Creates the tree
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		composite.setLayout(gl);

		// tree viewer
		treeContentProvider = new SimpleContentProvider();
		treeViewer = createTreeViewer(composite, treeContentProvider);

		// context menu : it is completely defined in the plugin.xml file.
		MenuManager menuManager = new MenuManager();
		Menu menu = menuManager.createContextMenu(treeViewer.getTree());
		treeViewer.getTree().setMenu(menu);
		getSite().registerContextMenu(menuManager, treeViewer);

		getSite().setSelectionProvider(treeViewer);

		treeViewer.setInput(getViewSite());
	}

	protected TreeViewer createTreeViewer(Composite parent,
			final ITreeContentProvider treeContentProvider) {

		final TreeViewer tmpTreeViewer = new TreeViewer(parent, SWT.MULTI);

		tmpTreeViewer.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));

		tmpTreeViewer.setContentProvider(treeContentProvider);
		tmpTreeViewer.setLabelProvider(new SimpleLabelProvider());
		tmpTreeViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent event) {
						// something can be done here
					}
				});

		tmpTreeViewer.addDoubleClickListener(new GenericDoubleClickListener(
				tmpTreeViewer));
		return tmpTreeViewer;
	}

	@Override
	public void setFocus() {
		// Do nothing for the time being.
	}
}
