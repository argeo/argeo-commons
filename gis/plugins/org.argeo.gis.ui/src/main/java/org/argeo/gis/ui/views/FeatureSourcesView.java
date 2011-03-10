package org.argeo.gis.ui.views;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.jcr.SimpleNodeContentProvider;
import org.argeo.gis.ui.editors.DefaultMapEditor;
import org.argeo.gis.ui.editors.MapFormPage;
import org.argeo.jcr.gis.GisTypes;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.part.ViewPart;

public class FeatureSourcesView extends ViewPart implements
		IDoubleClickListener {
	public final static String ID = "org.argeo.gis.ui.featureSourcesView";

	private String dataStoresBasePath = "/gis/dataStores";

	private Session session;

	private TreeViewer viewer;

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		String[] basePaths = { dataStoresBasePath };
		SimpleNodeContentProvider sncp = new SimpleNodeContentProvider(session,
				basePaths);
		sncp.setMkdirs(true);
		viewer.setContentProvider(sncp);
		viewer.setLabelProvider(new MapsLabelProvider());
		viewer.setInput(getViewSite());
		viewer.addDoubleClickListener(this);
	}

	public void doubleClick(DoubleClickEvent event) {
		if (!event.getSelection().isEmpty()) {
			Object obj = ((IStructuredSelection) event.getSelection())
					.getFirstElement();
			if (obj instanceof Node) {
				Node node = (Node) obj;
				try {
					if (!node.isNodeType(GisTypes.GIS_FEATURE_SOURCE))
						return;
				} catch (RepositoryException e) {
					throw new ArgeoException("Cannot check type of " + node, e);
				}
				IEditorPart ed = getSite().getWorkbenchWindow().getActivePage()
						.getActiveEditor();
				if (ed instanceof DefaultMapEditor) {
					((DefaultMapEditor) ed).getMapViewer().addLayer(node);
				} else if (ed instanceof FormEditor) {
					IFormPage activePage = ((FormEditor) ed)
							.getActivePageInstance();
					if (activePage instanceof MapFormPage) {
						((MapFormPage) activePage).getMapViewer()
								.addLayer(node);
					}
				}
			}

		}

	}

	@Override
	public void setFocus() {
		viewer.getTree().setFocus();
	}

	public void refresh() {
		viewer.refresh();
	}

	public void setSession(Session session) {
		this.session = session;
	}

	private class MapsLabelProvider extends LabelProvider {

		@Override
		public String getText(Object element) {
			return super.getText(element);
		}

	}
}
