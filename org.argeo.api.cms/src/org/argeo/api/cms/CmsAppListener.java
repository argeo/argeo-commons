package org.argeo.api.cms;

/** Notifies important events in a CMS App life cycle. */
public interface CmsAppListener {
	/** Theming has been updated and should be reloaded. */
	void themingUpdated();
}
