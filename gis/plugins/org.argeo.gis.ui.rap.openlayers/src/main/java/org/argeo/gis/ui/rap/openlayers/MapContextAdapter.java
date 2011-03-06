package org.argeo.gis.ui.rap.openlayers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.geotools.GeoToolsUtils;
import org.argeo.gis.ui.rap.openlayers.custom.JSON;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.map.MapContext;
import org.geotools.map.event.MapLayerListEvent;
import org.geotools.map.event.MapLayerListListener;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.identity.FeatureId;
import org.polymap.openlayers.rap.widget.base.OpenLayersEventListener;
import org.polymap.openlayers.rap.widget.base.OpenLayersObject;
import org.polymap.openlayers.rap.widget.base_types.Bounds;
import org.polymap.openlayers.rap.widget.base_types.OpenLayersMap;
import org.polymap.openlayers.rap.widget.controls.EditingToolbarControl;
import org.polymap.openlayers.rap.widget.controls.SelectFeatureControl;
import org.polymap.openlayers.rap.widget.controls.SnappingControl;
import org.polymap.openlayers.rap.widget.features.VectorFeature;
import org.polymap.openlayers.rap.widget.geometry.LineStringGeometry;
import org.polymap.openlayers.rap.widget.geometry.PointGeometry;
import org.polymap.openlayers.rap.widget.layers.VectorLayer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class MapContextAdapter implements MapLayerListListener,
		OpenLayersEventListener, MouseListener {
	private final static Log log = LogFactory.getLog(MapContextAdapter.class);

	private final MapContext mapContext;
	private final OpenLayersMap map;

	// edit
	private VectorLayer edit_layer;
	private EditingToolbarControl edit_toolbar;
	private VectorLayer selectable_boxes_layer;

	private Map<String, VectorLayer> vectorLayers = Collections
			.synchronizedMap(new HashMap<String, VectorLayer>());
	private Map<String, FeatureSource<SimpleFeatureType, SimpleFeature>> featureSources = Collections
			.synchronizedMap(new HashMap<String, FeatureSource<SimpleFeatureType, SimpleFeature>>());

	public MapContextAdapter(MapContext mapContext, OpenLayersMap openLayersMap) {
		this.mapContext = mapContext;
		this.map = openLayersMap;

		mapContext.addMapLayerListListener(this);

		HashMap<String, String> payloadMap = new HashMap<String, String>();
		payloadMap.put("layername", "event.layer.name");
		this.map.events.register(this, "changebaselayer", payloadMap);
		payloadMap.put("property", "event.property");
		payloadMap.put("visibility", "event.layer.visibility");
		this.map.events.register(this, "changelayer", payloadMap);

		// edit
		HashMap<String, String> editPayload = new HashMap<String, String>();
		editPayload.put("layername", "event.layer.name");
		editPayload.put("x", "event.xy.x");
		editPayload.put("y", "event.xy.y");
		edit_layer = new VectorLayer("edit layer");
		edit_layer.events.register(this, "beforefeatureadded", null);
		edit_layer.events.register(this, "afterfeatureadded", editPayload);
		this.map.addLayer(edit_layer);
		edit_layer.setVisibility(false);

		// add vector layer with some boxes to demonstrate the modify feature
		// feature
		// selectPayload.put("id", "feature.id");
		// selectPayload.put("lon", "feature.lonlat.lon");
		// selectPayload.put("lat", "feature.lonlat.lon");
		selectable_boxes_layer = new VectorLayer("selectable boxes");
		HashMap<String, String> selectPayload = new HashMap<String, String>();
		selectPayload.put("features", selectable_boxes_layer.getJSObjRef()
				+ ".selectedFeatures[0].id");
		selectPayload.put("id", "event.feature.id");
		selectPayload.put("fid", "event.feature.fid");
		selectPayload.put("geometry", "event.feature.geometry");
		selectPayload.put("bounds", "event.feature.bounds");
		selectPayload.put("lonlat", "event.feature.lonlat");
		selectable_boxes_layer.events.register(this, "featureselected",
				selectPayload);
		// selectable_boxes_layer.events.register(this, "featureunselected",
		// selectPayload);
		// selectable_boxes_layer.events.register(this,
		// SelectFeatureControl.EVENT_HIGHLIGHTED, selectPayload);
		// selectable_boxes_layer.events.register(this,
		// SelectFeatureControl.EVENT_SELECTED, null);
		// selectable_boxes_layer.events.register(this, "featuremodified",
		// null);
		map.addLayer(selectable_boxes_layer);
		VectorFeature vector_feature = new VectorFeature(new Bounds(
				-1952081.800054420018569, 1118889.974857959896326,
				7124447.410769510082901, 5465442.183322750031948).toGeometry());
		selectable_boxes_layer.addFeatures(vector_feature);
		selectable_boxes_layer.setVisibility(false);

		SelectFeatureControl mfc = new SelectFeatureControl(
				selectable_boxes_layer, 0);
		map.addControl(mfc);
		// mfc.setHighlightOnly(true);
		mfc.setRenderIntent("temporary");
		mfc.activate();

		HashMap<String, String> mapPayload = new HashMap<String, String>();
		mapPayload.put("bbox", map.getJSObjRef() + ".getExtent().toBBOX()");
		mapPayload.put("lonlat", map.getJSObjRef()
				+ ".getLonLatFromViewPortPx(event.xy)");
		mapPayload.put("x", "event.xy.x");
		mapPayload.put("y", "event.xy.y");
		mapPayload.put("button", "event.button");
		map.events.register(this, "click", mapPayload);
	}

	/*
	 * OPENLAYERS MAP
	 */

	public void process_event(OpenLayersObject source, String eventName,
			HashMap<String, String> payload) {
		if (log.isDebugEnabled())
			log.debug("openlayers event from " + source);
		if (eventName.equals("changebaselayer")) {
			if (log.isDebugEnabled())
				log.debug("client changed baselayer to '"
						+ payload.get("layername") + "' "
						+ payload.get("property"));
		} else if (eventName.equals("changelayer")) {
			if (log.isDebugEnabled())
				log.debug("client changed layer '" + payload.get("layername")
						+ "' " + payload.get("property") + "' "
						+ payload.get("visibility"));
			if (payload.get("property").equals("visibility")) {
				Boolean visible = payload.get("visibility").equals("true");
				if (payload.get("layername").equals(edit_layer.getName())) {
					if (visible) {
						// adding edit control for the vector layer created
						// above
						edit_toolbar = new EditingToolbarControl(edit_layer);
						map.addControl(edit_toolbar);
						VectorLayer[] snapping_layers = { edit_layer,
								selectable_boxes_layer };
						SnappingControl snap_ctrl = new SnappingControl(
								edit_layer, snapping_layers, false);
						snap_ctrl.activate();
						map.addControl(snap_ctrl);

					} else {
						edit_toolbar.deactivate();
						map.removeControl(edit_toolbar);
					}
				}
			}
		} else if (eventName.equals("beforefeatureadded")) {
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
			VectorLayer layer = (VectorLayer) source;
			log.debug(layer.getJSObjRef());

			String layerId = payload.get("layerId");
			String featureId = payload.get("featureId");
			FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = featureSources
					.get(layerId);
			SimpleFeature feature = GeoToolsUtils.querySingleFeature(
					featureSource, featureId);
			log.debug("Geotools Feature id : " + feature.getID());
		} else if (log.isDebugEnabled())
			log.debug("unknown event " + eventName + " : " + payload);

	}

	/*
	 * MAP CONTEXT
	 */

	@SuppressWarnings("unchecked")
	public void layerAdded(MapLayerListEvent event) {
		if (log.isDebugEnabled())
			log.debug("Map context layer added " + event);

		FeatureIterator<SimpleFeature> featureIterator = null;
		try {
			FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = (FeatureSource<SimpleFeatureType, SimpleFeature>) event
					.getLayer().getFeatureSource();

			String layerName = featureSource.getName().toString();
			String layerId = layerName;
			VectorLayer vectorLayer = new VectorLayer(layerName);
			vectorLayer.setObjAttr("id", layerId);
			vectorLayers.put(layerId, vectorLayer);
			featureSources.put(layerId, featureSource);

			// selection
			HashMap<String, String> selectPayload = new HashMap<String, String>();
			selectPayload.put("featureId", "event.feature.id");
			selectPayload.put("geometry", "event.feature.geometry");
			selectPayload.put("layerId", "event.feature.layer.id");
			vectorLayer.events.register(this, "featureselected", selectPayload);
			SelectFeatureControl mfc = new SelectFeatureControl(vectorLayer, 0);
			map.addControl(mfc);
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
				if (log.isDebugEnabled())
					log.debug("Feature " + feature.getID() + ", "
							+ feature.getClass().getName());
				// log.debug("  Geom: " + geom.getClass() + ", centroid="
				// + geom.getCentroid());
				if (geom instanceof Point) {
					Point mp = (Point) geom;
					if (log.isDebugEnabled())
						log.debug(" " + mp.getX() + "," + mp.getY());
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
			log.error("Cannot add layer " + event.getLayer(), e);
		} finally {
			GeoToolsUtils.closeQuietly(featureIterator);
		}
	}

	public void layerRemoved(MapLayerListEvent event) {
		if (log.isDebugEnabled())
			log.debug("Map context layer removed " + event);
	}

	public void layerChanged(MapLayerListEvent event) {
		if (log.isDebugEnabled())
			log.debug("Map context layer changed " + event);
	}

	public void layerMoved(MapLayerListEvent event) {
		if (log.isDebugEnabled())
			log.debug("Map context layer moved " + event);
	}

	/*
	 * MOUSE LISTENER
	 */
	public void mouseDoubleClick(MouseEvent e) {
		if (log.isDebugEnabled())
			log.debug("Mouse double click " + e);
	}

	public void mouseDown(MouseEvent e) {
		if (log.isDebugEnabled())
			log.debug("Mouse down " + e);
	}

	public void mouseUp(MouseEvent e) {
		if (log.isDebugEnabled())
			log.debug("Mouse up " + e);
	}

}
