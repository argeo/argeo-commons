package org.argeo.geotools.jcr;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.geotools.GeoToolsUtils;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.gis.GisNames;
import org.argeo.jcr.gis.GisTypes;
import org.argeo.jts.jcr.JtsJcrUtils;
import org.argeo.security.SystemExecutionService;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.FilterFactoryImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class GeoJcrIndex implements EventListener {
	final static String GEOJCR_INDEX = "GEOJCR_INDEX";
	// PostGIS convention
	final static String DEFAULT_GEOM_NAME = "the_geom";

	private final static Log log = LogFactory.getLog(GeoJcrIndex.class);

	public static SimpleFeatureType getWorkspaceGeoIndex(String workspaceName) {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setNamespaceURI(GisNames.GIS_NAMESPACE);
		builder.setName(workspaceName.toUpperCase() + "_" + GEOJCR_INDEX);

		builder.setDefaultGeometry(JcrUtils.normalize(GisNames.GIS_BBOX));
		builder.add(JcrUtils.normalize(GisNames.GIS_BBOX), Polygon.class);
		builder.add(JcrUtils.normalize(GisNames.GIS_CENTROID), Point.class);

		builder.add(JcrUtils.normalize(Property.JCR_UUID), String.class);
		builder.add(JcrUtils.normalize(Property.JCR_PATH), String.class);
		builder.add(JcrUtils.normalize(Property.JCR_PRIMARY_TYPE), String.class);
		// mix:lastModified
		builder.add(JcrUtils.normalize(Property.JCR_LAST_MODIFIED), Date.class);
		builder.add(JcrUtils.normalize(Property.JCR_LAST_MODIFIED_BY),
				String.class);

		return builder.buildFeatureType();
	}

	private DataStore dataStore;
	private Session session;
	private SystemExecutionService systemExecutionService;

	// TODO: use common factory finder?
	private FilterFactory2 ff = new FilterFactoryImpl();

	public void init() {

		systemExecutionService.executeAsSystem(new Runnable() {
			public void run() {
				try {
					session.getWorkspace()
							.getObservationManager()
							.addEventListener(GeoJcrIndex.this,
									Event.NODE_ADDED | Event.NODE_REMOVED, "/",
									true, null,
									new String[] { GisTypes.GIS_INDEXED },
									false);
				} catch (RepositoryException e) {
					throw new ArgeoException("Cannot initialize GeoJcr index",
							e);
				}

			}
		});
	}

	public void dispose() {
	}

	public void onEvent(EventIterator events) {
		SimpleFeatureType indexType = getWorkspaceGeoIndex(session
				.getWorkspace().getName());
		GeoToolsUtils.createSchemaIfNeeded(dataStore, indexType);
		FeatureStore<SimpleFeatureType, SimpleFeature> geoJcrIndex = GeoToolsUtils
				.getFeatureStore(dataStore, indexType.getName());

		FeatureCollection<SimpleFeatureType, SimpleFeature> toAdd = FeatureCollections
				.newCollection();
		Set<FeatureId> toRemove = new HashSet<FeatureId>();
		while (events.hasNext()) {
			Event event = events.nextEvent();
			try {
				Integer eventType = event.getType();
				if (Event.NODE_ADDED == eventType) {
					Node node = session.getNodeByIdentifier(event
							.getIdentifier());
					if (node.isNodeType(GisTypes.GIS_LOCATED)) {
						SimpleFeature feature = mapNodeToFeature(node,
								indexType);
						toAdd.add(feature);
					}
				} else if (Event.NODE_REMOVED == eventType) {
					String id = event.getIdentifier();
					toRemove.add(ff.featureId(id));
				}
			} catch (Exception e) {
				log.error("Cannot process event " + event, e);
			}
		}

		// persist
		// TODO: this may be more optimal to persist in one single transaction,
		// but we will loose modifications on all nodes if one fails
		try {
			Transaction transaction = new DefaultTransaction();
			geoJcrIndex.setTransaction(transaction);
			try {
				// points
				geoJcrIndex.addFeatures(toAdd);
				if (toRemove.size() != 0)
					geoJcrIndex.removeFeatures(ff.id(toRemove));
				transaction.commit();
			} catch (Exception e) {
				transaction.rollback();
				throw new ArgeoException("Cannot persist changes", e);
			} finally {
				transaction.close();
			}
		} catch (ArgeoException e) {
			throw e;
		} catch (IOException e) {
			throw new ArgeoException("Unexpected issue with the transaction", e);
		}
	}

	protected SimpleFeature mapNodeToFeature(Node node, SimpleFeatureType type) {
		try {
			SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

			Node locatedNode;
			if (node.isNodeType(GisTypes.GIS_LOCATED)) {
				locatedNode = node;
			} else if (node.isNodeType(GisTypes.GIS_INDEXED)) {
				locatedNode = findLocatedparent(node);
			} else {
				throw new ArgeoException("Unsupported node " + node);
			}

			// TODO: reproject to the feature store SRS
			Polygon bbox = (Polygon) JtsJcrUtils.readWkb(locatedNode
					.getProperty(GisNames.GIS_BBOX));
			builder.set(JcrUtils.normalize(GisNames.GIS_BBOX), bbox);
			Polygon centroid = (Polygon) JtsJcrUtils.readWkb(locatedNode
					.getProperty(GisNames.GIS_CENTROID));
			builder.set(JcrUtils.normalize(GisNames.GIS_CENTROID), centroid);

			builder.set(JcrUtils.normalize(Property.JCR_UUID),
					node.getIdentifier());
			builder.set(JcrUtils.normalize(Property.JCR_PATH), node.getPath());
			builder.set(JcrUtils.normalize(Property.JCR_PRIMARY_TYPE), node
					.getPrimaryNodeType().getName());
			if (node.hasProperty(Property.JCR_LAST_MODIFIED))
				builder.set(JcrUtils.normalize(Property.JCR_LAST_MODIFIED),
						node.getProperty(Property.JCR_LAST_MODIFIED).getDate()
								.getTime());
			if (node.hasProperty(Property.JCR_LAST_MODIFIED_BY))
				builder.set(JcrUtils.normalize(Property.JCR_LAST_MODIFIED_BY),
						node.getProperty(Property.JCR_LAST_MODIFIED_BY)
								.getString());
			return builder.buildFeature(node.getIdentifier());
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot map " + node + " to " + type, e);
		}
	}

	protected Node findLocatedparent(Node child) {
		try {
			if (child.getParent().isNodeType(GisTypes.GIS_LOCATED))
				return child.getParent();
			else
				return findLocatedparent(child.getParent());
		} catch (Exception e) {
			// also if child is root node
			throw new ArgeoException("Cannot find located parent", e);
		}
	}

	/** Returns the node as a point in the CRS of the related feature store. */
	protected Geometry reproject(CoordinateReferenceSystem crs,
			Geometry geometry, CoordinateReferenceSystem targetCrs) {
		// transform if not same CRS
		// FIXME: there is certainly a more standard way to reproject
		if (!targetCrs.getIdentifiers().contains(crs.getName())) {
			throw new ArgeoException("Reprojection not yet supported");
			// MathTransform transform;
			// try {
			// transform = CRS.findMathTransform(nodeCrs, featureStoreCrs);
			// if (geometry instanceof Point) {
			// Point point = (Point) geometry;
			// DirectPosition2D pos = new DirectPosition2D(nodeCrs,
			// point.getX(), point.getY());
			// DirectPosition targetPos = transform.transform(pos, null);
			// return geometryFactory.createPoint(new Coordinate(targetPos
			// .getCoordinate()[0], targetPos.getCoordinate()[1]));
			// } else if (geometry instanceof Polygon) {
			// Polygon polygon = (Polygon) geometry;
			// List<Coordinate> coordinates = new ArrayList<Coordinate>();
			// for (Coordinate coo : polygon.getExteriorRing()) {
			// DirectPosition pos = new DirectPosition2D(nodeCrs,
			// coo.x, coo.y);
			// DirectPosition targetPos = transform.transform(pos,
			// null);
			// // coordinates.add(o)
			// }
			// LinearRing ring = geometryFactory
			// .createLinearRing(coordinates
			// .toArray(new Coordinate[coordinates.size()]));
			// return geometryFactory.createPolygon(ring, null);
			// }
			// } catch (Exception e) {
			// throw new ArgeoException("Cannot transform from " + nodeCrs
			// + " to " + featureStoreCrs, e);
			// }
		} else {
			return geometry;
		}
	}

	public void setDataStore(DataStore dataStore) {
		this.dataStore = dataStore;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public void setSystemExecutionService(
			SystemExecutionService systemExecutionService) {
		this.systemExecutionService = systemExecutionService;
	}

}
