package org.argeo.gis.ui.rap.openlayers;

import org.polymap.openlayers.rap.widget.features.Feature;
import org.polymap.openlayers.rap.widget.geometry.Geometry;

public class IdentifiedVectorFeature extends Feature {

	public IdentifiedVectorFeature(Geometry point, String id) {
		_create(point.getJSObjRef(), id);
	}

	private void _create(String js_name, String attrs) {
		super.create("new OpenLayers.Feature.Vector(" + js_name + "," + attrs
				+ "				);");
	}
}
