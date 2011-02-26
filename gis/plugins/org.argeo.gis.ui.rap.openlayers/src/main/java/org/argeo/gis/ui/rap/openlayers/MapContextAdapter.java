package org.argeo.gis.ui.rap.openlayers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.map.MapContext;
import org.geotools.map.event.MapLayerListEvent;
import org.geotools.map.event.MapLayerListListener;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.polymap.openlayers.rap.widget.base.OpenLayersEventListener;
import org.polymap.openlayers.rap.widget.base.OpenLayersObject;
import org.polymap.openlayers.rap.widget.base_types.OpenLayersMap;
import org.polymap.openlayers.rap.widget.features.VectorFeature;
import org.polymap.openlayers.rap.widget.geometry.LineStringGeometry;
import org.polymap.openlayers.rap.widget.geometry.PointGeometry;
import org.polymap.openlayers.rap.widget.layers.VectorLayer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

public class MapContextAdapter implements MapLayerListListener,
		OpenLayersEventListener {
	private final static Log log = LogFactory.getLog(MapContextAdapter.class);

	private final MapContext mapContext;
	private final OpenLayersMap openLayersMap;

	public MapContextAdapter(MapContext mapContext, OpenLayersMap openLayersMap) {
		this.mapContext = mapContext;
		this.openLayersMap = openLayersMap;

		mapContext.addMapLayerListListener(this);

		HashMap<String, String> payloadMap = new HashMap<String, String>();
		payloadMap.put("layername", "event.layer.name");
		this.openLayersMap.events.register(this, "changebaselayer", payloadMap);
		payloadMap.put("property", "event.property");
		payloadMap.put("visibility", "event.layer.visibility");
		this.openLayersMap.events.register(this, "changelayer", payloadMap);
	}

	/*
	 * MAP CONTEXT
	 */

	@SuppressWarnings("unchecked")
	public void layerAdded(MapLayerListEvent event) {
		if (log.isDebugEnabled())
			log.debug("Map context layer added " + event);

		try {
			FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = (FeatureSource<SimpleFeatureType, SimpleFeature>) event
					.getLayer().getFeatureSource();

			VectorLayer vectorLayer = new VectorLayer(featureSource.getName()
					.toString());
			FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = featureSource
					.getFeatures();
			FeatureIterator<SimpleFeature> fit = featureCollection.features();
			while (fit.hasNext()) {
				SimpleFeature feature = fit.next();
				Geometry geom = (Geometry) feature.getDefaultGeometry();
				log.debug("Feature " + feature.getID());
//				log.debug("  Geom: " + geom.getClass() + ", centroid="
//						+ geom.getCentroid());
				if (geom instanceof MultiPolygon) {
					MultiPolygon mp = (MultiPolygon) geom;
					List<PointGeometry> points = new ArrayList<PointGeometry>();
					for (Coordinate coo : mp.getCoordinates()) {
						points.add(new PointGeometry(coo.x, coo.y));
					}
					VectorFeature vf = new VectorFeature(
							new LineStringGeometry(
									points.toArray(new PointGeometry[points
											.size()])));
					vectorLayer.addFeatures(vf);
				}
			}
			openLayersMap.addLayer(vectorLayer);
		} catch (IOException e) {
			log.error("Cannot add layer " + event.getLayer(), e);
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
	 * OPENLAYERS MAP
	 */

	public void process_event(OpenLayersObject source, String eventName,
			HashMap<String, String> payload) {
		if (log.isDebugEnabled())
			log.debug("openlayers event from" + source);
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
				VectorLayer edit_layer = new VectorLayer("edit layer");
				if (payload.get("layername").equals(edit_layer.getName())) {
					if (visible) {

					} else {
					}
				}
			}
		} else if (log.isDebugEnabled())
			log.debug("unknown event " + eventName);

	}

}
