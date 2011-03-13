package org.argeo.geotools.jcr;

import javax.jcr.Node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.jcr.gis.GisNames;
import org.argeo.jcr.gis.GisTypes;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/** Utilities to map JCR from/to JTS and GeoTools */
public class GeoJcrUtils {
	private final static Log log = LogFactory.getLog(GeoJcrUtils.class);

	/** Transforms a geometry node into position within its CRS */
	public static DirectPosition nodeToPosition(Node node) {
		try {
			if (node.isNodeType(GisTypes.GIS_POINT)) {
				CoordinateReferenceSystem crs = getCoordinateReferenceSystem(node);
				Point point = (Point) nodeToGeometry(node);
				return new DirectPosition2D(crs, point.getX(), point.getY());
			} else {
				throw new ArgeoException(node + " is not of a supported type");
			}
		} catch (Exception e) {
			throw new ArgeoException("Cannot extract position from " + node, e);
		}
	}

	/** Transforms a geometry node into a JTS geometry. */
	public static Geometry nodeToGeometry(Node node) {
		try {
			if (node.isNodeType(GisTypes.GIS_POINT)
					|| node.isNodeType(GisTypes.GIS_COORDINATE)) {
				Coordinate coo;
				if (node.hasProperty(GisNames.GIS_Z))
					coo = new Coordinate(node.getProperty(GisNames.GIS_X)
							.getDouble(), node.getProperty(GisNames.GIS_Y)
							.getDouble(), node.getProperty(GisNames.GIS_Z)
							.getDouble());
				else
					coo = new Coordinate(node.getProperty(GisNames.GIS_X)
							.getDouble(), node.getProperty(GisNames.GIS_Y)
							.getDouble());

				// TODO: use factory finder
				// GeometryFactory geometryFactory =
				// JTSFactoryFinder.getGeometryFactory(null);
				GeometryFactory geometryFactory = new GeometryFactory();
				return geometryFactory.createPoint(coo);
			} else {
				throw new ArgeoException(node + " is not of a supported type");
			}
		} catch (Exception e) {
			throw new ArgeoException("Cannot map " + node + " to a geometry", e);
		}
	}

	/** Reads and interpret the coordinate reference system from a node. */
	public static CoordinateReferenceSystem getCoordinateReferenceSystem(
			Node node) {
		try {
			if (!node.isNodeType(GisTypes.GIS_LOCATED))
				throw new ArgeoException(node + " is not of type "
						+ GisTypes.GIS_LOCATED);
			// Coordinate reference system
			String srs = node.getProperty(GisNames.GIS_SRS).getString();
			CoordinateReferenceSystem crs;
			try {
				// first, try to decode an EPSG code
				crs = CRS.decode(srs);
			} catch (Exception e) {
				// if it fails, try a WKT
				try {
					crs = CRS.parseWKT(srs);
				} catch (Exception e1) {
					// if it fails as well, log the error
					log.error("Cannot parse WKT " + srs, e1);
					// and then the previous error (probably more relevant)
					throw e;
				}
			}
			return crs;
		} catch (Exception e) {
			throw new ArgeoException(
					"Cannot get coordinate reference system for " + node, e);
		}
	}

	public static Geometry reproject(CoordinateReferenceSystem crs,
			Geometry geometry, CoordinateReferenceSystem targetCrs) {
		try {
			MathTransform transform = CRS.findMathTransform(crs, targetCrs);
			return JTS.transform(geometry, transform);
		} catch (Exception e) {
			throw new ArgeoException("Cannot reproject " + geometry + " from "
					+ crs + " to " + targetCrs);
		}
	}
}
