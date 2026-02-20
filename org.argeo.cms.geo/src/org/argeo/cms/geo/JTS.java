package org.argeo.cms.geo;

import org.argeo.api.cms.CmsLog;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Factories initialisation and workarounds for the JTS library. The idea is to
 * code defensively around factory initialisation, API changes, and issues
 * related to running in an OSGi environment. Rather see {@link GeoUtils} for
 * functional static utilities.
 */
public class JTS {
	private final static CmsLog log = CmsLog.getLog(JTS.class);

	public final static int WGS84_SRID = 4326;

	/** A geometry factory with no SRID specified */
	public final static GeometryFactory GEOMETRY_FACTORY;
	/** A geometry factory with SRID 4326 (WGS84 in the EPSG database) */
	public final static GeometryFactory GEOMETRY_FACTORY_WGS84;

	static {
		try {
			GEOMETRY_FACTORY = new GeometryFactory();
			GEOMETRY_FACTORY_WGS84 = new GeometryFactory(new PrecisionModel(), WGS84_SRID);
		} catch (RuntimeException e) {
			log.error("Basic JTS initialisation failed, geographical utilities are probably not available", e);
			throw e;
		}
	}

}
