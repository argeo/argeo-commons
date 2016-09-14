package org.argeo.node;

import java.util.List;
import java.util.Locale;

public interface NodeState {
	Locale getDefaultLocale();

	List<Locale> getLocales();

	String getHostname();

	boolean isClean();
	
	Long getAvailableSince();

}
