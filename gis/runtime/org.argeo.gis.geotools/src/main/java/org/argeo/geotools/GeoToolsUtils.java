package org.argeo.geotools;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.FilterFactoryImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;

/** Utilities related to the GeoTools framework */
public class GeoToolsUtils {

	private final static Log log = LogFactory.getLog(GeoToolsUtils.class);

	// TODO: use common factory finder?
	private static FilterFactory2 filterFactory = new FilterFactoryImpl();

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
						e1);
			}
		}
	}

	public static FilterFactory2 ff() {
		return filterFactory;
	}

	public static SimpleFeature querySingleFeature(
			FeatureSource<SimpleFeatureType, SimpleFeature> featureSource,
			String featureId) {
		Set<FeatureId> ids = new HashSet<FeatureId>();
		ids.add(ff().featureId(featureId));
		Filter filter = ff().id(ids);
		FeatureIterator<SimpleFeature> it = null;
		try {
			it = featureSource.getFeatures(filter).features();
			if (!it.hasNext())
				return null;
			else {
				SimpleFeature feature = it.next();
				if (it.hasNext())
					log.warn("More than one feature for feature id "
							+ featureId + " in feature source "
							+ featureSource.getName());
				return feature;
			}
		} catch (Exception e) {
			throw new ArgeoException("Cannot extract single feature "
					+ featureId + " from feature source "
					+ featureSource.getName(), e);
		} finally {
			closeQuietly(it);
		}
	}

	public static void closeQuietly(FeatureIterator<?> featureIterator) {
		if (featureIterator != null)
			try {
				featureIterator.close();
			} catch (Exception e) {
				// silent
			}
	}
}
