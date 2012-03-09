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
package org.argeo.jcr.ui.explorer.views;

import java.util.List;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Value;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.eclipse.ui.jcr.AsyncUiEventListener;
import org.argeo.eclipse.ui.jcr.utils.NodeViewerComparer;
import org.argeo.eclipse.ui.jcr.views.AbstractJcrBrowser;
import org.argeo.jcr.RepositoryRegister;
import org.argeo.jcr.security.JcrKeyring;
import org.argeo.jcr.ui.explorer.JcrExplorerPlugin;
import org.argeo.jcr.ui.explorer.browser.NodeContentProvider;
import org.argeo.jcr.ui.explorer.browser.NodeLabelProvider;
import org.argeo.jcr.ui.explorer.browser.PropertiesContentProvider;
import org.argeo.jcr.ui.explorer.model.SingleJcrNode;
import org.argeo.jcr.ui.explorer.utils.GenericNodeDoubleClickListener;
import org.argeo.jcr.ui.explorer.utils.JcrUiUtils;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

/**
 * Basic View to display a sash form to browse a JCR compliant multirepository
 * environment
 */

public class GenericJcrBrowser extends AbstractJcrBrowser {
	// private final static Log log =
	// LogFactory.getLog(GenericJcrBrowser.class);
	public final static String ID = JcrExplorerPlugin.ID + ".browserView";
	private boolean sortChildNodes = false;

	/* DEPENDENCY INJECTION */
	private JcrKeyring jcrKeyring;
	private RepositoryRegister repositoryRegister;
	private RepositoryFactory repositoryFactory;

	// This page widgets
	private TreeViewer nodesViewer;
	private NodeContentProvider nodeContentProvider;
	private TableViewer propertiesViewer;
	private EventListener resultsObserver;

	// Manage documents
	// private JcrFileProvider jcrFileProvider;
	// private FileHandler fileHandler;

	@Override
	public void createPartControl(Composite parent) {

		// look for session
		// Session nodeSession = jcrKeyring != null ? jcrKeyring.getSession()
		// : null;
		// if (nodeSession == null) {
		// Repository nodeRepository = JcrUtils.getRepositoryByAlias(
		// repositoryRegister, ArgeoJcrConstants.ALIAS_NODE);
		// if (nodeRepository != null)
		// try {
		// nodeSession = nodeRepository.login();
		// // TODO : enhance that to enable multirepository listener.
		// } catch (RepositoryException e1) {
		// throw new ArgeoException("Cannot login to node repository");
		// }
		// }

		// Instantiate the generic object that fits for
		// both RCP & RAP
		// Note that in RAP, it registers a service handler that provide the
		// access to the files.
		// jcrFileProvider = new JcrFileProvider();
		// fileHandler = new FileHandler(jcrFileProvider);

		parent.setLayout(new FillLayout());
		SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
		sashForm.setSashWidth(4);
		sashForm.setLayout(new FillLayout());

		// Create the tree on top of the view
		Composite top = new Composite(sashForm, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		top.setLayout(gl);

		nodeContentProvider = new NodeContentProvider(jcrKeyring,
				repositoryRegister, repositoryFactory, sortChildNodes);

		// nodes viewer
		nodesViewer = createNodeViewer(top, nodeContentProvider);

		// context menu : it is completely defined in the plugin.xml file.
		MenuManager menuManager = new MenuManager();
		Menu menu = menuManager.createContextMenu(nodesViewer.getTree());

		nodesViewer.getTree().setMenu(menu);
		getSite().registerContextMenu(menuManager, nodesViewer);
		getSite().setSelectionProvider(nodesViewer);

		nodesViewer.setInput(getViewSite());

		// Create the property viewer on the bottom
		Composite bottom = new Composite(sashForm, SWT.NONE);
		bottom.setLayout(new GridLayout(1, false));
		propertiesViewer = createPropertiesViewer(bottom);

		sashForm.setWeights(getWeights());
		nodesViewer.setComparer(new NodeViewerComparer());
	}

	/**
	 * To be overridden to adapt size of form and result frames.
	 */
	protected int[] getWeights() {
		return new int[] { 70, 30 };
	}

	protected TreeViewer createNodeViewer(Composite parent,
			final ITreeContentProvider nodeContentProvider) {

		final TreeViewer tmpNodeViewer = new TreeViewer(parent, SWT.MULTI);

		tmpNodeViewer.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));

