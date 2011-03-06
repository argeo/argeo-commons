package org.argeo.gis.ui.rap.openlayers.custom;

import org.polymap.openlayers.rap.widget.base.OpenLayersObject;

public class JSON extends OpenLayersObject {

	public JSON() {
		super.create("new OpenLayers.Format.JSON();");
		setObjAttr("pretty", true);
	}

}
