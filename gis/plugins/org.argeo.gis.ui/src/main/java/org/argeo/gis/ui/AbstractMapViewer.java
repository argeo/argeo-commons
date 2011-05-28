package org.argeo.gis.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.argeo.ArgeoException;
import org.argeo.geotools.jcr.GeoJcrMapper;
import org.argeo.jcr.CollectionNodeIterator;
import org.argeo.jcr.gis.GisTypes;
import org.eclipse.swt.widgets.Composite;
import org.geotools.data.FeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Logic of a map viewer which is independent from a particular map display
 * implementation.
 */
public abstract class AbstractMapViewer implements MapViewer {
	private final Node context;
	private final GeoJcrMapper geoJcrMapper;

	private Composite control;
	private Map<String, Set<String>> selected = new HashMap<String, Set<String>>();

	private Set<MapViewerListener> listeners = Collections
			.synchronizedSet(new HashSet<MapViewerListener>());

	protected abstract void addFeatureSource(String layerId,
			FeatureSource<SimpleFeatureType, SimpleFeature> featureSource,
			Object style);

	public AbstractMapViewer(Node context, GeoJcrMapper geoJcrMapper) {
		super();
		this.context = context;
		this.geoJcrMapper = geoJcrMapper;
	}

	public void addLayer(Node layer, Object style) {
		try {
			if (layer.isNodeType(GisTypes.GIS_FEATURE_SOURCE)) {
				addFeatureSource(layer.getPath(),
						geoJcrMapper.getFeatureSource(layer), style);
			} else {
				throw new ArgeoException("Unsupported layer " + layer);
			}
		} catch (Exception e) {
			throw new ArgeoException("Cannot add layer " + layer, e);
		}

	}

	public NodeIterator getSelectedFeatures() {
		try {
			List<Node> nodes = new ArrayList<Node>();
			for (String layerId : selected.keySet()) {
				Set<String> featureIds = selected.get(layerId);
				Node featureSource = context.getSession().getNode(layerId);
				for (String featureId : featureIds) {
					Node featureNode = geoJcrMapper.getFeatureNode(
							featureSource, featureId);
					nodes.add(featureNode);
				}
			}
			return new CollectionNodeIterator(nodes);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot get selected features from "
					+ context, e);
		}
	}

	public void addMapViewerListener(MapViewerListener listener) {
		listeners.add(listener);
	}

	public void removeMapViewerListener(MapViewerListener listener) {
		listeners.remove(listener);
	}

	protected Node getContext() {
		return context;
	}

	protected Map<String, Set<String>> getSelected() {
		return selected;
	}

	protected Set<MapViewerListener> getListeners() {
		return listeners;
	}

	protected void setControl(Composite control) {
		this.control = control;
	}

	public Composite getControl() {
		return control;
	}

	public GeoJcrMapper getGeoJcrMapper() {
		return geoJcrMapper;
	}

}
