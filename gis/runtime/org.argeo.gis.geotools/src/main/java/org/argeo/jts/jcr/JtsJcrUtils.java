package org.argeo.jts.jcr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.argeo.ArgeoException;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.gis.GisNames;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.InputStreamInStream;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;

/** Utilities depending only from the JTS library. */
public class JtsJcrUtils {
	private static GeometryFactory geometryFactory = new GeometryFactory();
	private static ThreadLocal<WKBWriter> wkbWriters = new ThreadLocal<WKBWriter>();
	private static ThreadLocal<WKBReader> wkbReaders = new ThreadLocal<WKBReader>() {
		protected WKBReader initialValue() {
			return new WKBReader(getGeometryFactory());
		}
	};

	public static GeometryFactory getGeometryFactory() {
		return geometryFactory;
	}

	public final static Geometry readWkb(Property property) {
		Binary wkbBinary = null;
		InputStream in = null;
		try {
			wkbBinary = property.getBinary();
			in = wkbBinary.getStream();
			WKBReader wkbReader = wkbReaders.get();
			return wkbReader.read(new InputStreamInStream(in));
		} catch (Exception e) {
			throw new ArgeoException("Cannot read WKB from " + property, e);
		} finally {
			IOUtils.closeQuietly(in);
			JcrUtils.closeQuietly(wkbBinary);
		}
	}

	public final static void writeWkb(Property property, Geometry geometry) {
		Binary wkbBinary = null;
		InputStream in = null;
		try {
			WKBWriter wkbWriter = wkbWriters.get();
			byte[] arr = wkbWriter.write(geometry);
			in = new ByteArrayInputStream(arr);
			wkbBinary = property.getSession().getValueFactory()
					.createBinary(in);
			property.setValue(wkbBinary);
		} catch (Exception e) {
			throw new ArgeoException("Cannot write WKB to " + property, e);
		} finally {
			IOUtils.closeQuietly(in);
			JcrUtils.closeQuietly(wkbBinary);
		}
	}
}
