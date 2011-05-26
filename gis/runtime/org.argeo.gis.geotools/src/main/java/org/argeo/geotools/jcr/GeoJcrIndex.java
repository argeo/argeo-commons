package org.argeo.geotools.jcr;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.geotools.GeoToolsUtils;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.gis.GisNames;
import org.argeo.jcr.gis.GisTypes;
import org.argeo.jts.jcr.JtsJcrUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/** Index JCR nodes containing or referencing GIS data. */
public class GeoJcrIndex implements EventListener, GisNames, GisTypes {
	// PostGIS convention
	final static String DEFAULT_GEOM_NAME = "the_geom";

	private final static Log log = LogFactory.getLog(GeoJcrIndex.class);

	private DataStore dataStore;
	private Session session;
	private Executor systemExecutionService;

	private String crs = "EPSG:4326";

	/** The key is the workspace */
	private Map<String, FeatureStore<SimpleFeatureType, SimpleFeature>> geoJcrIndexes = Collections
			.synchronizedMap(new HashMap<String, FeatureStore<SimpleFeatureType, SimpleFeature>>());

	// TODO: use common factory finder?
	private FilterFactory2 ff = new FilterFactoryImpl();

	/** Expects to execute with system authentication */
	public void init() {
		if (systemExecutionService != null)// legacy
			systemExecutionService.execute(new Runnable() {
				public void run() {
					initGeoJcrIndex();
				}
			});
		else
			initGeoJcrIndex();
	}

	protected void initGeoJcrIndex() {
		try {
			// create GIS schema
			SimpleFeatureType indexType = getWorkspaceGeoJcrIndexType(session
					.getWorkspace().getName());
			GeoToolsUtils.createSchemaIfNeeded(dataStore, indexType);

			// register JCR listeners
			ObservationManager om = session.getWorkspace()
					.getObservationManager();
			om.addEventListener(this, Event.NODE_ADDED | Event.NODE_REMOVED,
					"/", true, null, null, false);

			// FIXME: use a different listener for properties since it resets
			// the filter
			// om.addEventListener(this, Event.PROPERTY_CHANGED, "/",
			// true, null, new String[] { GIS_LOCATED }, false);

		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot initialize GeoJcr index", e);
		}
	}

	public void dispose() {
	}

