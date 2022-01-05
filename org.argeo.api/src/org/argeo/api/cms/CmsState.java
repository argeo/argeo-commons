package org.argeo.api.cms;

import java.util.List;
import java.util.Locale;

/** A running node process. */
public interface CmsState {
	Locale getDefaultLocale();

	List<Locale> getLocales();

	String getHostname();

	Long getAvailableSince();

}
