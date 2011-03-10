package org.argeo.gis.ui;

public interface MapViewerListener {
	public void featureSelected(String layerId, String featureId);

	public void featureUnselected(String layerId, String featureId);
}
