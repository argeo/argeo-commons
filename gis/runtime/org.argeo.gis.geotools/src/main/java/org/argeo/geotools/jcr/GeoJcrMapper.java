package org.argeo.geotools.jcr;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.geotools.data.FeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public interface GeoJcrMapper {
	/** Create it if it does not exist */
	public Node getNode(String dataStoreAlias,
			FeatureSource<SimpleFeatureType, SimpleFeature> featureSource,
			SimpleFeature feature);

	public Map<String, List<FeatureSource<SimpleFeatureType, SimpleFeature>>> getPossibleFeatureSources();

	public Node getNode(String dataStoreAlias,
			FeatureSource<SimpleFeatureType, SimpleFeature> featureSource);

	public FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource(
			Node node);

	public SimpleFeature getFeature(Node node);
}
