package org.argeo.geotools.jdbc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.argeo.ArgeoException;
import org.geotools.data.DataStore;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;

/**
 * Simplified data store to avoid issues with Spring and OSGi when Springs scans
 * for all available factory methods.
 */
public class PostgisDataStoreFactory {
	private PostgisNGDataStoreFactory wrappedFactory = new PostgisNGDataStoreFactory();

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public DataStore createDataStore(DataSource dataSource) {
		try {
			Map params = new HashMap();
			params.put(PostgisNGDataStoreFactory.DATASOURCE.key, dataSource);
			return wrappedFactory.createDataStore(params);
		} catch (IOException e) {
			throw new ArgeoException("Cannot create PostGIS data store", e);
		}
	}
}
