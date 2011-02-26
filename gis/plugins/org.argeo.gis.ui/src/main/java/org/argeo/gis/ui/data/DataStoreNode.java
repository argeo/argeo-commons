package org.argeo.gis.ui.data;

import java.io.IOException;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.TreeParent;
import org.geotools.data.DataStore;
import org.opengis.feature.type.Name;

public class DataStoreNode extends TreeParent {
	private DataStore dataStore;

	public DataStoreNode(DataStore dataStore) {
		super(dataStore.getInfo().getTitle() != null ? dataStore.getInfo()
				.getTitle() : dataStore.toString());
		this.dataStore = dataStore;
		try {
			for (Name name : dataStore.getNames()) {
				addChild(new FeatureNode(dataStore, name));
			}
		} catch (IOException e) {
			throw new ArgeoException("Cannot scan data store", e);
		}
	}

	public DataStore getDataStore() {
		return dataStore;
	}

}