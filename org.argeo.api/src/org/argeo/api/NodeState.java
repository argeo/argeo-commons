package org.argeo.api;

import java.util.List;
import java.util.Locale;

/** A running node process. */
public interface NodeState {
	Locale getDefaultLocale();

	List<Locale> getLocales();

	String getHostname();

	Long getAvailableSince();

}
