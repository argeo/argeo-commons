package org.argeo.eclipse.ui.jcr.views;

import java.io.BufferedInputStream;
import java.io.File;
import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.dialogs.Error;
import org.argeo.eclipse.ui.jcr.browser.NodeContentProvider;
import org.argeo.eclipse.ui.jcr.browser.NodeLabelProvider;
import org.argeo.eclipse.ui.jcr.browser.PropertiesContentProvider;
import org.argeo.eclipse.ui.jcr.browser.RepositoryNode;
import org.argeo.eclipse.ui.jcr.browser.WorkspaceNode;
import org.argeo.eclipse.ui.specific.FileHandler;
import org.argeo.jcr.RepositoryRegister;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.part.ViewPart;

public class GenericJcrBrowser extends ViewPart {
	private TreeViewer nodesViewer;
	private TableViewer propertiesViewer;

	private RepositoryRegister repositoryRegister;

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());

		SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
		sashForm.setSashWidth(4);
		sashForm.setLayout(new FillLayout());

		Composite top = new Composite(sashForm, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		top.setLayout(gl);

		// nodes viewer
		nodesViewer = new TreeViewer(top, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL);
		nodesViewer.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		nodesViewer.setContentProvider(new NodeContentProvider());
		nodesViewer.setLabelProvider(new NodeLabelProvider());
		nodesViewer
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
		nodesViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				if (event.getSelection() == null
						|| event.getSelection().isEmpty())
					return;
				Object obj = ((IStructuredSelection) event.getSelection())
						.getFirstElement();
				if (obj instanceof RepositoryNode) {
					((RepositoryNode) obj).login();
					nodesViewer.refresh(obj);
				} else if (obj instanceof WorkspaceNode) {
					((WorkspaceNode) obj).login();
					nodesViewer.refresh(obj);
				} // call the openFile commands on node
				else if (obj instanceof Node) {
					Node node = (Node) obj;
					try {
						if (node.isNodeType("nt:file")) {

							Node child = node.getNodes().nextNode();
							if (!child.isNodeType("nt:resource")) {
								Error.show("Cannot open file children Node that are not of 'nt:resource' type.");
								return;
							}
							BufferedInputStream fis = null;

							try {
								fis = (BufferedInputStream) child
										.getProperty("jcr:data").getBinary()
										.getStream();

								String name = node.getName();
								
								// Instantiate the generic object that fits for
								// both
								// RCP & RAP.
								FileHandler fh = new FileHandler();
								fh.openFile(name,
										fis);
								//fh.openFile(file);
							} catch (Exception e) {
								throw new ArgeoException(
										"Stream error while opening file", e);
							} finally {
								IOUtils.closeQuietly(fis);
							}
						}
					} catch (RepositoryException re) {
						re.printStackTrace();

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

		nodesViewer.setInput(repositoryRegister);

		Composite bottom = new Composite(sashForm, SWT.NONE);
		bottom.setLayout(new GridLayout(1, false));

		// properties viewer
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
		nodesViewer.refresh(parentNode);
	}

	public void setRepositoryRegister(RepositoryRegister repositoryRegister) {
		this.repositoryRegister = repositoryRegister;
	}
}
