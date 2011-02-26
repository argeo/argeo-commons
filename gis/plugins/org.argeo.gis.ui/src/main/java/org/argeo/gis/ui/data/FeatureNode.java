package org.argeo.gis.ui.data;

import java.io.IOException;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.TreeParent;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

public class FeatureNode extends TreeParent {
	private final DataStore dataStore;
	private final Name featureName;

	public FeatureNode(DataStore dataStore, Name name) {
		super(name.toString());
		this.dataStore = dataStore;
		this.featureName = name;
	}

	public FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource() {
		try {
			return dataStore.getFeatureSource(featureName);
		} catch (IOException e) {
			throw new ArgeoException("Cannot get feature " + featureName
					+ " of " + dataStore, e);
		}
	}

	public DataStore getDataStore() {
		return dataStore;
	}

	public Name getFeatureName() {
		return featureName;
	}

}