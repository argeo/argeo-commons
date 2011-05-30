package org.argeo.gis.ui.rap.openlayers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.jcr.Node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.geotools.GeoToolsUtils;
import org.argeo.geotools.jcr.GeoJcrMapper;
import org.argeo.gis.ui.AbstractMapViewer;
import org.argeo.gis.ui.MapViewerListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.polymap.openlayers.rap.widget.OpenLayersWidget;
import org.polymap.openlayers.rap.widget.base.OpenLayersEventListener;
import org.polymap.openlayers.rap.widget.base.OpenLayersObject;
import org.polymap.openlayers.rap.widget.base_types.OpenLayersMap;
import org.polymap.openlayers.rap.widget.base_types.Projection;
import org.polymap.openlayers.rap.widget.controls.KeyboardDefaultsControl;
import org.polymap.openlayers.rap.widget.controls.LayerSwitcherControl;
import org.polymap.openlayers.rap.widget.controls.NavigationControl;
import org.polymap.openlayers.rap.widget.controls.OverviewMapControl;
import org.polymap.openlayers.rap.widget.controls.PanZoomBarControl;
import org.polymap.openlayers.rap.widget.controls.ScaleControl;
import org.polymap.openlayers.rap.widget.controls.SelectFeatureControl;
import org.polymap.openlayers.rap.widget.features.VectorFeature;
import org.polymap.openlayers.rap.widget.geometry.LineStringGeometry;
import org.polymap.openlayers.rap.widget.geometry.PointGeometry;
import org.polymap.openlayers.rap.widget.layers.OSMLayer;
import org.polymap.openlayers.rap.widget.layers.VectorLayer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class OpenLayersMapViewer extends AbstractMapViewer implements
		OpenLayersEventListener {
	private final static Log log = LogFactory.getLog(OpenLayersMapViewer.class);

	private final OpenLayersMap map;

	private Map<String, VectorLayer> vectorLayers = Collections
			.synchronizedMap(new HashMap<String, VectorLayer>());
	private Map<String, FeatureSource<SimpleFeatureType, SimpleFeature>> featureSources = Collections
			.synchronizedMap(new HashMap<String, FeatureSource<SimpleFeatureType, SimpleFeature>>());

	public OpenLayersMapViewer(Node context, GeoJcrMapper geoJcrMapper,
			Composite parent) {
		super(context, geoJcrMapper);
		createControl(parent);

		this.map = ((OpenLayersWidget) getControl()).getMap();
		// TODO: make dependent of the base layer
		map.zoomTo(2);

		// mapContextProvider.getMapContext().addMapLayerListListener(this);

		HashMap<String, String> payloadMap = new HashMap<String, String>();
		payloadMap.put("layername", "event.layer.name");
		this.map.events.register(this, "changebaselayer", payloadMap);
		payloadMap.put("property", "event.property");
		payloadMap.put("visibility", "event.layer.visibility");
		this.map.events.register(this, "changelayer", payloadMap);

		// WARNING: registering click events on the map hides other events!!
		// HashMap<String, String> mapPayload = new HashMap<String, String>();
		// mapPayload.put("bbox", map.getJSObjRef() + ".getExtent().toBBOX()");
		// mapPayload.put("lonlat", map.getJSObjRef()
		// + ".getLonLatFromViewPortPx(event.xy)");
		// mapPayload.put("x", "event.xy.x");
		// mapPayload.put("y", "event.xy.y");
		// mapPayload.put("button", "event.button");
		// map.events.register(this, "click", mapPayload);
	}

	protected void createControl(Composite parent) {
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

		setControl(openLayersWidget);
	}

	/*
	 * OPENLAYERS MAP
	 */

	public void process_event(OpenLayersObject source, String eventName,
			HashMap<String, String> payload) {
		if (eventName.equals("beforefeatureadded")) {
			if (log.isDebugEnabled())
				log.debug("before feature added on layer '"
						+ payload.get("layername") + "' x=" + payload.get("x")
						+ "' y=" + payload.get("y"));
		} else if (eventName.equals("afterfeatureadded")) {
			if (log.isDebugEnabled())
				log.debug("after feature added on layer '"
						+ payload.get("layername") + "' x=" + payload.get("x")
						+ "' y=" + payload.get("y"));
		} else if (eventName.equals("featureselected")) {
			if (log.isDebugEnabled())
				log.debug("feature selected " + payload);
			String layerId = payload.get("layerId");
			String featureId = payload.get("featureId");
			if (!getSelected().containsKey(layerId))
				getSelected().put(layerId, new TreeSet<String>());
			getSelected().get(layerId).add(featureId);

			for (MapViewerListener listener : getListeners())
				listener.featureSelected(layerId, featureId);
		} else if (eventName.equals("featureunselected")) {
			if (log.isDebugEnabled())
				log.debug("feature unselected " + payload);
			String layerId = payload.get("layerId");
			String featureId = payload.get("featureId");
			if (getSelected().containsKey(layerId))
				getSelected().get(layerId).remove(featureId);
			for (MapViewerListener listener : getListeners())
				listener.featureUnselected(layerId, featureId);

		} else if (log.isDebugEnabled())
			log.debug("Unknown event '" + eventName + "' from "
					+ source.getClass().getName() + " (" + source.getJSObjRef()
					+ ")" + " : " + payload);

	}

	@Override
	protected void addFeatureSource(String layerId,
			FeatureSource<SimpleFeatureType, SimpleFeature> featureSource, Object style) {
		FeatureIterator<SimpleFeature> featureIterator = null;
		try {
			VectorLayer vectorLayer = new VectorLayer(featureSource.getName()
					.toString());
			vectorLayer.setObjAttr("id", layerId);
			vectorLayers.put(layerId, vectorLayer);
			featureSources.put(layerId, featureSource);

			// selection
			HashMap<String, String> selectPayload = new HashMap<String, String>();
			selectPayload.put("featureId", "event.feature.id");
			selectPayload.put("geometry", "event.feature.geometry");
			selectPayload.put("layerId", "event.feature.layer.id");
			vectorLayer.events.register(this, "featureselected", selectPayload);

			HashMap<String, String> unselectPayload = new HashMap<String, String>();
			unselectPayload.put("featureId", "event.feature.id");
			unselectPayload.put("geometry", "event.feature.geometry");
			unselectPayload.put("layerId", "event.feature.layer.id");
			vectorLayer.events.register(this, "featureunselected",
					unselectPayload);
			SelectFeatureControl mfc = new SelectFeatureControl(vectorLayer, 0);
			// mfc.events.register(this, SelectFeatureControl.EVENT_HIGHLIGHTED,
			// unselectPayload);
			// mfc.events.register(this,
			// SelectFeatureControl.EVENT_UNHIGHLIGHTED,
			// unselectPayload);
			map.addControl(mfc);
			mfc.setMultiple(true);
			mfc.setRenderIntent("temporary");
			mfc.activate();

			FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = featureSource
					.getFeatures();
			featureIterator = featureCollection.features();
			// TODO make this interruptible since it can easily block with huge
			// data
			while (featureIterator.hasNext()) {
				SimpleFeature feature = featureIterator.next();
				Geometry geom = (Geometry) feature.getDefaultGeometry();
				if (log.isTraceEnabled())
					log.trace("Feature " + feature.getID() + ", "
							+ geom.getClass().getName());
				// log.debug("  Geom: " + geom.getClass() + ", centroid="
				// + geom.getCentroid());
				if (geom instanceof Point) {
					Point mp = (Point) geom;
					PointGeometry pg = new PointGeometry(mp.getX(), mp.getY());
					VectorFeature vf = new VectorFeature(pg);
					vf.setObjAttr("id", feature.getID());
					vectorLayer.addFeatures(vf);
				} else if (geom instanceof MultiPolygon) {
					MultiPolygon mp = (MultiPolygon) geom;
					List<PointGeometry> points = new ArrayList<PointGeometry>();
					for (Coordinate coo : mp.getCoordinates()) {
						// if (log.isDebugEnabled())
						// log.debug(" " + coo.x + "," + coo.y);
						points.add(new PointGeometry(coo.x, coo.y));
					}
					VectorFeature vf = new VectorFeature(
							new LineStringGeometry(
									points.toArray(new PointGeometry[points
											.size()])));
					vectorLayer.addFeatures(vf);
				}
			}
			map.addLayer(vectorLayer);
		} catch (IOException e) {
			log.error("Cannot add layer " + featureSource.getName(), e);
		} finally {
			GeoToolsUtils.closeQuietly(featureIterator);
		}

	}

	public void addLayer(String layerId, Collection<?> collection, Object style) {
		// TODO Auto-generated method stub
		
	}

	public void setAreaOfInterest(ReferencedEnvelope areaOfInterest) {
		// TODO Auto-generated method stub
		
	}

	public void setStyle(String layerId, Object style) {
		// TODO Auto-generated method stub
		
	}
	
	
}
