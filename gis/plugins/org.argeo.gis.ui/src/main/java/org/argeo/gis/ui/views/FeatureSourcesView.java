package org.argeo.gis.ui.views;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.jcr.SimpleNodeContentProvider;
import org.argeo.geotools.jcr.GeoJcrMapper;
import org.argeo.gis.ui.editors.DefaultMapEditor;
import org.argeo.jcr.gis.GisTypes;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.ViewPart;
import org.geotools.data.FeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class FeatureSourcesView extends ViewPart implements
		IDoubleClickListener {
	public final static String ID = "org.argeo.gis.ui.featureSourcesView";

	private String dataStoresBasePath = "/gis/dataStores";

	private Session session;

	private TreeViewer viewer;

	private GeoJcrMapper geoJcrMapper;

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		String[] basePaths = { dataStoresBasePath };
		viewer.setContentProvider(new SimpleNodeContentProvider(session,
				basePaths));
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
					if (!node.getPrimaryNodeType().isNodeType(
							GisTypes.GIS_FEATURE_SOURCE))
						return;
				} catch (RepositoryException e) {
					throw new ArgeoException("Cannot check type of " + node, e);
				}
				FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = geoJcrMapper
						.getFeatureSource(node);
				IEditorPart ed = getSite().getWorkbenchWindow().getActivePage()
						.getActiveEditor();
				if (ed instanceof DefaultMapEditor) {
					((DefaultMapEditor) ed).addLayer(featureSource);
				}
			}

		}

	}

	public void setGeoJcrMapper(GeoJcrMapper geoJcrMapper) {
		this.geoJcrMapper = geoJcrMapper;
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
