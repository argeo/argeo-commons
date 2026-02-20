package org.argeo.cms.geo;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.stream.JsonGenerator;

/**
 * GeoJSon format.
 * 
 * @see https://datatracker.ietf.org/doc/html/rfc7946
 */
public class GeoJson {
	public final static String POINT_TYPE = "Point";
	public final static String MULTI_POINT_TYPE = "MultiPoint";
	public final static String LINE_STRING_TYPE = "LineString";
	public final static String POLYGON_TYPE = "Polygon";
	public final static String GEOMETRY_COLLECTION_TYPE = "GeometryCollection";

	public final static String TYPE = "type";
	public final static String GEOMETRY = "geometry";
	public final static String GEOMETRIES = "geometries";
	public final static String COORDINATES = "coordinates";
	public final static String BBOX = "bbox";
	public final static String PROPERTIES = "properties";

	/*
	 * WRITE
	 */
	/** Writes a {@link Geometry} as GeoJSON. */
	public static void writeGeometry(JsonGenerator g, Geometry geometry) {
		if (geometry instanceof Point point) {
			g.write(TYPE, POINT_TYPE);
			g.writeStartArray(COORDINATES);
			writeCoordinate(g, point.getCoordinate());
			g.writeEnd();// coordinates array
		} else if (geometry instanceof MultiPoint multiPoint) {
			g.write(TYPE, MULTI_POINT_TYPE);
			g.writeStartArray(COORDINATES);
			writeCoordinates(g, multiPoint.getCoordinates());
			g.writeEnd();// coordinates array
		} else if (geometry instanceof LineString lineString) {
			g.write(TYPE, LINE_STRING_TYPE);
			g.writeStartArray(COORDINATES);
			writeCoordinates(g, lineString.getCoordinates());
			g.writeEnd();// coordinates array
		} else if (geometry instanceof Polygon polygon) {
			g.write(TYPE, POLYGON_TYPE);
			g.writeStartArray(COORDINATES);
			LinearRing exteriorRing = polygon.getExteriorRing();
			g.writeStartArray();
			writeCoordinates(g, exteriorRing.getCoordinates());
			g.writeEnd();
			for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
				LinearRing interiorRing = polygon.getInteriorRingN(i);
				// TODO verify that holes are clockwise
				g.writeStartArray();
				writeCoordinates(g, interiorRing.getCoordinates());
				g.writeEnd();
			}
			g.writeEnd();// coordinates array
		} else if (geometry instanceof GeometryCollection geometryCollection) {// must be last
			g.write(TYPE, GEOMETRY_COLLECTION_TYPE);
			g.writeStartArray(GEOMETRIES);
			for (int i = 0; i < geometryCollection.getNumGeometries(); i++) {
				g.writeStartObject();
				writeGeometry(g, geometryCollection.getGeometryN(i));
				g.writeEnd();// geometry object
			}
			g.writeEnd();// geometries array
		} else {
			throw new IllegalArgumentException(geometry.getClass() + " is not supported.");
		}
	}

	/** Writes a sequence of coordinates [[lat,lon],[lat,lon]] */
	public static void writeCoordinates(JsonGenerator g, Coordinate[] coordinates) {
		for (Coordinate coordinate : coordinates) {
			g.writeStartArray();
			writeCoordinate(g, coordinate);
			g.writeEnd();
		}
	}

	/** Writes a pair of coordinates [lat,lon]. */
	public static void writeCoordinate(JsonGenerator g, Coordinate coordinate) {
		// !! longitude is first in GeoJSON
		g.write(coordinate.getY());
		g.write(coordinate.getX());
		double z = coordinate.getZ();
		if (!Double.isNaN(z)) {
			g.write(z);
		}
	}

	/**
	 * Writes the {@link Envelope} of a {@link Geometry} as a bbox GeoJSON object.
	 */
	public static void writeBBox(JsonGenerator g, Geometry geometry) {
		g.writeStartArray(BBOX);
		Envelope envelope = geometry.getEnvelopeInternal();
		g.write(envelope.getMinX());
		g.write(envelope.getMinY());
		g.write(envelope.getMaxX());
		g.write(envelope.getMaxY());
		g.writeEnd();
	}

	/*
	 * READ
	 */
	/** Reads a geometry from the geometry object of a GEoJSON feature. */
	@SuppressWarnings("unchecked")
	public static <T extends Geometry> T readGeometry(JsonObject geom, Class<T> clss) {
		String type = geom.getString(TYPE);
		JsonArray coordinates = geom.getJsonArray(COORDINATES);
		Geometry res = switch (type) {
		case POINT_TYPE: {
			Coordinate coord = readCoordinate(coordinates);
			yield JTS.GEOMETRY_FACTORY_WGS84.createPoint(coord);
		}
		case LINE_STRING_TYPE: {
			Coordinate[] coords = readCoordinates(coordinates);
			yield JTS.GEOMETRY_FACTORY_WGS84.createLineString(coords);
		}
		case POLYGON_TYPE: {
			assert coordinates.size() > 0;
			LinearRing exterior = JTS.GEOMETRY_FACTORY_WGS84
					.createLinearRing(readCoordinates(coordinates.getJsonArray(0)));
			LinearRing[] holes = new LinearRing[coordinates.size() - 1];
			for (int i = 0; i < coordinates.size() - 1; i++) {
				holes[i] = JTS.GEOMETRY_FACTORY_WGS84
						.createLinearRing(readCoordinates(coordinates.getJsonArray(i + 1)));
			}
			yield JTS.GEOMETRY_FACTORY_WGS84.createPolygon(exterior, holes);
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + type);
		};
//		res.normalize();
		return (T) res;
	}

	/** Reads a coordinate pair [lon,lat]. */
	public static Coordinate readCoordinate(JsonArray arr) {
		assert arr.size() >= 2;
		// !! longitude is first in GeoJSon
		return new Coordinate(arr.getJsonNumber(1).doubleValue(), arr.getJsonNumber(0).doubleValue());
	}

	/** Reads a coordinate sequence [[lon,lat],[lon,lat]]. */
	public static Coordinate[] readCoordinates(JsonArray arr) {
		Coordinate[] coords = new Coordinate[arr.size()];
		for (int i = 0; i < arr.size(); i++)
			coords[i] = readCoordinate(arr.getJsonArray(i));
		return coords;
	}

	/** singleton */
	private GeoJson() {
	}

}
