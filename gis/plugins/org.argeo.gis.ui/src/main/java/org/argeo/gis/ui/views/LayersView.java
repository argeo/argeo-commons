package org.argeo.gis.ui.views;

import org.argeo.eclipse.ui.TreeParent;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.part.ViewPart;
import org.geotools.map.MapContext;
import org.geotools.map.MapLayer;
import org.geotools.map.event.MapLayerListEvent;
import org.geotools.map.event.MapLayerListListener;

public class LayersView extends ViewPart implements MapLayerListListener {
	private TreeViewer viewer;

	private MapContext mapContext;

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new MapContextContentProvider());
		viewer.setLabelProvider(new MapContextLabelProvider());
		viewer.setInput(getViewSite());
	}

	public void setMapContext(MapContext mapContext) {
		viewer.setInput(mapContext);
		if (this.mapContext != null) {
			this.mapContext.removeMapLayerListListener(this);
		}
		this.mapContext = mapContext;
		this.mapContext.addMapLayerListListener(this);
	}

	/*
	 * MAP LAYER LIST LISTENER
	 */
	public void layerAdded(MapLayerListEvent event) {
		viewer.refresh();
	}

	public void layerRemoved(MapLayerListEvent event) {
		viewer.refresh();
	}

	public void layerChanged(MapLayerListEvent event) {
		viewer.refresh();
	}

	public void layerMoved(MapLayerListEvent event) {
		viewer.refresh();
	}

	/*
	 * VIEW
	 */
	@Override
	public void setFocus() {
		viewer.getTree().setFocus();
	}

	public void refresh() {
		viewer.refresh();
	}

	private class MapContextContentProvider implements ITreeContentProvider {

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof MapContext)
				return new Object[] { new MapContextNode(
						(MapContext) inputElement) };
			else if (inputElement instanceof IViewSite)
				return new Object[] {};
			else
				return getChildren(inputElement);
		}

		public Object[] getChildren(Object element) {
			if (element instanceof MapContextNode) {
				MapContextNode mapContextNode = (MapContextNode) element;
				return mapContextNode.getMapContext().getLayers();
			} else if (element instanceof MapLayer) {
				//MapLayer mapLayer = (MapLayer) element;

			} else if (element instanceof TreeParent) {
				return ((TreeParent) element).getChildren();
			}
			return new Object[0];
		}

		public Object getParent(Object element) {
			if (element instanceof TreeParent) {
				return ((TreeParent) element).getParent();
			}
			return null;
		}

		public boolean hasChildren(Object element) {
			if (element instanceof MapContextNode) {
				return true;
			} else if (element instanceof TreeParent) {
				return ((TreeParent) element).hasChildren();
			} else if (element instanceof MapLayer) {
				return false;
			}
			return false;
		}

	}

	private class MapContextLabelProvider extends LabelProvider {

		@Override
		public String getText(Object element) {
			if (element instanceof MapLayer) {
				MapLayer mapLayer = (MapLayer) element;
				String title = mapLayer.getTitle();
				if (title == null || title.trim().equals(""))
					title = mapLayer.toString();
				return title;
			}
			return super.getText(element);
		}

	}

	private class MapContextNode extends TreeParent {
		private MapContext mapContext;

		public MapContextNode(MapContext mapContext) {
			super("Map Context");
			this.mapContext = mapContext;
		}

		public MapContext getMapContext() {
			return mapContext;
		}

	}
}
