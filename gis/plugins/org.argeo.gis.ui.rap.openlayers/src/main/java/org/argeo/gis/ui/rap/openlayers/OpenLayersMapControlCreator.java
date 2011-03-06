package org.argeo.gis.ui.rap.openlayers;

import org.argeo.gis.ui.MapContextProvider;
import org.argeo.gis.ui.MapControlCreator;
import org.argeo.gis.ui.rap.openlayers.custom.BingLayer;
import org.argeo.gis.ui.rap.openlayers.custom.GoogleLayer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.polymap.openlayers.rap.widget.OpenLayersWidget;
import org.polymap.openlayers.rap.widget.base_types.OpenLayersMap;
import org.polymap.openlayers.rap.widget.base_types.Projection;
import org.polymap.openlayers.rap.widget.controls.KeyboardDefaultsControl;
import org.polymap.openlayers.rap.widget.controls.LayerSwitcherControl;
import org.polymap.openlayers.rap.widget.controls.MouseDefaultsControl;
import org.polymap.openlayers.rap.widget.controls.NavigationControl;
import org.polymap.openlayers.rap.widget.controls.OverviewMapControl;
import org.polymap.openlayers.rap.widget.controls.PanZoomBarControl;
import org.polymap.openlayers.rap.widget.controls.ScaleControl;
import org.polymap.openlayers.rap.widget.layers.OSMLayer;
import org.polymap.openlayers.rap.widget.layers.WMSLayer;

public class OpenLayersMapControlCreator implements MapControlCreator {
	public Composite createMapControl(Composite parent,
			MapContextProvider mapContextProvider) {

		// OpenLayersWidget openLayersWidget = new OpenLayersWidget(parent,
		// SWT.MULTI | SWT.WRAP, "/js_lib/OpenLayers/OpenLayers.js");
		OpenLayersWidget openLayersWidget = new OpenLayersWidget(parent,
				SWT.MULTI | SWT.WRAP);
		openLayersWidget.setLayoutData(new GridData(GridData.FILL_BOTH));

		OpenLayersMap map = openLayersWidget.getMap();
		map.setProjection(new Projection("EPSG:900913"));
		map.setDisplayProjection(new Projection("EPSG:4326"));
		map.setUnits("m");

		map.addControl(new LayerSwitcherControl());
		NavigationControl navigationControl = new NavigationControl();
		navigationControl.setObjAttr("handleRightClicks", true);
		navigationControl.setObjAttr("zoomBoxEnabled", true);
		map.addControl(navigationControl);
		map.addControl(new KeyboardDefaultsControl());
		map.addControl(new PanZoomBarControl());
		map.addControl(new ScaleControl());

		// WMSLayer baseLayer = new WMSLayer("argeo_dev",
		// "https://dev.argeo.org/geoserver/wms?",
		// "naturalearth:10m_admin_0_countries");

		OSMLayer osmLayer = new OSMLayer("OSM",
				"http://tile.openstreetmap.org/${z}/${x}/${y}.png", 19);
		map.addLayer(osmLayer);

		map.addControl(new OverviewMapControl());

		// map.addLayer(new BingLayer("Bing Aerial", BingLayer.AERIAL));

		MapContextAdapter mapContextAdapter = new MapContextAdapter(
				mapContextProvider.getMapContext(), map);
		// FIXME: find a better way to register it
		openLayersWidget.setData(mapContextAdapter);
		// openLayersWidget.addMouseListener(mapContextAdapter);
		return openLayersWidget;
	}

}