	public void onEvent(final EventIterator events) {
		final FeatureCollection<SimpleFeatureType, SimpleFeature> toAdd = FeatureCollections
				.newCollection();
		final Set<FeatureId> toRemove = new HashSet<FeatureId>();

		// execute with system authentication so that JCR can be read
		systemExecutionService.execute(new Runnable() {
			public void run() {
				while (events.hasNext()) {
					Event event = events.nextEvent();
					try {
						Integer eventType = event.getType();
						if (Event.NODE_ADDED == eventType) {
							Node node = session.getNodeByIdentifier(event
									.getIdentifier());
							if (node.isNodeType(GIS_INDEXED)) {
								SimpleFeature feature = mapNodeToFeature(node,
										getGeoJcrIndex().getSchema());
								toAdd.add(feature);
							}
						} else if (Event.NODE_REMOVED == eventType) {
							// we have no way to check whether the node was
							// actually
							// geoindexed without querying the index, this is
							// therefore
							// more optimal to create a filter with all ideas
							// and apply
							// a remove later on
							String id = event.getIdentifier();
							toRemove.add(ff.featureId(id));
						} else if (Event.PROPERTY_CHANGED == eventType) {
							// TODO: monitor changes to SRS, BBOX AND CENTROID
						}
					} catch (Exception e) {
						log.error("Cannot process event " + event, e);
					}
				}
			}
		});

		// TODO: this may be more optimal to persist in one single
		// transaction,
		// but we will loose modifications on all nodes if a single one
		// fails
		try {
			Transaction transaction = new DefaultTransaction();
			getGeoJcrIndex().setTransaction(transaction);
			try {
				// points
				getGeoJcrIndex().addFeatures(toAdd);
				if (toRemove.size() != 0)
					getGeoJcrIndex().removeFeatures(ff.id(toRemove));
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

	protected FeatureStore<SimpleFeatureType, SimpleFeature> getGeoJcrIndex() {
		String workspaceName = session.getWorkspace().getName();
		if (!geoJcrIndexes.containsKey(workspaceName)) {
			SimpleFeatureType indexType = getWorkspaceGeoJcrIndexType(workspaceName);
			FeatureStore<SimpleFeatureType, SimpleFeature> geoIndex = GeoToolsUtils
					.getFeatureStore(dataStore, indexType.getName());
			geoJcrIndexes.put(workspaceName, geoIndex);
		}
		return geoJcrIndexes.get(workspaceName);
	}

	protected SimpleFeatureType getWorkspaceGeoJcrIndexType(String workspaceName) {

		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setNamespaceURI(GIS_NAMESPACE);
		builder.setName(workspaceName + "_geojcr_index");
		try {
			builder.setCRS(CRS.decode(crs));
		} catch (Exception e) {
			throw new ArgeoException("Cannot set CRS " + crs, e);
		}

		builder.setDefaultGeometry(JcrUtils.normalize(GIS_BBOX));
		builder.add(JcrUtils.normalize(GIS_BBOX), Polygon.class);
		builder.add(JcrUtils.normalize(GIS_CENTROID), Point.class);

		builder.add(JcrUtils.normalize("jcr:uuid"), String.class);
		builder.add(JcrUtils.normalize("jcr:path"), String.class);
		builder.add(JcrUtils.normalize("jcr:primaryType"), String.class);
		// mix:lastModified
		builder.add(JcrUtils.normalize("jcr:lastModified"), Date.class);
		builder.add(JcrUtils.normalize("jcr:lastModifiedBy"), String.class);

		return builder.buildFeatureType();
	}

	protected SimpleFeature mapNodeToFeature(Node node, SimpleFeatureType type) {
		try {
			SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

			Node locatedNode;
			if (node.isNodeType(GIS_LOCATED)) {
				locatedNode = node;
			} else if (node.isNodeType(GIS_INDEXED)) {
				locatedNode = findLocatedParent(node);
			} else {
				throw new ArgeoException("Unsupported node " + node);
			}

			// TODO: reproject to the feature store SRS
			Polygon bbox = (Polygon) JtsJcrUtils.readWkb(locatedNode
					.getProperty(GIS_BBOX));
			builder.set(JcrUtils.normalize(GIS_BBOX), bbox);
			Point centroid = (Point) JtsJcrUtils.readWkb(locatedNode
					.getProperty(GIS_CENTROID));
			builder.set(JcrUtils.normalize(GIS_CENTROID), centroid);

			builder.set(JcrUtils.normalize("jcr:uuid"), node.getIdentifier());
			builder.set(JcrUtils.normalize("jcr:path"), node.getPath());
			builder.set(JcrUtils.normalize("jcr:primaryType"), node
					.getPrimaryNodeType().getName());
			if (node.hasProperty(Property.JCR_LAST_MODIFIED))
				builder.set(JcrUtils.normalize("jcr:lastModified"), node
						.getProperty(Property.JCR_LAST_MODIFIED).getDate()
						.getTime());
			if (node.hasProperty(Property.JCR_LAST_MODIFIED_BY))
				builder.set(JcrUtils.normalize("jcr:lastModifiedBy"), node
						.getProperty(Property.JCR_LAST_MODIFIED_BY).getString());
			return builder.buildFeature(node.getIdentifier());
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot map " + node + " to " + type, e);
		}
	}

	protected Node findLocatedParent(Node child) {
		try {
			if (child.getParent().isNodeType(GIS_LOCATED))
				return child.getParent();
			else
				return findLocatedParent(child.getParent());
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

	public void setSystemExecutionService(Executor systemExecutionService) {
		this.systemExecutionService = systemExecutionService;
	}

}
