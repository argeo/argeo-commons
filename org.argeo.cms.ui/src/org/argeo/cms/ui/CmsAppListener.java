package org.argeo.cms.ui;

/** Notifies important events in a {@link CmsApp} life cycle. */
public interface CmsAppListener {
	/** Theming has been updated and should be reloaded. */
	void themingUpdated();
}
