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
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class GeoJcrIndex implements EventListener {
	public final static SimpleFeatureType JCR_POINT;

	final static String JCR_POINT_NAME = "JCR_POINT";
	// PostGIS convention
	final static String DEFAULT_GEOM_NAME = "the_geom";

	private final static Log log = LogFactory.getLog(GeoJcrIndex.class);

	static {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setNamespaceURI(GisNames.GIS_NAMESPACE);
		builder.setName(JCR_POINT_NAME);

		builder.setDefaultGeometry(DEFAULT_GEOM_NAME);
		builder.add(DEFAULT_GEOM_NAME, Point.class);

		builder.add(JcrUtils.normalize(Property.JCR_UUID), String.class);
		builder.add(JcrUtils.normalize(Property.JCR_PATH), String.class);
		builder.add(JcrUtils.normalize(Property.JCR_PRIMARY_TYPE), String.class);
		// mix:created
		// builder.add(JcrUtils.normalize(Property.JCR_CREATED), Date.class);
		// builder.add(JcrUtils.normalize(Property.JCR_CREATED_BY),
		// String.class);
		// mix:lastModified
		builder.add(JcrUtils.normalize(Property.JCR_LAST_MODIFIED), Date.class);
		builder.add(JcrUtils.normalize(Property.JCR_LAST_MODIFIED_BY),
				String.class);

		JCR_POINT = builder.buildFeatureType();
	}

	private DataStore dataStore;
	private FeatureStore<SimpleFeatureType, SimpleFeature> pointsIndex;
	private Session session;
	private SystemExecutionService systemExecutionService;

	// TODO: use common factory finder?
	private FilterFactory2 ff = new FilterFactoryImpl();

	// TODO: use finder?
	private GeometryFactory geometryFactory = new GeometryFactory();

	public void init() {
		GeoToolsUtils.createSchemaIfNeeded(dataStore, JCR_POINT);
		pointsIndex = GeoToolsUtils.getFeatureStore(dataStore,
				JCR_POINT.getName());

		systemExecutionService.executeAsSystem(new Runnable() {
			public void run() {
				try {
					session.getWorkspace()
							.getObservationManager()
							.addEventListener(GeoJcrIndex.this,
									Event.NODE_ADDED | Event.NODE_REMOVED, "/",
									true, null,
									new String[] { GisTypes.GIS_GEOMETRY },
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
		FeatureCollection<SimpleFeatureType, SimpleFeature> pointsToAdd = FeatureCollections
				.newCollection();
		Set<FeatureId> pointsToRemove = new HashSet<FeatureId>();
		while (events.hasNext()) {
			Event event = events.nextEvent();
			try {
				Integer eventType = event.getType();
				if (Event.NODE_ADDED == eventType) {
					Node node = session.getNodeByIdentifier(event
							.getIdentifier());
					if (node.isNodeType(GisTypes.GIS_POINT)) {
						Point point = nodeToPoint(node);
						SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
								JCR_POINT);
						featureBuilder.set(DEFAULT_GEOM_NAME, point);
						mapNodeToFeature(node, featureBuilder);
						pointsToAdd.add(featureBuilder.buildFeature(node
								.getIdentifier()));
					}
				} else if (Event.NODE_REMOVED == eventType) {
					String id = event.getIdentifier();
					pointsToRemove.add(ff.featureId(id));
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
			pointsIndex.setTransaction(transaction);
			try {
				// points
				pointsIndex.addFeatures(pointsToAdd);
				if (pointsToRemove.size() != 0)
					pointsIndex.removeFeatures(ff.id(pointsToRemove));
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

	protected void mapNodeToFeature(Node node,
			SimpleFeatureBuilder featureBuilder) {
		try {
			featureBuilder.set(JcrUtils.normalize(Property.JCR_UUID),
					node.getIdentifier());
			featureBuilder.set(JcrUtils.normalize(Property.JCR_PATH),
					node.getPath());
			featureBuilder.set(JcrUtils.normalize(Property.JCR_PRIMARY_TYPE),
					node.getPrimaryNodeType().getName());
			if (node.hasProperty(Property.JCR_LAST_MODIFIED))
				featureBuilder.set(
						JcrUtils.normalize(Property.JCR_LAST_MODIFIED), node
								.getProperty(Property.JCR_LAST_MODIFIED)
								.getDate().getTime());
			if (node.hasProperty(Property.JCR_LAST_MODIFIED_BY))
				featureBuilder
						.set(JcrUtils.normalize(Property.JCR_LAST_MODIFIED_BY),
								node.getProperty(Property.JCR_LAST_MODIFIED_BY)
										.getString());
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot map " + node + " to "
					+ featureBuilder.getFeatureType(), e);
		}
	}

	/** Return the node as a point in the CRS of the related feature store. */
	protected Point nodeToPoint(Node node) {
		CoordinateReferenceSystem featureStoreCrs = pointsIndex.getSchema()
				.getCoordinateReferenceSystem();
		DirectPosition nodePosition = GeoJcrUtils.nodeToPosition(node);
		CoordinateReferenceSystem nodeCrs = nodePosition
				.getCoordinateReferenceSystem();

		// transform if not same CRS
		DirectPosition targetPosition;
		if (!featureStoreCrs.getIdentifiers().contains(nodeCrs.getName())) {
			MathTransform transform;
			try {
				transform = CRS.findMathTransform(nodeCrs, featureStoreCrs);
				targetPosition = transform.transform(nodePosition, null);
			} catch (Exception e) {
				throw new ArgeoException("Cannot transform from " + nodeCrs
						+ " to " + featureStoreCrs, e);
			}
		} else {
			targetPosition = nodePosition;
		}
		double[] coo = targetPosition.getCoordinate();
		return geometryFactory.createPoint(new Coordinate(coo[0], coo[1]));
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
