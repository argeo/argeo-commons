package org.argeo.cms.geo.ux;

/** An UX part displaying a map. */
public interface MapPart {
	void setCenter(double lng, double lat);

	/** Event when a feature has been single-clicked. */
	record FeatureSingleClickEvent(String path) {
	};

	/** Event when a feature has been selected. */
	record FeatureSelectedEvent(String path) {
	};

	/** Event when a feature popup is requested. */
	record FeaturePopupEvent(String path) {
	};
}
