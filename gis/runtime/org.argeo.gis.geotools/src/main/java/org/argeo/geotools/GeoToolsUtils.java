package org.argeo.geotools;

import java.io.IOException;

import org.argeo.ArgeoException;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

/** Utilities related to the GeoTools framework */
public class GeoToolsUtils {

	/** Opens a read/write feature store */
	public static FeatureStore<SimpleFeatureType, SimpleFeature> getFeatureStore(
			DataStore dataStore, Name name) {
		FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
		try {
			featureSource = dataStore.getFeatureSource(name);
		} catch (IOException e) {
			throw new ArgeoException("Cannot open feature source " + name
					+ " in data store " + dataStore, e);
		}
		if (!(featureSource instanceof FeatureStore)) {
			throw new ArgeoException("Feature source " + name
					+ " is not writable.");
		}
		return (FeatureStore<SimpleFeatureType, SimpleFeature>) featureSource;
	}

	/** Creates the provided schema in the data store. */
	public static void createSchemaIfNeeded(DataStore dataStore,
			SimpleFeatureType featureType) {
		try {
			dataStore.getSchema(featureType.getName());
		} catch (IOException e) {
			// assume it does not exist
			try {
				dataStore.createSchema(featureType);
			} catch (IOException e1) {
				throw new ArgeoException("Cannot create schema " + featureType,
						e);
			}
		}
	}
}