		tmpNodeViewer.setContentProvider(nodeContentProvider);
		tmpNodeViewer.setLabelProvider(new NodeLabelProvider());
		tmpNodeViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent event) {
						if (!event.getSelection().isEmpty()) {
							IStructuredSelection sel = (IStructuredSelection) event
									.getSelection();
							Object firstItem = sel.getFirstElement();
							if (firstItem instanceof SingleJcrNode)
								propertiesViewer
										.setInput(((SingleJcrNode) firstItem)
												.getNode());
						} else {
							propertiesViewer.setInput(getViewSite());
						}
					}
				});

		resultsObserver = new TreeObserver(tmpNodeViewer.getTree().getDisplay());
		if (jcrKeyring != null)
			try {
				ObservationManager observationManager = jcrKeyring.getSession()
						.getWorkspace().getObservationManager();
				observationManager.addEventListener(resultsObserver,
						Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED, "/",
						true, null, null, false);
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot register listeners", e);
			}

		tmpNodeViewer
				.addDoubleClickListener(new GenericNodeDoubleClickListener(
						tmpNodeViewer));
		return tmpNodeViewer;
	}

	protected TableViewer createPropertiesViewer(Composite parent) {
		propertiesViewer = new TableViewer(parent);
		propertiesViewer.getTable().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		propertiesViewer.getTable().setHeaderVisible(true);
		propertiesViewer.setContentProvider(new PropertiesContentProvider());
		TableViewerColumn col = new TableViewerColumn(propertiesViewer,
				SWT.NONE);
		col.getColumn().setText("Name");
		col.getColumn().setWidth(200);
		col.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				try {
					return ((Property) element).getName();
				} catch (RepositoryException e) {
					throw new ArgeoException(
							"Unexpected exception in label provider", e);
				}
			}
		});
		col = new TableViewerColumn(propertiesViewer, SWT.NONE);
		col.getColumn().setText("Value");
		col.getColumn().setWidth(400);
		col.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				try {
					Property property = (Property) element;
					if (property.getType() == PropertyType.BINARY)
						return "<binary>";
					else if (property.isMultiple()) {
						StringBuffer buf = new StringBuffer("[");
						Value[] values = property.getValues();
						for (int i = 0; i < values.length; i++) {
							if (i != 0)
								buf.append(", ");
							buf.append(values[i].getString());
						}
						buf.append(']');
						return buf.toString();
					} else
						return property.getValue().getString();
				} catch (RepositoryException e) {
					throw new ArgeoException(
							"Unexpected exception in label provider", e);
				}
			}
		});
		col = new TableViewerColumn(propertiesViewer, SWT.NONE);
		col.getColumn().setText("Type");
		col.getColumn().setWidth(200);
		col.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				try {
					return PropertyType.nameFromValue(((Property) element)
							.getType());
				} catch (RepositoryException e) {
					throw new ArgeoException(
							"Unexpected exception in label provider", e);
				}
			}
		});
		propertiesViewer.setInput(getViewSite());
		return propertiesViewer;
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	protected TreeViewer getNodeViewer() {
		return nodesViewer;
	}

	/** Notifies the current view that a node has been added */
	public void nodeAdded(TreeParent parentNode) {
		// insure that Ui objects have been correctly created:
		JcrUiUtils.forceRefreshIfNeeded(parentNode);
		getNodeViewer().refresh(parentNode);
		getNodeViewer().expandToLevel(parentNode, 1);
	}

	/** Notifies the current view that a node has been added */
	public void nodeRemoved(TreeParent parentNode) {
		IStructuredSelection newSel = new StructuredSelection(parentNode);
		getNodeViewer().setSelection(newSel, true);
		// Force refresh
		IStructuredSelection tmpSel = (IStructuredSelection) getNodeViewer()
				.getSelection();
		getNodeViewer().refresh(tmpSel.getFirstElement());
	}

	class TreeObserver extends AsyncUiEventListener {

		public TreeObserver(Display display) {
			super(display);
		}

		@Override
		protected Boolean willProcessInUiThread(List<Event> events)
				throws RepositoryException {
			for (Event event : events) {
				if (getLog().isTraceEnabled())
					getLog().debug("Received event " + event);
				String path = event.getPath();
				int index = path.lastIndexOf('/');
				String propertyName = path.substring(index + 1);
				if (getLog().isTraceEnabled())
					getLog().debug("Concerned property " + propertyName);
			}
			return false;
		}

		protected void onEventInUiThread(List<Event> events)
				throws RepositoryException {
			if (getLog().isTraceEnabled())
				getLog().trace("Refresh result list");
			nodesViewer.refresh();
		}

	}

	public void setSortChildNodes(boolean sortChildNodes) {
		this.sortChildNodes = sortChildNodes;
	}

	public boolean getSortChildNodes() {
		return sortChildNodes;
	}

	/* DEPENDENCY INJECTION */
	public void setRepositoryRegister(RepositoryRegister repositoryRegister) {
		this.repositoryRegister = repositoryRegister;
	}

	public void setJcrKeyring(JcrKeyring jcrKeyring) {
		this.jcrKeyring = jcrKeyring;
	}

	public void setRepositoryFactory(RepositoryFactory repositoryFactory) {
		this.repositoryFactory = repositoryFactory;
	}

}
