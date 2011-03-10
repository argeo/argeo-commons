package org.argeo.gis.ui;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.eclipse.swt.widgets.Composite;

public interface MapViewer {
	public void addLayer(Node layer);

	public NodeIterator getSelectedFeatures();

	public Composite getControl();

	public void addMapViewerListener(MapViewerListener listener);

	public void removeMapViewerListener(MapViewerListener listener);

}
