package org.argeo.jcr.ui.explorer.views;

import java.util.Arrays;
import java.util.List;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.jcr.AsyncUiEventListener;
import org.argeo.eclipse.ui.jcr.utils.NodeViewerComparer;
import org.argeo.eclipse.ui.jcr.views.AbstractJcrBrowser;
import org.argeo.eclipse.ui.specific.FileHandler;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.RepositoryRegister;
import org.argeo.jcr.ui.explorer.browser.NodeContentProvider;
import org.argeo.jcr.ui.explorer.browser.NodeLabelProvider;
import org.argeo.jcr.ui.explorer.browser.PropertiesContentProvider;
import org.argeo.jcr.ui.explorer.utils.GenericNodeDoubleClickListener;
import org.argeo.jcr.ui.explorer.utils.JcrFileProvider;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
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
	private final static Log log = LogFactory.getLog(GenericJcrBrowser.class);

	/* DEPENDENCY INJECTION */
	private Session session;
	private RepositoryRegister repositoryRegister;

	// This page widgets
	private TreeViewer nodesViewer;
	private NodeContentProvider nodeContentProvider;
	private TableViewer propertiesViewer;
	private EventListener resultsObserver;

	// Manage documents
	private JcrFileProvider jcrFileProvider;
	private FileHandler fileHandler;

	@Override
	public void createPartControl(Composite parent) {

		// look for session
		Session nodeSession = session;
		if (nodeSession == null) {
			Repository nodeRepository = JcrUtils.getRepositoryByAlias(
					repositoryRegister, ArgeoJcrConstants.ALIAS_NODE);
			if (nodeRepository != null)
				try {
					nodeSession = nodeRepository.login();
					// TODO : enhance that to enable multirepository listener.
					session = nodeSession;
				} catch (RepositoryException e1) {
					throw new ArgeoException("Cannot login to node repository");
				}
		}

		// Instantiate the generic object that fits for
		// both RCP & RAP
		// Note that in RAP, it registers a service handler that provide the
		// access to the files.
		jcrFileProvider = new JcrFileProvider();
		fileHandler = new FileHandler(jcrFileProvider);

		parent.setLayout(new FillLayout());
		SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
		sashForm.setSashWidth(4);
		sashForm.setLayout(new FillLayout());

		// Create the tree on top of the view
		Composite top = new Composite(sashForm, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		top.setLayout(gl);

		nodeContentProvider = new NodeContentProvider(nodeSession,
				repositoryRegister);

		// nodes viewer
		nodesViewer = createNodeViewer(top, nodeContentProvider);

		// context menu
		MenuManager menuManager = new MenuManager();
		Menu menu = menuManager.createContextMenu(nodesViewer.getTree());
		nodesViewer.getTree().setMenu(menu);
		getSite().registerContextMenu(menuManager, nodesViewer);
		getSite().setSelectionProvider(nodesViewer);

		nodesViewer.setInput(getViewSite());

		// Create the property viewer on the bottom
		Composite bottom = new Composite(sashForm, SWT.NONE);
		bottom.setLayout(new GridLayout(1, false));
		propertiesViewer = new TableViewer(bottom);
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
					else if (property.isMultiple())
						return Arrays.asList(property.getValues()).toString();
					else
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

		sashForm.setWeights(getWeights());
		nodesViewer.setComparer(new NodeViewerComparer());
	}

	/**
	 * To be overridden to adapt size of form and result frames.
	 */
	protected int[] getWeights() {
		return new int[] { 70, 30 };
	}

	// @Override
	// public void setFocus() {
	// nodesViewer.getTree().setFocus();
	// }
	//
	// /*
	// * NOTIFICATION
	// */
	// public void refresh(Object obj) {
	// nodesViewer.refresh(obj);
	// }
	//
	// public void nodeAdded(Node parentNode, Node newNode) {
	// nodesViewer.refresh(parentNode);
	// nodesViewer.expandToLevel(newNode, 0);
	// }
	//
	// public void nodeRemoved(Node parentNode) {
	//
	// IStructuredSelection newSel = new StructuredSelection(parentNode);
	// nodesViewer.setSelection(newSel, true);
	// // Force refresh
	// IStructuredSelection tmpSel = (IStructuredSelection) nodesViewer
	// .getSelection();
	// nodesViewer.refresh(tmpSel.getFirstElement());
	// }

	private JcrFileProvider getJcrFileProvider() {
		return jcrFileProvider;
	}

	private FileHandler getFileHandler() {
		return fileHandler;
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
							propertiesViewer.setInput(sel.getFirstElement());
						} else {
							propertiesViewer.setInput(getViewSite());
						}
					}
				});

		resultsObserver = new TreeObserver(tmpNodeViewer.getTree().getDisplay());
		try {
			ObservationManager observationManager = session.getWorkspace()
					.getObservationManager();
			observationManager.addEventListener(resultsObserver,
					Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED, "/", true,
					null, null, false);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot register listeners", e);
		}

		tmpNodeViewer
				.addDoubleClickListener(new GenericNodeDoubleClickListener(
						tmpNodeViewer));
		return tmpNodeViewer;
	}

	@Override
	protected TreeViewer getNodeViewer() {
		return nodesViewer;
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

	/* DEPENDENCY INJECTION */
	public void setRepositoryRegister(RepositoryRegister repositoryRegister) {
		this.repositoryRegister = repositoryRegister;
	}

	public void setSession(Session session) {
		this.session = session;
	}

}
