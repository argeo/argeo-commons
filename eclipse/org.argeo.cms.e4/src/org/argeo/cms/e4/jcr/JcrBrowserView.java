package org.argeo.cms.e4.jcr;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.argeo.api.cms.CmsConstants;
import org.argeo.cms.CmsException;
import org.argeo.cms.security.CryptoKeyring;
import org.argeo.cms.security.Keyring;
import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.cms.ui.jcr.JcrBrowserUtils;
import org.argeo.cms.ui.jcr.NodeContentProvider;
import org.argeo.cms.ui.jcr.NodeLabelProvider;
import org.argeo.cms.ui.jcr.OsgiRepositoryRegister;
import org.argeo.cms.ui.jcr.PropertiesContentProvider;
import org.argeo.cms.ui.jcr.model.SingleJcrNodeElem;
import org.argeo.cms.ux.widgets.TreeParent;
import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.jcr.AsyncUiEventListener;
import org.argeo.eclipse.ui.jcr.util.NodeViewerComparer;
import org.argeo.jcr.JcrUtils;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.services.EMenuService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * Basic View to display a sash form to browse a JCR compliant multiple
 * repository environment
 */
public class JcrBrowserView {
	final static String ID = "org.argeo.cms.e4.jcrbrowser";
	final static String NODE_VIEWER_POPUP_MENU_ID = "org.argeo.cms.e4.popupmenu.nodeViewer";

	private boolean sortChildNodes = true;

	/* DEPENDENCY INJECTION */
	@Inject
	@Optional
	private Keyring keyring;
	@Inject
	private RepositoryFactory repositoryFactory;
	@Inject
	private Repository nodeRepository;

	// Current user session on the home repository default workspace
	private Session userSession;

	private OsgiRepositoryRegister repositoryRegister = new OsgiRepositoryRegister();

	// This page widgets
	private TreeViewer nodesViewer;
	private NodeContentProvider nodeContentProvider;
	private TableViewer propertiesViewer;
	private EventListener resultsObserver;

