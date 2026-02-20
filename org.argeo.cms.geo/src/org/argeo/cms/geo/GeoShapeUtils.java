package org.argeo.cms.geo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/** Utilities around ODK's GeoShape format */
public class GeoShapeUtils {

	@SuppressWarnings("unchecked")
	public static <T> T geoShapeToGeometry(String geoShape, Class<T> clss) {
		Objects.requireNonNull(geoShape);
		GeometryFactory geometryFactory = JTS.GEOMETRY_FACTORY_WGS84;
		List<Coordinate> coordinates = new ArrayList<>();
		StringTokenizer stSeg = new StringTokenizer(geoShape.trim(), ";");
		while (stSeg.hasMoreTokens()) {
			StringTokenizer stPt = new StringTokenizer(stSeg.nextToken().trim(), " ");
			String lat = stPt.nextToken();
			String lng = stPt.nextToken();
			String alt = stPt.nextToken();
			// String precision = stPt.nextToken();
			Coordinate coord;
			if (!alt.equals("0.0")) {
				coord = new Coordinate(Double.parseDouble(lat), Double.parseDouble(lng), Double.parseDouble(alt));
			} else {
				coord = new Coordinate(Double.parseDouble(lat), Double.parseDouble(lng));
			}
			coordinates.add(coord);
		}
		if (LineString.class.isAssignableFrom(clss)) {
			LineString lineString = geometryFactory
					.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
			return (T) lineString;
		} else if (MultiPoint.class.isAssignableFrom(clss)) {
			MultiPoint multiPoint = geometryFactory
					.createMultiPointFromCoords(coordinates.toArray(new Coordinate[coordinates.size()]));
			return (T) multiPoint;
		} else if (Polygon.class.isAssignableFrom(clss)) {
			Coordinate first = coordinates.get(0);
			Coordinate last = coordinates.get(coordinates.size() - 1);
			if (!(first.getX() == last.getX() && first.getY() == last.getY())) {
				// close the line string
				coordinates.add(first);
			}
			Polygon polygon = geometryFactory.createPolygon(coordinates.toArray(new Coordinate[coordinates.size()]));
			return (T) polygon;
		} else {
			throw new IllegalArgumentException("Unsupported format " + clss);
		}
	}

	/** Converts a {@link Geometry} with WGS84 coordinates to a GeoShape. */
	public static String geometryToGeoShape(Geometry geometry) {
		if (geometry instanceof Point point) {
			Coordinate coordinate = point.getCoordinate();
			StringBuilder sb = new StringBuilder();
			appendToGeoShape(sb, coordinate.getX(), coordinate.getY(), coordinate.getZ());
			return sb.toString();
		} else if (geometry instanceof Polygon || geometry instanceof LineString) {
			StringBuilder sb = new StringBuilder();
			for (Coordinate coordinate : geometry.getCoordinates()) {
				appendToGeoShape(sb, coordinate.getX(), coordinate.getY(), coordinate.getZ());
				sb.append(';');
			}
			return sb.toString();
		} else {
			throw new IllegalArgumentException("Unsupported geometry " + geometry.getClass());
		}
	}

	public static String geoPointToGeoShape(double lon, double lat, double alt) {
		StringBuilder sb = new StringBuilder();
		appendToGeoShape(sb, lon, lat, alt);
		return sb.toString();
	}

	private static void appendToGeoShape(StringBuilder sb, double lon, double lat, double alt) {
		sb.append(lat).append(' ');
		sb.append(lon).append(' ');
		if (alt != Double.NaN)
			sb.append(alt).append(' ');
		else
			sb.append("0 ");
		sb.append('0');
	}

	/** singleton */
	private GeoShapeUtils() {
	}

}
