package org.argeo.eclipse.ui.jcr.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.argeo.ArgeoException;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class GenericJcrBrowser extends ViewPart {
	// private final static Log log =
	// LogFactory.getLog(GenericJcrBrowser.class);

	private TreeViewer nodesViewer;
	private TableViewer propertiesViewer;

	private Session jcrSession;

	private Comparator<Item> itemComparator = new Comparator<Item>() {
		public int compare(Item o1, Item o2) {
			try {
				return o1.getName().compareTo(o2.getName());
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot compare " + o1 + " and " + o2,
						e);
			}
		}
	};

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());

		SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
		sashForm.setSashWidth(4);
		sashForm.setLayout(new FillLayout());

		Composite top = new Composite(sashForm, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		top.setLayout(gl);

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
		try {
			nodesViewer.setInput(jcrSession.getRootNode());
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot initialize view", e);
		}

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

	public void setJcrSession(Session jcrSession) {
		this.jcrSession = jcrSession;
	}

	/*
	 * NODES
	 */
	private class NodeContentProvider implements ITreeContentProvider {

		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		public Object[] getChildren(Object parentElement) {
			try {
				if (parentElement instanceof Node) {
					List<Node> children = new ArrayList<Node>();
					NodeIterator nit = ((Node) parentElement).getNodes();
					while (nit.hasNext()) {
						Node node = nit.nextNode();
						children.add(node);
					}
					Node[] arr = children.toArray(new Node[children.size()]);
					Arrays.sort(arr, itemComparator);
					return arr;
				} else {
					return null;
				}
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot retrieve children for "
						+ parentElement, e);
			}
		}

		public Object getParent(Object element) {
			try {
				if (element instanceof Node) {
					return ((Node) element).getParent();
				}
				return null;
			} catch (RepositoryException e) {
				throw new ArgeoException(
						"Cannot retrieve parent of " + element, e);
			}
		}

		public boolean hasChildren(Object element) {
			try {
				if (element instanceof Node) {
					return ((Node) element).hasNodes();
				}
				return false;
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot check children of " + element,
						e);
			}
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

	class NodeLabelProvider extends LabelProvider {

		public String getText(Object element) {
			try {
				if (element instanceof Node) {
					Node node = (Node) element;
					String label = node.getName();
					// try {
					// Item primaryItem = node.getPrimaryItem();
					// label = primaryItem instanceof Property ? ((Property)
					// primaryItem)
					// .getValue().getString()
					// + " ("
					// + node.getName()
					// + ")" : node.getName();
					// } catch (RepositoryException e) {
					// label = node.getName();
					// }
					StringBuffer mixins = new StringBuffer("");
					for (NodeType type : node.getMixinNodeTypes())
						mixins.append(' ').append(type.getName());

					return label + " [" + node.getPrimaryNodeType().getName()
							+ mixins + "]";
				}
				return element.toString();
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot get text for of " + element, e);
			}
		}

	}

	/*
	 * PROPERTIES
	 */
	private class PropertiesContentProvider implements
			IStructuredContentProvider {

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public Object[] getElements(Object inputElement) {
			try {
				if (inputElement instanceof Node) {
					Set<Property> props = new TreeSet<Property>(itemComparator);
					PropertyIterator pit = ((Node) inputElement)
							.getProperties();
					while (pit.hasNext())
						props.add(pit.nextProperty());
					return props.toArray();
				}
				return new Object[] {};
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot get element for "
						+ inputElement, e);
			}
		}

	}
}
