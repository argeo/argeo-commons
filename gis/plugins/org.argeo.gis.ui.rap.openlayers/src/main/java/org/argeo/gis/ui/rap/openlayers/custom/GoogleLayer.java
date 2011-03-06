package org.argeo.gis.ui.rap.openlayers.custom;

import org.polymap.openlayers.rap.widget.layers.Layer;

public class GoogleLayer extends Layer {
	public GoogleLayer(String name) {
		super.setName(name);
		super.create("new OpenLayers.Layer.Google( '" + name
				+ "',{'sphericalMercator': true, numZoomLevels: 20})");

	}
}
