package org.argeo.gis.ui.rap.openlayers.custom;

import org.polymap.openlayers.rap.widget.layers.Layer;

public class BingLayer extends Layer {
	public final static String ROAD = "Road";
	public final static String AERIAL = "Aerial";
	public final static String AERIAL_WITH_LABEL = "AerialWithLabels";

	public BingLayer(String name, String apiKey, String type) {
		super.setName(name);
		super.create("new OpenLayers.Layer.Bing({name:'" + name + "', key:'"
				+ apiKey + "' ,type:'" + type + "'})");

	}
}
