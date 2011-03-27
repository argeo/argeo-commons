package org.argeo.eclipse.ui.jcr.views;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.jcr.SimpleNodeContentProvider;
import org.argeo.eclipse.ui.jcr.browser.HomeContentProvider;
import org.argeo.eclipse.ui.jcr.browser.NodeLabelProvider;
import org.argeo.eclipse.ui.jcr.browser.RepositoryNode;
import org.argeo.eclipse.ui.jcr.browser.WorkspaceNode;
import org.argeo.eclipse.ui.jcr.utils.JcrFileProvider;
import org.argeo.eclipse.ui.jcr.utils.NodeViewerComparer;
import org.argeo.eclipse.ui.specific.FileHandler;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.part.ViewPart;

/** JCR browser organized around a user home node. */
public class UserCentricJcrBrowser extends ViewPart {
	// private final static Log log = LogFactory.getLog(UserBrowser.class);

	private TreeViewer nodesViewer;

	private Session session;

	@Override
	public void createPartControl(Composite parent) {

		// Instantiate the generic object that fits for
		// both RCP & RAP, must be final to be accessed in the double click
		// listener.
		// Not that in RAP, it registers a service handler that provide the
		// access to the files.

		final JcrFileProvider jfp = new JcrFileProvider();
		final FileHandler fh = new FileHandler(jfp);

		parent.setLayout(new FillLayout());

		Composite top = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		top.setLayout(gl);

		// nodes viewer
		nodesViewer = new TreeViewer(top, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL);
		nodesViewer.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));

		SimpleNodeContentProvider contentProvider = new HomeContentProvider(
				session);
		nodesViewer.setContentProvider(contentProvider);
		nodesViewer.setLabelProvider(new NodeLabelProvider());

		nodesViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				if (event.getSelection() == null
						|| event.getSelection().isEmpty())
					return;
				Object obj = ((IStructuredSelection) event.getSelection())
						.getFirstElement();
				if (obj instanceof RepositoryNode) {
					RepositoryNode rpNode = (RepositoryNode) obj;
					rpNode.login();
					// For the file provider to be able to browse the various
					// repository.
					// TODO : enhanced that.
					jfp.setRepositoryNode(rpNode);
					nodesViewer.refresh(obj);

				} else if (obj instanceof WorkspaceNode) {
					((WorkspaceNode) obj).login();
					nodesViewer.refresh(obj);
				} else if (obj instanceof Node) {
					Node node = (Node) obj;

					// double clic on a file node triggers its opening
					try {
						if (node.isNodeType("nt:file")) {
							String name = node.getName();
							String id = node.getIdentifier();
							fh.openFile(name, id);
						}
					} catch (RepositoryException re) {
						throw new ArgeoException(
								"Repository error while getting Node file info",
								re);
					}
				}
			}
		});

		// context menu
		MenuManager menuManager = new MenuManager();
		Menu menu = menuManager.createContextMenu(nodesViewer.getTree());
		nodesViewer.getTree().setMenu(menu);
		getSite().registerContextMenu(menuManager, nodesViewer);
		getSite().setSelectionProvider(nodesViewer);

		nodesViewer.setInput(session);

		nodesViewer.setComparer(new NodeViewerComparer());

	}

	@Override
	public void setFocus() {
		nodesViewer.getTree().setFocus();
	}

	/**
	 * To be overidden to adapt size of form and result frames.
	 * 
	 * @return
	 */
	protected int[] getWeights() {
		return new int[] { 70, 30 };
	}

	/*
	 * NOTIFICATION
	 */
	public void refresh(Object obj) {
		nodesViewer.refresh(obj);
	}

	public void nodeAdded(Node parentNode, Node newNode) {
		nodesViewer.refresh(parentNode);
		nodesViewer.expandToLevel(newNode, 0);
	}

	public void nodeRemoved(Node parentNode) {

		IStructuredSelection newSel = new StructuredSelection(parentNode);
		nodesViewer.setSelection(newSel, true);
		// Force refresh
		IStructuredSelection tmpSel = (IStructuredSelection) nodesViewer
				.getSelection();
		nodesViewer.refresh(tmpSel.getFirstElement());
	}

	public void setSession(Session session) {
		this.session = session;
	}

}
