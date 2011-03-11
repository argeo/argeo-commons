package org.argeo.gis.ui.views;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.argeo.eclipse.ui.AbstractTreeContentProvider;
import org.argeo.gis.ui.data.DataStoreNode;
import org.argeo.gis.ui.data.FeatureNode;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.geotools.data.DataStore;

public class DataStoresView extends ViewPart implements IDoubleClickListener {
	private TreeViewer viewer;

	private List<DataStore> dataStores;

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new DataStoreContentProvider());
		viewer.setLabelProvider(new DataStoreLabelProvider());
		viewer.setInput(getViewSite());
		viewer.addDoubleClickListener(this);
	}

	public void doubleClick(DoubleClickEvent event) {
		if (!event.getSelection().isEmpty()) {
			Iterator<?> it = ((IStructuredSelection) event.getSelection())
					.iterator();
			while (it.hasNext()) {
				Object obj = it.next();
				if (obj instanceof FeatureNode) {
//					FeatureNode featureNode = (FeatureNode) obj;
//					FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = featureNode
//							.getFeatureSource();
//					IEditorPart ed = getSite().getWorkbenchWindow().getActivePage().getActiveEditor();
//					if(ed instanceof DefaultMapEditor){
////						((DefaultMapEditor)ed).addLayer(featureSource);
//					}
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

	public void setDataStores(List<DataStore> dataStores) {
		this.dataStores = dataStores;
	}

	private class DataStoreContentProvider extends AbstractTreeContentProvider {

		public Object[] getElements(Object inputElement) {
			List<DataStoreNode> dataStoreNodes = new ArrayList<DataStoreNode>();
			// it is better to deal with OSGi reference using and iterator
			Iterator<DataStore> it = dataStores.iterator();
			while (it.hasNext())
				dataStoreNodes.add(new DataStoreNode(it.next()));
			return dataStoreNodes.toArray();
		}

	}

	private class DataStoreLabelProvider extends LabelProvider {

		@Override
		public String getText(Object element) {
			return super.getText(element);
		}

	}
}
