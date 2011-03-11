package org.argeo.geotools.jcr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.geotools.GeoToolsConstants;
import org.argeo.geotools.GeoToolsUtils;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.gis.GisNames;
import org.argeo.jcr.gis.GisTypes;
import org.argeo.jts.jcr.JtsJcrUtils;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class SimpleGeoJcrMapper implements GeoJcrMapper {
	private final static Log log = LogFactory.getLog(SimpleGeoJcrMapper.class);

	private String dataStoresBasePath = "/gis/dataStores";

	private Map<String, DataStore> registeredDataStores = Collections
			.synchronizedSortedMap(new TreeMap<String, DataStore>());

	public Map<String, List<FeatureSource<SimpleFeatureType, SimpleFeature>>> getPossibleFeatureSources() {
		Map<String, List<FeatureSource<SimpleFeatureType, SimpleFeature>>> res = new TreeMap<String, List<FeatureSource<SimpleFeatureType, SimpleFeature>>>();
		dataStores: for (String alias : registeredDataStores.keySet()) {
			DataStore dataStore = registeredDataStores.get(alias);
			List<Name> names;
			try {
				names = dataStore.getNames();
			} catch (IOException e) {
				log.warn("Cannot list features sources of data store " + alias,
						e);
				continue dataStores;
			}
			List<FeatureSource<SimpleFeatureType, SimpleFeature>> lst = new ArrayList<FeatureSource<SimpleFeatureType, SimpleFeature>>();
			for (Name name : names) {
				try {
					lst.add(dataStore.getFeatureSource(name));
				} catch (IOException e) {
					if (log.isTraceEnabled())
						log.trace("Skipping " + name + " of data store "
								+ alias + " because it is probably"
								+ " not a feature source", e);
				}
			}
			res.put(alias, lst);
		}
		return res;
	}

	// public Node getNode(String dataStoreAlias,
	// FeatureSource<SimpleFeatureType, SimpleFeature> featureSource,
	// SimpleFeature feature) {
	// StringBuffer pathBuf = new StringBuffer(dataStoresBasePath);
	// pathBuf.append('/').append(dataStoreAlias);
	// pathBuf.append('/').append(featureSource.getName());
	//
	// // TODO: use centroid or bbox to create some depth
	// // Geometry geometry = (Geometry)feature.getDefaultGeometry();
	// // Point centroid = geometry.getCentroid();
	//
	// pathBuf.append('/').append(feature.getID());
	//
	// String path = pathBuf.toString();
	// try {
	// if (session.itemExists(path))
	// return session.getNode(path);
	// else
	// return JcrUtils.mkdirs(session, path);
	// } catch (RepositoryException e) {
	// throw new ArgeoException("Cannot get feature node for " + path, e);
	// }
	// }

	public Node getFeatureNode(Node featureSourceNode, String featureId) {
		Binary bbox = null;
		Binary centroid = null;
		try {
			if (!featureSourceNode.hasNode(featureId)) {
				FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = getFeatureSource(featureSourceNode);
				SimpleFeature feature = GeoToolsUtils.querySingleFeature(
						featureSource, featureId);
				Node featureNode = featureSourceNode.addNode(featureId);
				featureNode.addMixin(GisTypes.GIS_FEATURE);
				Geometry geometry = (Geometry) feature.getDefaultGeometry();

				// SRS
				String srs;
				CoordinateReferenceSystem crs = featureSource.getSchema()
						.getCoordinateReferenceSystem();
				try {
					Integer epsgCode = CRS.lookupEpsgCode(crs, false);
					if (epsgCode != null)
						srs = "EPSG:" + epsgCode;
					else
						srs = crs.toWKT();
				} catch (FactoryException e) {
					log.warn("Cannot lookup EPSG code", e);
					srs = crs.toWKT();
				}
				featureNode.setProperty(GisNames.GIS_SRS, srs);

				Polygon bboxPolygon;
				Geometry envelope = geometry.getEnvelope();
				if (envelope instanceof Point) {
					Point pt = (Point) envelope;
					Coordinate[] coords = new Coordinate[4];
					for (int i = 0; i < coords.length; i++)
						coords[i] = pt.getCoordinate();
					bboxPolygon = JtsJcrUtils.getGeometryFactory()
							.createPolygon(
									JtsJcrUtils.getGeometryFactory()
											.createLinearRing(coords), null);
				} else if (envelope instanceof Polygon) {
					bboxPolygon = (Polygon) envelope;
				} else {
					throw new ArgeoException("Unsupported envelope format "
							+ envelope.getClass());
				}
				bbox = JtsJcrUtils.writeWkb(featureNode.getSession(),
						bboxPolygon);
				featureNode.setProperty(GisNames.GIS_BBOX, bbox);
				centroid = JtsJcrUtils.writeWkb(featureNode.getSession(),
						geometry.getCentroid());
				featureNode.setProperty(GisNames.GIS_CENTROID, centroid);
				featureSourceNode.getSession().save();
				return featureNode;
			} else {
				return featureSourceNode.getNode(featureId);
			}
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot get feature node for feature "
					+ featureId + " from " + featureSourceNode, e);
		} finally {
			JcrUtils.closeQuietly(bbox);
			JcrUtils.closeQuietly(centroid);
		}
	}

	protected Node getNode(Session session, String dataStoreAlias) {
		try {
			Node dataStores;
			if (!session.itemExists(dataStoresBasePath)) {
				dataStores = JcrUtils.mkdirs(session, dataStoresBasePath);
				dataStores.getSession().save();
			} else
				dataStores = session.getNode(dataStoresBasePath);

			Node dataStoreNode;
			if (dataStores.hasNode(dataStoreAlias))
				dataStoreNode = dataStores.getNode(dataStoreAlias);
			else {
				dataStoreNode = dataStores.addNode(dataStoreAlias,
						GisTypes.GIS_DATA_STORE);
				dataStoreNode.getSession().save();
			}
			return dataStoreNode;
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot get node for data store "
					+ dataStoreAlias, e);
		}
	}

	public Node getFeatureSourceNode(Session session, String dataStoreAlias,
			FeatureSource<SimpleFeatureType, SimpleFeature> featureSource) {
		try {
			String name = featureSource.getName().toString();
			Node dataStoreNode = getNode(session, dataStoreAlias);
			if (dataStoreNode.hasNode(name))
				return dataStoreNode.getNode(name);
			else {
				Node featureSourceNode = dataStoreNode.addNode(name);
				featureSourceNode.addMixin(GisTypes.GIS_FEATURE_SOURCE);
				featureSourceNode.getSession().save();
				return featureSourceNode;
			}
		} catch (RepositoryException e) {
			throw new ArgeoException(
					"Cannot get feature source node for data store "
							+ dataStoreAlias + " and feature source "
							+ featureSource.getName(), e);
		}
	}

	public FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource(
			Node node) {
		try {
			Node dataStoreNode = node.getParent();
			// TODO: check a dataStore type
			if (!registeredDataStores.containsKey(dataStoreNode.getName()))
				throw new ArgeoException("No data store registered under "
						+ dataStoreNode);
			DataStore dataStore = registeredDataStores.get(dataStoreNode
					.getName());
			return dataStore.getFeatureSource(node.getName());
		} catch (Exception e) {
			throw new ArgeoException("Cannot find feature source " + node, e);
		}
	}

	public SimpleFeature getFeature(Node node) {
		// TODO Auto-generated method stub
		return null;
	}

	public void register(DataStore dataStore, Map<String, String> properties) {
		if (!properties.containsKey(GeoToolsConstants.ALIAS_KEY)) {
			log.warn("Cannot register data store " + dataStore
					+ " since it has no '" + GeoToolsConstants.ALIAS_KEY
					+ "' property");
			return;
		}
		registeredDataStores.put(properties.get(GeoToolsConstants.ALIAS_KEY),
				dataStore);
	}

	public void unregister(DataStore dataStore, Map<String, String> properties) {
		if (!properties.containsKey(GeoToolsConstants.ALIAS_KEY)) {
			log.warn("Cannot unregister data store " + dataStore
					+ " since it has no '" + GeoToolsConstants.ALIAS_KEY
					+ "' property");
			return;
		}
		registeredDataStores
				.remove(properties.get(GeoToolsConstants.ALIAS_KEY));
	}
}
