package org.argeo.api.cms.ux;

/** The actual implementation of a user interface, using a given technology. */
public interface CmsUi {
	Object getData(String key);

	void setData(String key, Object value);

	CmsView getCmsView();

	void updateLastAccess();

	default boolean isTimedOut() {
		return false;
	};

}