	@PostConstruct
	public void createPartControl(Composite parent, IEclipseContext context, EPartService partService,
			ESelectionService selectionService, EMenuService menuService) {
		repositoryRegister.init();

		parent.setLayout(new FillLayout());
		SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
		// sashForm.setSashWidth(4);
		// sashForm.setLayout(new FillLayout());

		// Create the tree on top of the view
		Composite top = new Composite(sashForm, SWT.NONE);
		// GridLayout gl = new GridLayout(1, false);
		top.setLayout(CmsSwtUtils.noSpaceGridLayout());

		try {
			this.userSession = this.nodeRepository.login(CmsConstants.HOME_WORKSPACE);
		} catch (RepositoryException e) {
			throw new CmsException("Cannot open user session", e);
		}

		nodeContentProvider = new NodeContentProvider(userSession, keyring, repositoryRegister, repositoryFactory,
				sortChildNodes);

		// nodes viewer
		nodesViewer = createNodeViewer(top, nodeContentProvider);

		// context menu : it is completely defined in the plugin.xml file.
		// MenuManager menuManager = new MenuManager();
		// Menu menu = menuManager.createContextMenu(nodesViewer.getTree());

		// nodesViewer.getTree().setMenu(menu);

		nodesViewer.setInput("");

		// Create the property viewer on the bottom
		Composite bottom = new Composite(sashForm, SWT.NONE);
		bottom.setLayout(CmsSwtUtils.noSpaceGridLayout());
		propertiesViewer = createPropertiesViewer(bottom);

		sashForm.setWeights(getWeights());
		nodesViewer.setComparer(new NodeViewerComparer());
		nodesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				selectionService.setSelection(selection.toList());
			}
		});
		nodesViewer.addDoubleClickListener(new JcrE4DClickListener(nodesViewer, partService));
		menuService.registerContextMenu(nodesViewer.getControl(), NODE_VIEWER_POPUP_MENU_ID);
		// getSite().registerContextMenu(menuManager, nodesViewer);
		// getSite().setSelectionProvider(nodesViewer);
	}

	@PreDestroy
	public void dispose() {
		JcrUtils.logoutQuietly(userSession);
		repositoryRegister.destroy();
	}

	public void refresh(Object obj) {
		// Enable full refresh from a command when no element of the tree is
		// selected
		if (obj == null) {
			Object[] elements = nodeContentProvider.getElements(null);
			for (Object el : elements) {
				if (el instanceof TreeParent)
					JcrBrowserUtils.forceRefreshIfNeeded((TreeParent) el);
				getNodeViewer().refresh(el);
			}
		} else
			getNodeViewer().refresh(obj);
	}

	/**
	 * To be overridden to adapt size of form and result frames.
	 */
	protected int[] getWeights() {
		return new int[] { 70, 30 };
	}

	protected TreeViewer createNodeViewer(Composite parent, final ITreeContentProvider nodeContentProvider) {

		final TreeViewer tmpNodeViewer = new TreeViewer(parent, SWT.MULTI);

		tmpNodeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		tmpNodeViewer.setContentProvider(nodeContentProvider);
		tmpNodeViewer.setLabelProvider((IBaseLabelProvider) new NodeLabelProvider());
		tmpNodeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (!event.getSelection().isEmpty()) {
					IStructuredSelection sel = (IStructuredSelection) event.getSelection();
					Object firstItem = sel.getFirstElement();
					if (firstItem instanceof SingleJcrNodeElem)
						propertiesViewer.setInput(((SingleJcrNodeElem) firstItem).getNode());
				} else {
					propertiesViewer.setInput("");
				}
			}
		});

		resultsObserver = new TreeObserver(tmpNodeViewer.getTree().getDisplay());
		if (keyring != null)
			try {
				ObservationManager observationManager = userSession.getWorkspace().getObservationManager();
				observationManager.addEventListener(resultsObserver, Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED, "/",
						true, null, null, false);
			} catch (RepositoryException e) {
				throw new EclipseUiException("Cannot register listeners", e);
			}

		// tmpNodeViewer.addDoubleClickListener(new JcrDClickListener(tmpNodeViewer));
		return tmpNodeViewer;
	}

	protected TableViewer createPropertiesViewer(Composite parent) {
		propertiesViewer = new TableViewer(parent, SWT.NONE);
		propertiesViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		propertiesViewer.getTable().setHeaderVisible(true);
		propertiesViewer.setContentProvider(new PropertiesContentProvider());
		TableViewerColumn col = new TableViewerColumn(propertiesViewer, SWT.NONE);
		col.getColumn().setText("Name");
		col.getColumn().setWidth(200);
		col.setLabelProvider(new ColumnLabelProvider() {
			private static final long serialVersionUID = -6684361063107478595L;

			public String getText(Object element) {
				try {
					return ((Property) element).getName();
				} catch (RepositoryException e) {
					throw new EclipseUiException("Unexpected exception in label provider", e);
				}
			}
		});
		col = new TableViewerColumn(propertiesViewer, SWT.NONE);
		col.getColumn().setText("Value");
		col.getColumn().setWidth(400);
		col.setLabelProvider(new ColumnLabelProvider() {
			private static final long serialVersionUID = -8201994187693336657L;

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
					throw new EclipseUiException("Unexpected exception in label provider", e);
				}
			}
		});
		col = new TableViewerColumn(propertiesViewer, SWT.NONE);
		col.getColumn().setText("Type");
		col.getColumn().setWidth(200);
		col.setLabelProvider(new ColumnLabelProvider() {
			private static final long serialVersionUID = -6009599998150286070L;

			public String getText(Object element) {
				return JcrBrowserUtils.getPropertyTypeAsString((Property) element);
			}
		});
		propertiesViewer.setInput("");
		return propertiesViewer;
	}

	protected TreeViewer getNodeViewer() {
		return nodesViewer;
	}

	/**
	 * Resets the tree content provider
	 * 
	 * @param sortChildNodes if true the content provider will use a comparer to
	 *                       sort nodes that might slow down the display
	 */
	public void setSortChildNodes(boolean sortChildNodes) {
		this.sortChildNodes = sortChildNodes;
		((NodeContentProvider) nodesViewer.getContentProvider()).setSortChildren(sortChildNodes);
		nodesViewer.setInput("");
	}

	/** Notifies the current view that a node has been added */
	public void nodeAdded(TreeParent parentNode) {
		// insure that Ui objects have been correctly created:
		JcrBrowserUtils.forceRefreshIfNeeded(parentNode);
		getNodeViewer().refresh(parentNode);
		getNodeViewer().expandToLevel(parentNode, 1);
	}

	/** Notifies the current view that a node has been removed */
	public void nodeRemoved(TreeParent parentNode) {
		IStructuredSelection newSel = new StructuredSelection(parentNode);
		getNodeViewer().setSelection(newSel, true);
		// Force refresh
		IStructuredSelection tmpSel = (IStructuredSelection) getNodeViewer().getSelection();
		getNodeViewer().refresh(tmpSel.getFirstElement());
	}

	class TreeObserver extends AsyncUiEventListener {

		public TreeObserver(Display display) {
			super(display);
		}

		@Override
		protected Boolean willProcessInUiThread(List<Event> events) throws RepositoryException {
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

		protected void onEventInUiThread(List<Event> events) throws RepositoryException {
			if (getLog().isTraceEnabled())
				getLog().trace("Refresh result list");
			nodesViewer.refresh();
		}

	}

	public boolean getSortChildNodes() {
		return sortChildNodes;
	}

	public void setFocus() {
		getNodeViewer().getTree().setFocus();
	}

	/* DEPENDENCY INJECTION */
	// public void setRepositoryRegister(RepositoryRegister repositoryRegister) {
	// this.repositoryRegister = repositoryRegister;
	// }

	public void setKeyring(CryptoKeyring keyring) {
		this.keyring = keyring;
	}

	public void setRepositoryFactory(RepositoryFactory repositoryFactory) {
		this.repositoryFactory = repositoryFactory;
	}

	public void setNodeRepository(Repository nodeRepository) {
		this.nodeRepository = nodeRepository;
	}
}
